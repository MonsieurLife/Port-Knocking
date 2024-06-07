
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PortKnockingClient {
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^(([0-9]{1,3})\\.){3}([0-9]{1,3})$"
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter server IP address: ");
        String serverAddress = scanner.nextLine();

        while (!isValidIPAddress(serverAddress)) {
            System.out.println("Invalid IP address. Please try again.");
            System.out.print("Enter server IP address: ");
            serverAddress = scanner.nextLine();
        }

        int[] knockPorts = new int[3];
        System.out.println("Enter the port knocking sequence:");

        for (int i = 0; i < 3; i++) {
            System.out.print("Port " + (i + 1) + ": ");
            while (!scanner.hasNextInt()) {
                System.out.println("Invalid port number. Please enter a valid integer.");
                System.out.print("Port " + (i + 1) + ": ");
                scanner.next();
            }
            knockPorts[i] = scanner.nextInt();
        }

        try {
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramSocket socket = new DatagramSocket();

            for (int port : knockPorts) {
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

    private static boolean isValidIPAddress(String ip) {
        if (IP_ADDRESS_PATTERN.matcher(ip).matches()) {
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                int number = Integer.parseInt(part);
                if (number < 0 || number > 255) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
