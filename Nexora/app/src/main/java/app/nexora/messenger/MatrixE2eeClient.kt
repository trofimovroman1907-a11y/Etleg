package app.nexora.messenger

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.CreateRoomParameters
import org.matrix.rustcomponents.sdk.RoomPreset
import org.matrix.rustcomponents.sdk.RoomVisibility
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import org.matrix.rustcomponents.sdk.SlidingSyncVersion

/**
 * Matrix Rust SDK owner for Nexora's E2EE state.
 *
 * The SDK's SQLite stores contain Olm/Megolm keys and room state. The serialized
 * Matrix session is encrypted separately with Android Keystore before storage.
 * Never use the REST message endpoint to send into encrypted rooms.
 */
class MatrixE2eeClient private constructor(
    private val context: Context,
    private val client: Client
) : AutoCloseable {

    val userId: String get() = client.userId()
    val deviceId: String get() = client.deviceId()
    val accessToken: String get() = client.session().accessToken

    fun createEncryptedPrivateRoom(name: String, invitees: List<String> = emptyList()): String =
        runBlocking {
            val roomId = client.createRoom(
                CreateRoomParameters(
                    name = name,
                    topic = null,
                    isEncrypted = true,
                    isDirect = invitees.size == 1,
                    visibility = RoomVisibility.Private,
                    preset = RoomPreset.PRIVATE_CHAT,
                    invite = invitees
                )
            )
            roomId
        }

    fun enableEncryption(roomId: String) = runBlocking {
        val room = client.getRoom(roomId)
            ?: throw IllegalStateException("Комната не найдена в локальном состоянии Matrix")
        room.enableEncryption()
        client.encryption().waitForE2eeInitializationTasks()
    }

    fun sendEncryptedText(roomId: String, text: String) = runBlocking {
        require(text.isNotBlank()) { "Пустое сообщение" }
        val room = client.getRoom(roomId)
            ?: throw IllegalStateException("Комната не найдена")
        // Timeline.send() routes the event through the SDK send queue.
        // The Rust SDK transparently encrypts it when the room is encrypted.
        val timeline = room.timeline()
        timeline.send(messageEventContentFromMarkdown(text))
    }

    fun logout() = runBlocking {
        client.logout()
        SecureSessionStore.clear(context, SESSION_KEY)
        client.close()
    }

    override fun close() {
        client.close()
    }

    companion object {
        private const val SESSION_KEY = "matrix_sdk_session_v1"
        private const val STORE_DIR = "matrix-sdk"

        @Volatile private var active: MatrixE2eeClient? = null

        @JvmStatic
        @Synchronized
        fun login(context: Context, homeserver: String, username: String, password: String): MatrixE2eeClient =
            runBlocking {
                val app = context.applicationContext
                val client = buildClient(app, homeserver)
                try {
                    client.login(username, password, "Nexora Android", null)
                    client.encryption().waitForE2eeInitializationTasks()
                    val wrapper = MatrixE2eeClient(app, client)
                    wrapper.persistSession()
                    active?.close()
                    active = wrapper
                    wrapper
                } catch (error: Throwable) {
                    client.close()
                    throw error
                }
            }

        @JvmStatic
        @Synchronized
        fun restore(context: Context): MatrixE2eeClient? = runBlocking {
            val app = context.applicationContext
            val raw = SecureSessionStore.read(app, SESSION_KEY) ?: return@runBlocking null
            val json = JSONObject(raw)
            val homeserver = json.getString("homeserver")
            val session = Session(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").takeIf { it.isNotEmpty() },
                userId = json.getString("user_id"),
                deviceId = json.getString("device_id"),
                homeserverUrl = homeserver,
                oauthData = json.optString("oauth_data").takeIf { it.isNotEmpty() },
                slidingSyncVersion = SlidingSyncVersion.valueOf(json.optString("sliding_sync_version", "NONE"))
            )
            val client = buildClient(app, homeserver)
            try {
                client.restoreSession(session)
                client.encryption().waitForE2eeInitializationTasks()
                val wrapper = MatrixE2eeClient(app, client)
                active?.close()
                active = wrapper
                wrapper
            } catch (error: Throwable) {
                client.close()
                throw error
            }
        }

        @JvmStatic
        fun current(): MatrixE2eeClient? = active

        private fun buildClient(context: Context, homeserver: String): Client =
            runBlocking {
                val root = java.io.File(context.noBackupFilesDir, STORE_DIR).apply { mkdirs() }
                ClientBuilder()
                    .homeserverUrl(homeserver)
                    .sessionPaths(
                        java.io.File(root, "state").absolutePath,
                        java.io.File(root, "cache").absolutePath
                    )
                    .autoEnableCrossSigning(true)
                    .autoEnableBackups(true)
                    .slidingSyncVersionBuilder(org.matrix.rustcomponents.sdk.SlidingSyncVersionBuilder.NONE)
                    .build()
            }

        private fun MatrixE2eeClient.persistSession() {
            val s = client.session()
            val json = JSONObject()
                .put("access_token", s.accessToken)
                .put("refresh_token", s.refreshToken)
                .put("user_id", s.userId)
                .put("device_id", s.deviceId)
                .put("homeserver", s.homeserverUrl)
                .put("oauth_data", s.oauthData)
                .put("sliding_sync_version", s.slidingSyncVersion.name)
            SecureSessionStore.write(context, SESSION_KEY, json.toString())
        }
    }
}
