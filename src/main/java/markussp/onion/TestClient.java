package markussp.onion;

import markussp.onion.router.OnionSocket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TestClient {

    public static void main(String[] args) throws Exception{
        OnionSocket socket = new OnionSocket(InetAddress.getByName("localhost"), 3010);

        System.out.println("Connected to client through onion network!");
        System.out.println("Write messages to the client, type 'x' to exit");

        Scanner scanner = new Scanner(System.in);
        String line;
        while(!(line = scanner.nextLine()).equals("x")){
            byte[] message = line.getBytes(StandardCharsets.UTF_8);
            socket.send(message);
            message = socket.read();
            System.out.println(new String(message, StandardCharsets.UTF_8).trim());
        }
        socket.close();
    }
}
