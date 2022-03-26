package markussp.onion.util;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class Crypto {

    public static KeyPair generateKeyPair(){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(Standards.KEYSIZE);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static KeyPair generateKeyPair(byte[] otherEncodedKey) throws InvalidKeySpecException{
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec otherEncodedKeySpec = new X509EncodedKeySpec(otherEncodedKey);
            PublicKey otherPublicKey = keyFactory.generatePublic(otherEncodedKeySpec);
            DHParameterSpec parameterSpec = ((DHPublicKey) otherPublicKey).getParams();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(parameterSpec);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SecretKeySpec generateSecretKeySpec(KeyPair keyPair, byte[] otherEncodedKey) throws InvalidKeySpecException, InvalidKeyException{
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec otherEncodedKeySpec = new X509EncodedKeySpec(otherEncodedKey);
            PublicKey otherPublicKey = keyFactory.generatePublic(otherEncodedKeySpec);
            keyAgreement.doPhase(otherPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();

            return new SecretKeySpec(sharedSecret, 0, 16, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Cipher generateCipher(SecretKeySpec secretKeySpec) throws InvalidKeyException{
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Cipher generateDecipher(byte[] encodedParams, SecretKeySpec secretKeySpec) throws IOException, InvalidKeyException, InvalidAlgorithmParameterException{
        try {
            AlgorithmParameters algParams = AlgorithmParameters.getInstance("AES");
            algParams.init(encodedParams);
            Cipher decipher = Cipher.getInstance("AES/CBC/NoPadding");
            decipher.init(Cipher.DECRYPT_MODE, secretKeySpec, algParams);

            return decipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
