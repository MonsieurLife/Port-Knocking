import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PortKnockingClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int[] KNOCK_PORTS = {7000, 8000, 9000};

    public static void main(String[] args) {
        try {
            InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
            DatagramSocket socket = new DatagramSocket();

            for (int port : KNOCK_PORTS) {
                byte[] data = new byte[1];
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
                System.out.println("Knocked on port: " + port);
                Thread.sleep(1000); // Attendre une seconde entre chaque knock
            }

            socket.close();
            System.out.println("Port knocking sequence completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
