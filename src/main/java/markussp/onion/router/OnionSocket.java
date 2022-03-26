package markussp.onion.router;

import markussp.onion.model.Address;
import markussp.onion.model.SessionKey;
import markussp.onion.util.Crypto;
import markussp.onion.util.Standards;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * The OnionSocket class provides secure communication by using a onion network. If a network is
 * established, using this class is as easy as constructing a new object of this class with
 * the networks Distributor and the address to connect to as arguments.
 *
 * If a network is not established, a local network is needed for this class to work:
 * <ul>
 * <li> Run an instance of {@link Distributor}</li>
 * <li> Run atleast three instances of {@link OnionNode}</li>
 * </ul>
 * Read the documentation and make sure to specify the correct addresses in each class.
 */
public class OnionSocket {
    private SessionKey[] keys = new SessionKey[0];
    private InputStream input;
    private OutputStream output;

    /**
     * Get an instance of OnionSocket using the standard NodeDistribution address.
     * @param address the IP-address to connect to
     * @param port the portnumber to connect to
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeySpecException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    public OnionSocket(InetAddress address, int port) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        this(InetAddress.getByName(Standards.DIST), Standards.DISTPORT, address, port);
    }

    /**
     * Get an instance of OnionSocket with a specified NodeDistribution address.
     * @param distAddress the IP-addres of the NodeDistribution server
     * @param distPort the portnumber of the NodeDistribution server
     * @param address the IP-address to connect to
     * @param port the portnumber to connect to
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public OnionSocket(InetAddress distAddress, int distPort, InetAddress address, int port) throws IOException, InvalidAlgorithmParameterException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //Request nodes from Distributor
        Socket socket = new Socket(distAddress, distPort);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        output.write(2);

        //Read answer
        SessionKey sessionKey = initKeyExchange();
        byte[] ciphertext = read();
        byte[] message = sessionKey.decrypt(ciphertext);
        socket.close();

        //Read first node
        InetAddress a = InetAddress.getByAddress(Arrays.copyOf(message, 4));
        int p = ByteBuffer.wrap(Arrays.copyOfRange(message, 4, 8)).getInt();

        //Connect to first node
        socket = new Socket(a, p);
        input = socket.getInputStream();
        output = socket.getOutputStream();

        //Link the next two nodes
        for(int i=1; i<3; i++){
            a = InetAddress.getByAddress(Arrays.copyOfRange(message, 8*i, 8*i + 4));
            p = ByteBuffer.wrap(Arrays.copyOfRange(message, 8*i + 4, 8*(i+1))).getInt();

            nodeHandshake(new Address(a, p));
        }

        //Connect to destination address
        nodeHandshake(new Address(address, port));
    }

    /**
     * Send a message through the connected onion routers.
     * Will pad the array with trailing zeroes to ensure the standard packet size
     * of 512. Will encrypt with {@link SessionKey}s if this socket has some.
     * @param bytes the message to send, with a maximum size of 512
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public void send(byte[] bytes) throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] message = new byte[Standards.PACKETSIZE];
        System.arraycopy(bytes, 0, message, 0, bytes.length);

        //Encrypt with available session keys
        if(keys.length > 0){
            for(int i=keys.length-1; i>=0; i--){
                message = keys[i].encrypt(message);
            }
        }

        output.write(message);
    }

    /**
     * Read a message through the connected onion routers.
     * Will block until a message is recieved, so call when you expect a message.
     * Will be padded with trailing zeroes to ensure the standard packet size
     * of 512. Will decrypt with {@link SessionKey}s if this socket has some.
     * @return a byte array of size 512
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public byte[] read() throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] message = input.readNBytes(Standards.PACKETSIZE);

        //Decrypt with available session keys
        if(keys.length > 0){
            for(int i=0; i<keys.length; i++){
                message = keys[i].decrypt(message);
            }
        }

        return message;
    }

    /**
     * Method for initiating a Diffie-Hellman key exchange.
     * @return a {@link SessionKey} object holding both the cipher and decipher, this
     * will be the same object as the other node.
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private SessionKey initKeyExchange() throws InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IOException, IllegalBlockSizeException, BadPaddingException {
        //Generate and send public key
        KeyPair keyPair = Crypto.generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] message = new byte[Standards.PACKETSIZE];
        byte[] lengthArray = ByteBuffer.allocate(4).putInt(publicKey.length).array();
        System.arraycopy(lengthArray, 0, message, 0, 4);
        System.arraycopy(publicKey, 0, message, 4, publicKey.length);

        send(message);

        //Read other public key and cipher parameters
        message = read();
        lengthArray = Arrays.copyOf(message, 4);
        int keyLength = ByteBuffer.wrap(lengthArray).getInt();
        byte[] otherEncodedKey = Arrays.copyOfRange(message, 4, 4 + keyLength);
        byte[] encodedParams = Arrays.copyOfRange(message, 4 + keyLength, 22 + keyLength);

        //Generate shared secret and ciphers
        SecretKeySpec secretKeySpec = Crypto.generateSecretKeySpec(keyPair, otherEncodedKey);
        Cipher decipher = Crypto.generateDecipher(encodedParams, secretKeySpec);
        Cipher cipher = Crypto.generateCipher(secretKeySpec);

        //Send cipher parameters
        send(cipher.getParameters().getEncoded());

        return new SessionKey(cipher, decipher);
    }

    /**
     * Method for correctly connecting to a {@link OnionNode}, making it switch over to relay mode.
     * When called the first time, most of the traffic will be in cleartext. But as more
     * nodes are connected with this OnionSocket, the traffic will be encrypted until
     * it reaches the intended recipient using the {@link SessionKey}s made during the
     * previous handshakes.
     * @param nextNode after the key exchange the node this socket is talking with needs to
     * know who to relay to. This is the address of the next node.
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeySpecException
     * @throws IOException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    private void nodeHandshake(Address nextNode) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, InvalidKeySpecException, IOException, BadPaddingException, InvalidKeyException {
        SessionKey sessionKey = initKeyExchange();
        keys = Arrays.copyOf(keys, keys.length + 1);
        keys[keys.length-1] = sessionKey;

        //Send next node address
        InetAddress address = nextNode.address;
        int port = nextNode.port;
        byte[] addressArray = address.getAddress();
        byte[] portArray = ByteBuffer.allocate(4).putInt(port).array();
        byte[] nextAddress = new byte[8];
        System.arraycopy(addressArray, 0, nextAddress, 0, 4);
        System.arraycopy(portArray, 0, nextAddress, 4, 4);

        send(nextAddress);
    }
}
