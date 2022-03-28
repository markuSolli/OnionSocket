package markussp.onion;

import markussp.onion.util.Standards;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestClient2 {
    private static int portnr = 3010;

    public static void main(String[] args) throws Exception{
        if(args.length > 0){
            portnr = Integer.parseInt(args[0]);
        }

        ServerSocket server = new ServerSocket(portnr);
        Socket client = server.accept();
        InputStream input = client.getInputStream();
        OutputStream output = client.getOutputStream();

        while(true){
            byte[] message = input.readNBytes(Standards.PACKETSIZE);
            String response = "From client: " + new String(message, StandardCharsets.UTF_8).trim();
            System.out.println(response);
            byte[] responseArray = response.getBytes(StandardCharsets.UTF_8);
            message = new byte[Standards.PACKETSIZE];
            System.arraycopy(responseArray, 0, message, 0, responseArray.length);
            output.write(message);
        }
    }
}
