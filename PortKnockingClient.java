import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PortKnockingClient {
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^(([0-9]{1,3})\\.){3}([0-9]{1,3})$"
    ); // Pattern pour valider une adresse IP

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Demande et vérification de l'adresse IP du serveur
        System.out.print("Enter server IP address: ");
        String serverAddress = scanner.nextLine();
        while (!isValidIPAddress(serverAddress)) {
            System.out.println("Invalid IP address. Please try again.");
            System.out.print("Enter server IP address: ");
            serverAddress = scanner.nextLine();
        }

        // Demande et validation de la séquence de ports de knocking
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
            // Résolution de l'adresse IP du serveur
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramSocket socket = new DatagramSocket();

            // Envoi de paquets UDP pour chaque port de la séquence de knocking
            for (int port : knockPorts) {
                byte[] data = new byte[1];
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet); // Envoi du paquet
                System.out.println("Knocked on port: " + port);
                Thread.sleep(1000); // Attendre une seconde entre chaque knock
            }

            socket.close();
            System.out.println("Port knocking sequence completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode pour valider une adresse IP
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
