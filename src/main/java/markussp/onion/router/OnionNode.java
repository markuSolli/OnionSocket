package markussp.onion.router;

import markussp.onion.model.SessionKey;
import markussp.onion.util.Standards;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The OnionNode class is used to relay messages through an onion network.
 * By calling {@link #launch() launch()} the server will start.
 * By default the server will run on port 3000.
 */
public class OnionNode {
    private final int portnr;
    private final String distAddress;
    private final int distPort;
    private boolean running = true;

    /**
     * Make a new OnionNode with a custom portnumber
     * @param portnr an available port
     * @throws IOException
     */
    public OnionNode(int portnr) throws IOException {
        this(portnr, Standards.DIST, Standards.DISTPORT);
    }

    /**
     * Make a new OnionNode with a specified address
     * for the {@link Distributor} running in the network.
     * By default the OnionNode will try to connect to localhost:3040.
     * @param distAddress the IP-address of the NodeDistributor
     * @param distPort the portnumber of the NodeDistributor
     * @throws IOException
     */
    public OnionNode(String distAddress, int distPort) throws IOException {
        this(Standards.PORTNR, distAddress, distPort);
    }

    /**
     * Make a new OnionNode with a custom portnumber, and a specified address
     * for the {@link Distributor} running in the network.
     * By default the OnionNode will try to connect to localhost:2999.
     * @param portnr an available port to host on
     * @param distAddress the IP-address of the NodeDistributor
     * @param distPort the portnumber of the NodeDistributor
     * @throws IOException
     */
    public OnionNode(int portnr, String distAddress, int distPort) throws IOException {
        this.portnr = portnr;
        this.distAddress = distAddress;
        this.distPort = distPort;
    }

    /**
     * Launch the server. Will check in with the Distributor, afterwards
     * it listens on the port handling connections in seperate threads.
     * Will block until {@link #close()} is called.
     * @throws IOException if an error occurs when:
     * <ul>
     * <li> creating the {@link Socket}, {@link OutputStream} or {@link ServerSocket} </li>
     * <li> writing to OutputStream </li>
     * <li> closing the Socket or OutputStream </li>
     * <li> waiting for a connection </li>
     * </ul>
     * @throws IOException
     * @throws InterruptedException
     */
    public void launch() throws IOException, InterruptedException {
        //Check in at Distributor
        sendToDistributor(0);

        //Listen on port and send new connections to their own threads
        ServerSocket server = new ServerSocket(portnr);
        ArrayList<NodeThread> nodes = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        server.setSoTimeout(500);
        while(running){
            try {
                Socket socket = server.accept();
                NodeThread node = new NodeThread(socket);
                Thread thread = new Thread(node);
                nodes.add(node);
                threads.add(thread);

                thread.start();
            }catch(SocketTimeoutException ignored){}
        }

        //Close connections and notify Distributor
        sendToDistributor(1);
        for(NodeThread node : nodes){
            node.close();
        }

        for(Thread thread : threads){
            thread.join();
        }
    }

    /**
     * Close the OnionNode server safely
     * @throws IOException
     */
    public void close() throws IOException {
        running = false;
    }

    /**
     * Send a flag to the Distributor. 0 for check-in, 1 for check-out
     * @param flag 0 or 1
     * @throws IOException
     */
    private void sendToDistributor(int flag) throws IOException {
        Socket socket = new Socket(distAddress, distPort);
        OutputStream output = socket.getOutputStream();
        byte[] portArray = ByteBuffer.allocate(4).putInt(portnr).array();

        output.write(flag);
        output.write(portArray);

        socket.close();
    }
}

/**
 * The NodeThread class is called by {@link OnionNode} upon connection with a client
 * or node. This class handles the initial key exchange before it goes into relay mode,
 * encrypting traffic going backwards in the chain and decrypting traffic going forwards in the
 * chain.
 */
class NodeThread implements Runnable{
    private final Socket socket;
    private boolean running = true;

    /**
     * Create a new object to be ran in a seperate thread.
     * @param socket the socket object to communicate with
     */
    public NodeThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            SessionKey sessionKey = Standards.handleKeyExchange(input, output);

            //Read next node address
            byte[] ciphertext = input.readNBytes(Standards.PACKETSIZE);
            byte[] cleartext = sessionKey.decrypt(ciphertext);
            InetAddress nextAddress = InetAddress.getByAddress(Arrays.copyOf(cleartext, 4));
            int nextPort = ByteBuffer.wrap(Arrays.copyOfRange(cleartext, 4, 8)).getInt();

            Socket nextSocket = new Socket(nextAddress, nextPort);
            InputStream nextInput = nextSocket.getInputStream();
            OutputStream nextOutput = nextSocket.getOutputStream();

            //---------------RELAY MODE------------------------
            //Will continiously listen to the sockets in each end to relay incoming data
            while(running){
                if(nextInput.available() > 0){
                    //Read from next node and encrypt
                    cleartext = nextInput.readNBytes(512);
                    ciphertext = sessionKey.encrypt(cleartext);

                    //Send to previous node
                    output.write(ciphertext);
                }
                if(input.available() > 0){
                    //Read from previous node and decrypt
                    ciphertext = input.readNBytes(512);
                    cleartext = sessionKey.decrypt(ciphertext);

                    //Send to next node
                    nextOutput.write(cleartext);
                }
            }

            //Shut down connections
            output.flush();
            nextOutput.flush();
            nextSocket.close();
            socket.close();
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeySpecException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            try {
                socket.close();
            }catch (IOException ignored){}
        }
    }

    public void close(){
        running = false;
    }
}
