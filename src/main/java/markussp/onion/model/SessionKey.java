package markussp.onion.model;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class SessionKey {
    public Cipher cipher;
    public Cipher decipher;

    public SessionKey(Cipher cipher, Cipher decipher){
        this.cipher = cipher;
        this.decipher = decipher;
    }

    public byte[] encrypt(byte[] cleartext) throws IllegalBlockSizeException, BadPaddingException {
        return cipher.doFinal(cleartext);
    }

    public byte[] decrypt(byte[] ciphertext) throws IllegalBlockSizeException, BadPaddingException {
        return decipher.doFinal(ciphertext);
    }
}
