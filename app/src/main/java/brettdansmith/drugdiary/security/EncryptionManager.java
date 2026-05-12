package brettdansmith.drugdiary.security;

import android.util.Base64;
import android.util.Base64OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Handles encrypted JSON vault payloads.
 *
 * Optimized for a fresh-install environment. Uses AES/CBC with a random IV 
 * prepended to the ciphertext. No legacy support for older formats.
 */
public final class EncryptionManager {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private static final byte[] SALT = "DrugDiarySecureSalt_v3".getBytes();
    private static final int IV_LENGTH = 16;

    private EncryptionManager() {}

    public static String encrypt(String jsonData, SecretKeySpec key) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(jsonData.getBytes(StandardCharsets.UTF_8));
        byte[] envelope = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();
        return Base64.encodeToString(envelope, Base64.NO_WRAP);
    }

    public static void encryptToFile(String jsonData, SecretKeySpec key, File file) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        try (FileOutputStream fos = new FileOutputStream(file);
             Base64OutputStream b64os = new Base64OutputStream(fos, Base64.NO_WRAP)) {
            b64os.write(iv);
            try (CipherOutputStream cos = new CipherOutputStream(b64os, cipher);
                 OutputStreamWriter writer = new OutputStreamWriter(cos, StandardCharsets.UTF_8)) {
                writer.write(jsonData);
            }
        }
    }

    public static String decrypt(String encryptedData, SecretKeySpec key) throws Exception {
        byte[] decoded = Base64.decode(encryptedData, Base64.DEFAULT);
        if (decoded.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted payload");
        }
        
        byte[] iv = new byte[IV_LENGTH];
        byte[] payload = new byte[decoded.length - IV_LENGTH];
        System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
        System.arraycopy(decoded, IV_LENGTH, payload, 0, payload.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
    }

    public static SecretKeySpec deriveKey(String password) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
