package markussp.onion.router;

import markussp.onion.model.Address;
import markussp.onion.model.SessionKey;
import markussp.onion.util.Standards;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Random;

public class Distributor {
    private static int portnr = Standards.DISTPORT;
    private static final ArrayList<Address> nodes = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        //Override standard portnumber if given
        if(args.length > 0){
            portnr = Integer.parseInt(args[0]);
        }

        //Launch server
        ServerSocket server = new ServerSocket(portnr);

        //Listen on port and send new connections to their own thread
        while(true){
            Socket socket = server.accept();
            new Thread(new DistributorThread(socket)).start();
        }
    }

    static synchronized int getSize(){
        return nodes.size();
    }

    static synchronized Address getNode(int index){
        return nodes.get(index);
    }

    static synchronized void checkIn(Address address){
        nodes.add(address);
    }

    static synchronized void checkOut(Address address){
        nodes.remove(address);
    }
}

class DistributorThread implements Runnable{
    private final Socket socket;

    DistributorThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            int flag = input.read();
            if(flag == 0){                  //Node check-in
                int port = ByteBuffer.wrap(input.readNBytes(4)).getInt();
                Address address = new Address(socket.getInetAddress(), port);
                Distributor.checkIn(address);
            }else if(flag == 1){            //Node check-out
                int port = ByteBuffer.wrap(input.readNBytes(4)).getInt();
                Address address = new Address(socket.getInetAddress(), port);
                Distributor.checkOut(address);
            }else if(flag == 2){            //Client node-request
                SessionKey sessionKey = Standards.handleKeyExchange(input, output);

                byte[] message = new byte[Standards.PACKETSIZE];
                Random random = new Random();
                ArrayList<Integer> used = new ArrayList<>(Standards.NODES);
                int size = Distributor.getSize();

                if(size >= Standards.NODES){
                    //Give three nodes to the client
                    for(int i=0; i<Standards.NODES; i++){
                        int index;

                        //Find an unused node
                        while(used.contains(index = random.nextInt(size)));

                        //Store the nodes address
                        Address address = Distributor.getNode(index);
                        used.add(index);

                        byte[] addArray = address.address.getAddress();
                        byte[] portArray = ByteBuffer.allocate(4).putInt(address.port).array();
                        System.arraycopy(addArray, 0, message, i*8, 4);
                        System.arraycopy(portArray, 0, message, i*8 + 4, 4);
                    }

                    //Send the chosen nodes to the client
                    output.write(sessionKey.encrypt(message));
                }
            }

            output.flush();
            socket.close();
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeySpecException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
