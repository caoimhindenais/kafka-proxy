package utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    //You could imagine this been injected by encryption as a service
    public final static String TOP_SECRET_KEY = "ThisIsASecretKey";
    public static final String CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";

    private static SecretKeySpec secretKey;
    private static byte[] key;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void setKey(String myKey)
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "utils.AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static byte[] encrypt(String secretKey, byte[] plainText) {

        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        if (raw.length != 16) {
            throw new IllegalArgumentException("Invalid secretKey size.");
        }

        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(CIPHER_INSTANCE);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec,
                    new IvParameterSpec(new byte[16]));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        byte[] bytes = new byte[0];
        try {
            bytes = cipher.doFinal(plainText);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static byte[] decrypt(String key, byte[] encrypted)
             {

        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length != 16) {
            throw new IllegalArgumentException("Invalid key size.");
        }
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

                 Cipher cipher = null;
                 try {
                     cipher = Cipher.getInstance(CIPHER_INSTANCE);
                 } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                     e.printStackTrace();
                 }
                 try {
                     cipher.init(Cipher.DECRYPT_MODE, skeySpec,
                    new IvParameterSpec(new byte[16]));
                 } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                     e.printStackTrace();
                 }
                 byte[] original = new byte[0];
                 try {
                     original = cipher.doFinal(encrypted);
                 } catch (IllegalBlockSizeException | BadPaddingException e) {
                     e.printStackTrace();
                     return original;
                 }

                 return  original;
    }
}