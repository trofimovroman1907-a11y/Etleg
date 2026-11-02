package app.nexora.messenger

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.CreateRoomParameters
import org.matrix.rustcomponents.sdk.EncryptionState
import org.matrix.rustcomponents.sdk.Membership
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.RoomPreset
import org.matrix.rustcomponents.sdk.RoomVisibility
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import org.matrix.rustcomponents.sdk.TaskHandle
import org.matrix.rustcomponents.sdk.Timeline
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.TimelineListener
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import org.matrix.rustcomponents.sdk.use

/**
 * The only message/session gateway used by the Android UI.
 * Plaintext Matrix REST message endpoints are deliberately not exposed here.
 */
class MatrixE2eeClient private constructor(
    private val context: Context,
    private val client: Client
) : AutoCloseable {

    data class RoomSummary(val id: String, val name: String)
    data class ChatMessage(val sender: String, val body: String, val isOwn: Boolean)

    val userId: String get() = client.userId()
    val deviceId: String get() = client.deviceId()
    val accessToken: String get() = client.session().accessToken

    private val syncService = client.syncService()
        .withSharePos(true)
        .withOfflineMode()
        .finish()

    @Volatile private var activeTimeline: Timeline? = null
    @Volatile private var activeTimelineTask: TaskHandle? = null

    fun startSync() = runBlocking {
        syncService.start()
    }

    fun joinedRooms(): List<RoomSummary> = runBlocking {
        client.rooms()
            .filter { it.membership() == Membership.JOINED }
            .map { room ->
                val id = room.id()
                RoomSummary(id, room.displayName() ?: id)
            }
            .sortedBy { it.name.lowercase() }
    }

    fun createEncryptedPrivateRoom(name: String, invitees: List<String> = emptyList()): String =
        runBlocking {
            require(name.isNotBlank()) { "Укажи название комнаты" }
            client.createRoom(
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
        }

    /**
     * Observe the SDK timeline. The SDK decrypts Megolm events before they reach this callback.
     * Incoming plaintext is never fetched from /rooms/{id}/messages.
     */
    fun observeTimeline(roomId: String, onUpdate: (List<ChatMessage>) -> Unit) = runBlocking {
        closeTimelineInternal()
        val room = client.getRoom(roomId)
            ?: throw IllegalStateException("Комната не найдена в Matrix SDK")

        if (room.latestEncryptionState() != EncryptionState.ENCRYPTED) {
            throw IllegalStateException("Комната не зашифрована. Отправка заблокирована.")
        }

        val timeline = room.timeline()
        activeTimeline = timeline
        val items = mutableListOf<ChatMessage?>()

        val listener = object : TimelineListener {
            override fun onUpdate(diff: List<TimelineDiff>) {
                val snapshot = synchronized(items) {
                    applyDiff(items, diff)
                    items.filterNotNull().toList()
                }
                onUpdate(snapshot)
            }
        }
        activeTimelineTask = timeline.addListener(listener)
        timeline.paginateBackwards(50u.toUShort())
    }

    fun sendEncryptedText(roomId: String, text: String) = runBlocking {
        require(text.isNotBlank()) { "Пустое сообщение" }
        val room = client.getRoom(roomId)
            ?: throw IllegalStateException("Комната не найдена")
        if (room.latestEncryptionState() != EncryptionState.ENCRYPTED) {
            throw IllegalStateException("Отправка запрещена: E2EE не включено в этой комнате.")
        }
        val timeline = room.timeline()
        timeline.send(messageEventContentFromMarkdown(text))
    }

    fun closeTimeline() = runBlocking {
        closeTimelineInternal()
    }

    private fun closeTimelineInternal() {
        activeTimelineTask?.let { task ->
            task.cancel()
            task.destroy()
        }
        activeTimelineTask = null
        activeTimeline = null
    }

    private fun applyDiff(items: MutableList<ChatMessage?>, diffs: List<TimelineDiff>) {
        for (diff in diffs) {
            when (diff) {
                is TimelineDiff.Append -> diff.values.forEach { items.add(mapItem(it)) }
                is TimelineDiff.PushBack -> items.add(mapItem(diff.value))
                is TimelineDiff.PushFront -> items.add(0, mapItem(diff.value))
                is TimelineDiff.Set -> items[diff.index.toInt()] = mapItem(diff.value)
                is TimelineDiff.Insert -> items.add(diff.index.toInt(), mapItem(diff.value))
                is TimelineDiff.Remove -> if (diff.index.toInt() in items.indices) items.removeAt(diff.index.toInt())
                is TimelineDiff.Reset -> {
                    items.clear()
                    diff.values.forEach { items.add(mapItem(it)) }
                }
                is TimelineDiff.Truncate -> {
                    val length = diff.length.toInt().coerceIn(0, items.size)
                    while (items.size > length) items.removeAt(items.lastIndex)
                }
                TimelineDiff.PopFront -> if (items.isNotEmpty()) items.removeAt(0)
                TimelineDiff.PopBack -> if (items.isNotEmpty()) items.removeAt(items.lastIndex)
                TimelineDiff.Clear -> items.clear()
            }
        }
    }

    private fun mapItem(item: TimelineItem): ChatMessage? = item.use { timelineItem ->
        val event = timelineItem.asEvent() ?: return@use null
        val body = event.content.use { content ->
            when (content) {
                is TimelineItemContent.MsgLike -> when (val kind = content.content.kind) {
                    is MsgLikeKind.Message -> kind.content.body
                    is MsgLikeKind.UnableToDecrypt -> "Не удалось расшифровать сообщение"
                    is MsgLikeKind.Sticker -> kind.body
                    else -> null
                }
                else -> null
            }
        }
        body?.takeIf { it.isNotBlank() }?.let {
            ChatMessage(event.sender, it, event.isOwn)
        }
    }

    fun logout() = runBlocking {
        closeTimelineInternal()
        try {
            syncService.stop()
            syncService.destroy()
        } finally {
            client.logout()
            SecureSessionStore.clear(context, SESSION_KEY)
            client.close()
        }
    }

    override fun close() {
        closeTimelineInternal()
        try {
            runBlocking {
                syncService.stop()
                syncService.destroy()
            }
        } catch (_: Throwable) {
        }
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
                    val wrapper = MatrixE2eeClient(app, client)
                    wrapper.startSync()
                    client.encryption().waitForE2eeInitializationTasks()
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
                val wrapper = MatrixE2eeClient(app, client)
                wrapper.startSync()
                client.encryption().waitForE2eeInitializationTasks()
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
