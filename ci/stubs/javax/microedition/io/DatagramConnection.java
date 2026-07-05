package javax.microedition.io;

import java.io.IOException;

public interface DatagramConnection extends Connection {
    Datagram newDatagram(byte[] buf, int size) throws IOException;
    void send(Datagram dgram) throws IOException;
    void receive(Datagram dgram) throws IOException;
}
