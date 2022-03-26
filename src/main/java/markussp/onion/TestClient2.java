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

        byte[] message = input.readNBytes(Standards.PACKETSIZE);

        System.out.println(new String(message, StandardCharsets.UTF_8).trim());

        message = new byte[Standards.PACKETSIZE];
        byte[] string = ("Hello from client 2").getBytes();
        System.arraycopy(string, 0, message, 0, string.length);

        output.write(message);

        client.close();
        server.close();
    }
}
