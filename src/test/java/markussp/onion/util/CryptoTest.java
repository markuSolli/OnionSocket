package markussp.onion.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoTest {

    @Nested
    public class generateKeyPair{

        @Test
        public void returns_keypair() throws InvalidKeySpecException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            byte[] encodedPublicKey = keyPairA.getPublic().getEncoded();
            KeyPair keyPairB = Crypto.generateKeyPair(encodedPublicKey);

            assertNotNull(keyPairB);
        }

        @Test
        public void handles_wrong_encoded_key() {
            assertThrows(InvalidKeySpecException.class, () -> Crypto.generateKeyPair(new byte[]{1,2,3,4}));
        }
    }

    @Nested
    public class generateSecretKeySpec{

        @Test
        public void returns_keyspec() throws InvalidKeySpecException, InvalidKeyException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpecA = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            SecretKeySpec secretKeySpecB = Crypto.generateSecretKeySpec(keyPairB, keyPairA.getPublic().getEncoded());

            assertNotNull(secretKeySpecA);
            assertNotNull(secretKeySpecB);
        }

        @Test
        public void produces_equal_keyspecs() throws InvalidKeySpecException, InvalidKeyException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpecA = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            SecretKeySpec secretKeySpecB = Crypto.generateSecretKeySpec(keyPairB, keyPairA.getPublic().getEncoded());

            assertEquals(secretKeySpecA, secretKeySpecB);
        }

        @Test
        public void handles_wrong_key_pair() throws InvalidKeySpecException, NoSuchAlgorithmException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(Standards.KEYSIZE >> 1);
            KeyPair wrongKeyPair = keyPairGenerator.generateKeyPair();

            assertThrows(InvalidKeyException.class, () -> Crypto.generateSecretKeySpec(wrongKeyPair, keyPairB.getPublic().getEncoded()));
        }

        @Test
        public void handles_wrong_encoded_key(){
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            byte[] wrongEncodedKey = {1,2,3,4};

            assertThrows(InvalidKeySpecException.class, () -> Crypto.generateSecretKeySpec(keyPairA, wrongEncodedKey));
        }
    }

    @Nested
    public class generateCipher{

        @Test
        public void returns_cipher() throws InvalidKeySpecException, InvalidKeyException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            Cipher cipher = Crypto.generateCipher(secretKeySpec);

            assertNotNull(cipher);
        }

        @Test
        public void cipher_encrypts_with_equal_size() throws InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            Cipher cipher = Crypto.generateCipher(secretKeySpec);
            assertNotNull(cipher);

            byte[] string = ("Hello World!").getBytes(StandardCharsets.UTF_8);
            byte[] cleartext = Arrays.copyOf(string, 16);
            byte[] ciphertext = cipher.doFinal(cleartext);

            assertNotEquals(cleartext, ciphertext);
            assertEquals(cleartext.length, ciphertext.length);
        }
    }

    @Nested
    public class generateDecipher{

        @Test
        public void returns_decipher() throws InvalidKeySpecException, InvalidKeyException, IOException, InvalidAlgorithmParameterException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            Cipher cipher = Crypto.generateCipher(secretKeySpec);
            assertNotNull(cipher);

            Cipher decipher = Crypto.generateDecipher(cipher.getParameters().getEncoded(), secretKeySpec);

            assertNotNull(decipher);
        }

        @Test
        public void decipher_decrypts_correctly() throws InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            Cipher cipher = Crypto.generateCipher(secretKeySpec);
            assertNotNull(cipher);
            Cipher decipher = Crypto.generateDecipher(cipher.getParameters().getEncoded(), secretKeySpec);
            assertNotNull(decipher);

            String strA = "Hello world!";
            byte[] strArray = strA.getBytes(StandardCharsets.UTF_8);
            byte[] cleartext = Arrays.copyOf(strArray, 16);
            byte[] ciphertext = cipher.doFinal(cleartext);
            String strB = new String(decipher.doFinal(ciphertext), StandardCharsets.UTF_8).trim();

            assertEquals(strA, strB);
        }

        @Test
        public void handles_wrong_params() throws InvalidKeySpecException, InvalidKeyException {
            KeyPair keyPairA = Crypto.generateKeyPair();
            assertNotNull(keyPairA);
            KeyPair keyPairB = Crypto.generateKeyPair(keyPairA.getPublic().getEncoded());
            assertNotNull(keyPairB);
            SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPairA, keyPairB.getPublic().getEncoded());
            Cipher cipher = Crypto.generateCipher(secretKeySpec);
            assertNotNull(cipher);

            byte[] wrongParams = {1,2,3,4};
            assertThrows(IOException.class, () -> Crypto.generateDecipher(wrongParams, secretKeySpec));
        }
    }
}
