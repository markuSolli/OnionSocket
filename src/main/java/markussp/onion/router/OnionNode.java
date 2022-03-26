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

public class OnionNode {
    private final int portnr;
    private final String distAddress;
    private final int distPort;
    private boolean running = true;

    public OnionNode(int portnr) throws IOException {
        this(portnr, Standards.DIST, Standards.DISTPORT);
    }

    public OnionNode(String distAddress, int distPort) throws IOException {
        this(Standards.PORTNR, distAddress, distPort);
    }

    public OnionNode(int portnr, String distAddress, int distPort) throws IOException {
        this.portnr = portnr;
        this.distAddress = distAddress;
        this.distPort = distPort;
    }

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

    public void close() throws IOException {
        running = false;
    }

    private void sendToDistributor(int flag) throws IOException {
        Socket socket = new Socket(distAddress, distPort);
        OutputStream output = socket.getOutputStream();
        byte[] portArray = ByteBuffer.allocate(4).putInt(portnr).array();

        output.write(flag);
        output.write(portArray);

        socket.close();
    }
}

class NodeThread implements Runnable{
    private final Socket socket;
    private boolean running = true;

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
