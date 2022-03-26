package markussp.onion.util;

import markussp.onion.model.SessionKey;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * The Standards class is a static class for package-wide constants and methods.
 */
public final class Standards {
    public static final int PORTNR = 3000;                  //port number to run servers on
    public static final String DIST = "localhost";          //address for the Distributor
    public static final int DISTPORT = 3040;                //port for the Distributor
    public static final int PACKETSIZE = 512;               //packet size
    public static final int NODES = 3;                      //number of nodes in a chain
    public static final int KEYSIZE = 1024;                 //Public key size

    /**
     * This methods handles a Diffie-Hellman key exchange as the recieving end.
     * @param input the InputStream to read from
     * @param output the OutputStream to write to
     * @return a {@link SessionKey} object holding both the cipher and decipher, this
     * will be the same object as the client who initiated the handshake.
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public static SessionKey handleKeyExchange(InputStream input, OutputStream output) throws IOException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        //Read other clients public key
        byte[] message = input.readNBytes(PACKETSIZE);
        byte[] lengthArray = Arrays.copyOf(message, 4);
        int length = ByteBuffer.wrap(lengthArray).getInt();
        byte[] otherEndodedKey = Arrays.copyOfRange(message, 4, 4 + length);

        //Generate cipher
        KeyPair keyPair = Crypto.generateKeyPair(otherEndodedKey);
        SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPair, otherEndodedKey);
        Cipher cipher = Crypto.generateCipher(secretKeySpec);

        //Send public key and cipher parameters
        message = new byte[PACKETSIZE];
        byte[] encodedKey = keyPair.getPublic().getEncoded();
        lengthArray = ByteBuffer.allocate(4).putInt(encodedKey.length).array();
        byte[] encodedParams = cipher.getParameters().getEncoded();

        System.arraycopy(lengthArray, 0, message, 0, 4);
        System.arraycopy(encodedKey, 0, message, 4, encodedKey.length);
        System.arraycopy(encodedParams, 0, message, 4 + encodedKey.length, 18);

        output.write(message);

        //Read decipher parameters and generate decipher
        message = input.readNBytes(PACKETSIZE);
        byte[] otherEncodedParams = Arrays.copyOf(message, 18);
        Cipher decipher = Crypto.generateDecipher(otherEncodedParams, secretKeySpec);

        return new SessionKey(cipher, decipher);
    }
}
