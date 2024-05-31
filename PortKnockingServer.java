import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.net.*;

public class PortKnockingServer {
    private static final int[] KNOCK_PORTS = {7000, 8000, 9000}; // Ports de la séquence de knocking
    private static final int SERVICE_PORT = 22; // Port du service à ouvrir
    private static final int TIMEOUT = 5000; // Temps maximum entre les knocks en millisecondes

    private List<ClientKnock> clientKnocks = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new PortKnockingServer().startServer();
    }

    public void startServer() throws IOException {
        // Démarrer un thread pour chaque port de knocking
        for (int port : KNOCK_PORTS) {
            new Thread(new KnockListener(port)).start();
        }

        // Démarrer le service
        ServerSocket serviceSocket = new ServerSocket(SERVICE_PORT);
        System.out.println("Service is running on port " + SERVICE_PORT);

        while (true) {
            Socket clientSocket = serviceSocket.accept();
            InetAddress clientAddress = clientSocket.getInetAddress();
            System.out.println("Service accessed by: " + clientAddress);
            // Traiter la connexion du client
            clientSocket.close();
        }
    }

    private class KnockListener implements Runnable {
        private int port;

        public KnockListener(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    InetAddress address = packet.getAddress();
                    processKnock(address, port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processKnock(InetAddress clientAddress, int port) {
            synchronized (clientKnocks) {
                ClientKnock clientKnock = getClientKnock(clientAddress);
                if (clientKnock == null) {
                    clientKnock = new ClientKnock(clientAddress);
                    clientKnocks.add(clientKnock);
                }

                if (clientKnock.processKnock(port)) {
                    System.out.println("Port knocking sequence completed by: " + clientAddress);
                    //openServicePort(clientAddress);
                    openSSHConnection(clientAddress);
                    clientKnocks.remove(clientKnock);
                }
            }
        }

        private ClientKnock getClientKnock(InetAddress clientAddress) {
            for (ClientKnock clientKnock : clientKnocks) {
                if (clientKnock.getAddress().equals(clientAddress)) {
                    return clientKnock;
                }
            }
            return null;
        }

        private void openServicePort(InetAddress clientAddress) {
            // Ouvrir le port de service pour le client spécifié
            System.out.println("Service port opened for: " + clientAddress);
        }   
    }

    private class ClientKnock {
        private InetAddress address;
        private int currentStep;
        private long lastKnockTime;

        public ClientKnock(InetAddress address) {
            this.address = address;
            this.currentStep = 0;
            this.lastKnockTime = System.currentTimeMillis();
        }

        public InetAddress getAddress() {
            return address;
        }

        public boolean processKnock(int port) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKnockTime > TIMEOUT) {
                currentStep = 0;
            }
            lastKnockTime = currentTime;

            if (KNOCK_PORTS[currentStep] == port) {
                currentStep++;
                if (currentStep == KNOCK_PORTS.length) {
                    return true;
                }
            } else {
                currentStep = 0;
            }

            return false;
        }
    }
}
