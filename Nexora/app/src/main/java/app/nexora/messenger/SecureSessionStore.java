package app.nexora.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts the Matrix access token with a non-exportable Android Keystore key. */
public final class SecureSessionStore {
    private static final String PREFS = "nexora_secure_session";
    private static final String KEY_ALIAS = "nexora_matrix_session_v1";
    private static final String PREFIX = "v1:";
    private SecureSessionStore() {}

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        java.security.Key existing = ks.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public static void write(Context context, String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        ByteBuffer data = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        data.put((byte) iv.length).put(iv).put(encrypted);
        String encoded = PREFIX + Base64.encodeToString(data.array(), Base64.NO_WRAP);
        if (!prefs(context).edit().putString(name, encoded).commit()) {
            throw new IllegalStateException("Could not save encrypted session");
        }
    }

    public static String read(Context context, String name) throws Exception {
        String encoded = prefs(context).getString(name, null);
        if (encoded == null) return null;
        if (!encoded.startsWith(PREFIX)) throw new IllegalStateException("Unknown session format");
        ByteBuffer data = ByteBuffer.wrap(Base64.decode(encoded.substring(PREFIX.length()), Base64.NO_WRAP));
        int ivLength = data.get() & 0xff;
        if (ivLength < 12 || ivLength > 16 || data.remaining() <= ivLength) {
            throw new IllegalStateException("Invalid encrypted session");
        }
        byte[] iv = new byte[ivLength];
        data.get(iv);
        byte[] ciphertext = new byte[data.remaining()];
        data.get(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    public static void clear(Context context, String name) {
        prefs(context).edit().remove(name).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
