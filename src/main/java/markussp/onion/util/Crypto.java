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

/**
 * The Crypto class is a static class providing methods used in a Diffie-Hellman Key Exchange.
 */
public final class Crypto {

    /**
     * Generate a key pair, the public key needs to be encoded and sent to the other client
     * @return a new KeyPair
     */
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

    /**
     * Generate a KeyPair with the same specifiations as another clients public key
     * @param otherEncodedKey the other clients encoded public key
     * @return a new KeyPair
     * @throws InvalidKeySpecException
     */
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

    /**
     * After generating a KeyPair and getting the other clients PublicKey, the shared secret can be
     * generated.
     * @param keyPair this clients key pair
     * @param otherEncodedKey the other clients encoded public key
     * @return a SecretKeySpec, will be equal to the other clients one
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
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

    /**
     * After generating a SecretKeySpec, a Cipher object can be generated to encrypt data
     * @param secretKeySpec the shared secret
     * @return a Cipher object for encryption
     * @throws InvalidKeyException
     */
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

    /**
     * After generating a SecretKeySpec, the other client will generate a Cipher and send this client
     * the specifications used in the creation. This method will make a Cipher object that can decipher
     * the data from the other client.
     * @param encodedParams the encoded cipher parameters from the other client
     * @param secretKeySpec the shared secret
     * @return a Cipher object for decryption
     * @throws IOException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
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
