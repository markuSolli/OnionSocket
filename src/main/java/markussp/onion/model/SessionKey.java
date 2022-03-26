package markussp.onion.model;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * The SessionKey class is for holding both {@link Cipher} objects needed for encryped
 * communication. The ciphers themselves are private but encryption and decryption
 * is available through methods.
 */
public class SessionKey {
    public Cipher cipher;
    public Cipher decipher;

    /**
     * Store a single object for both encryption and decryption.
     * @param cipher the cipher object in encryption mode.
     * @param decipher the cipher object in decryption mode.
     */
    public SessionKey(Cipher cipher, Cipher decipher){
        this.cipher = cipher;
        this.decipher = decipher;
    }

    /**
     * Encrypt a byte array with this objects cipher.
     * @param cleartext The byte array to encrypt.
     * @return an encrypted byte array.
     * @throws IllegalBlockSizeException if the inputs block size does not correspond with
     * the provided encryption algorithm upon cipher creation.
     * @throws BadPaddingException if this cipher (wrongly) is in decryption mode, and
     * the given data is not padded correctly.
     */
    public byte[] encrypt(byte[] cleartext) throws IllegalBlockSizeException, BadPaddingException {
        return cipher.doFinal(cleartext);
    }

    /**
     * Decrypt a byte array with this objects cipher.
     * @param ciphertext The encrypted byte array.
     * @return a decrypted byte array.
     * @throws IllegalBlockSizeException if the inputs block size does not correspond with
     * the provided encryption algorithm upon cipher creation.
     * @throws BadPaddingException if unpadding has been requested in cypher creation, and
     * the given data is not padded correctly.
     */
    public byte[] decrypt(byte[] ciphertext) throws IllegalBlockSizeException, BadPaddingException {
        return decipher.doFinal(ciphertext);
    }
}
