package markussp.onion.model;

import java.net.InetAddress;

/**
 * The Address class is for holding complete network addresses,
 * both {@code InetAddress} and {@code port}.
 */
public class Address {
    public InetAddress address;
    public int port;

    /**
     * Store a complete network address.
     * @param address the IP-address.
     * @param port the portnumber.
     */
    public Address(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    @Override
    public String toString() {
        return address.getHostAddress() + ":" + port;
    }
}
