
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class PortKnockingServer {
    private static final int SERVICE_PORT = 22; // Port du service à ouvrir
    private static final int TIMEOUT = 5000; // Temps maximum entre les knocks en millisecondes
    private static final int OPEN_PORT_DURATION = 600000; // Durée pendant laquelle le port reste ouvert (en millisecondes)
    private final int[] knockPorts;
    private final List<ClientKnock> clientKnocks = new ArrayList<>();

    public PortKnockingServer(int[] knockPorts) {
        this.knockPorts = knockPorts;
    }

    public void startServer() throws IOException {
        // Démarrer un thread pour chaque port de knocking
        for (int port : knockPorts) {
            new Thread(new KnockListener(port)).start();
        }

        System.out.println("Port knocking server is running...");
    }

    private class KnockListener implements Runnable {
        private final int port;

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
                    openServicePort(clientAddress);
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
            String ipAddress = clientAddress.getHostAddress();
            String addRuleCommand = String.format("sudo iptables -A INPUT -p tcp -s %s --dport %d -j ACCEPT", ipAddress, SERVICE_PORT);
            String deleteRuleCommand = String.format("sudo iptables -D INPUT -p tcp -s %s --dport %d -j ACCEPT", ipAddress, SERVICE_PORT);

            try {
                // Ajouter la règle iptables
                Process addProcess = Runtime.getRuntime().exec(new String[]{"bash", "-c", addRuleCommand});
                int addExitCode = addProcess.waitFor();
                if (addExitCode == 0) {
                    System.out.println("SSH access granted to: " + clientAddress);

                    // Planifier la suppression de la règle après OPEN_PORT_DURATION millisecondes
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                Process deleteProcess = Runtime.getRuntime().exec(new String[]{"bash", "-c", deleteRuleCommand});
                                int deleteExitCode = deleteProcess.waitFor();
                                if (deleteExitCode == 0) {
                                    System.out.println("SSH access revoked for: " + clientAddress);
                                } else {
                                    System.out.println("Failed to revoke SSH access for: " + clientAddress);
                                }
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }, OPEN_PORT_DURATION);
                } else {
                    System.out.println("Failed to grant SSH access to: " + clientAddress);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientKnock {
        private final InetAddress address;
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

            if (knockPorts[currentStep] == port) {
                currentStep++;
                if (currentStep == knockPorts.length) {
                    return true;
                }
            } else {
                currentStep = 0;
            }

            return false;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] knockPorts = new int[3];

        System.out.println("Enter the port knocking sequence:");

        for (int i = 0; i < 3; i++) {
            System.out.print("Port " + (i + 1) + ": ");
            knockPorts[i] = scanner.nextInt();
        }

        try {
            PortKnockingServer server = new PortKnockingServer(knockPorts);
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
