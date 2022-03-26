package markussp.onion;

import markussp.onion.router.OnionSocket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class TestClient {

    public static void main(String[] args) throws Exception{
        OnionSocket socket = new OnionSocket(InetAddress.getByName("localhost"), 3010);

        byte[] message = ("Hello from client 1").getBytes(StandardCharsets.UTF_8);
        socket.send(message);

        message = socket.read();
        System.out.println(new String(message, StandardCharsets.UTF_8).trim());
    }
}
