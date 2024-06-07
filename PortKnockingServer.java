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
    private static final int SERVICE_PORT = 22; // Port du service à ouvrir (SSH)
    private static final int TIMEOUT = 5000; // Temps maximum entre les knocks en millisecondes (5 secondes)
    private static final int OPEN_PORT_DURATION = 600000; // Durée pendant laquelle le port reste ouvert (600 secondes ou 10 minutes)
    private final int[] knockPorts; // Séquence des ports de knocking
    private final List<ClientKnock> clientKnocks = new ArrayList<>(); // Liste des tentatives de knocking des clients

    // Constructeur prenant en paramètre la séquence de ports de knocking
    public PortKnockingServer(int[] knockPorts) {
        this.knockPorts = knockPorts;
    }

    // Méthode pour démarrer le serveur de port knocking
    public void startServer() throws IOException {
        // Démarrer un thread pour chaque port de knocking
        for (int port : knockPorts) {
            new Thread(new KnockListener(port)).start();
        }

        System.out.println("Port knocking server is running...");
    }

    // Classe interne pour écouter les knocks sur un port spécifique
    private class KnockListener implements Runnable {
        private final int port;

        // Constructeur prenant en paramètre le port à écouter
        public KnockListener(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // Réception d'un paquet UDP
                    InetAddress address = packet.getAddress(); // Adresse du client
                    processKnock(address, port); // Traitement du knock
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Méthode pour traiter un knock d'un client sur un port donné
        private void processKnock(InetAddress clientAddress, int port) {
            synchronized (clientKnocks) {
                ClientKnock clientKnock = getClientKnock(clientAddress);
                if (clientKnock == null) {
                    clientKnock = new ClientKnock(clientAddress); // Créer un nouvel objet ClientKnock si non existant
                    clientKnocks.add(clientKnock);
                }

                // Si la séquence de knocking est complète
                if (clientKnock.processKnock(port)) {
                    System.out.println("Port knocking sequence completed by: " + clientAddress);
                    openServicePort(clientAddress); // Ouvrir le port du service pour ce client
                    clientKnocks.remove(clientKnock); // Retirer le client de la liste
                }
            }
        }

        // Méthode pour obtenir un ClientKnock pour une adresse spécifique
        private ClientKnock getClientKnock(InetAddress clientAddress) {
            for (ClientKnock clientKnock : clientKnocks) {
                if (clientKnock.getAddress().equals(clientAddress)) {
                    return clientKnock;
                }
            }
            return null;
        }

        // Méthode pour ouvrir le port du service pour une adresse client
        private void openServicePort(InetAddress clientAddress) {
            String ipAddress = clientAddress.getHostAddress();
            String addRuleCommand = String.format("sudo iptables -A INPUT -p tcp -s %s --dport %d -j ACCEPT", ipAddress, SERVICE_PORT);
            String deleteRuleCommand = String.format("sudo iptables -D INPUT -p tcp -s %s --dport %d -j ACCEPT", ipAddress, SERVICE_PORT);

            try {
                // Ajouter la règle iptables pour autoriser l'accès SSH
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

    // Classe interne pour représenter une tentative de knocking d'un client
    private class ClientKnock {
        private final InetAddress address;
        private int currentStep; // Étape actuelle de la séquence de knocking
        private long lastKnockTime; // Heure du dernier knock

        public ClientKnock(InetAddress address) {
            this.address = address;
            this.currentStep = 0;
            this.lastKnockTime = System.currentTimeMillis();
        }

        public InetAddress getAddress() {
            return address;
        }

        // Méthode pour traiter un knock sur un port spécifique
        public boolean processKnock(int port) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKnockTime > TIMEOUT) {
                currentStep = 0; // Réinitialiser la séquence si le temps écoulé entre les knocks est trop long
            }
            lastKnockTime = currentTime;

            if (knockPorts[currentStep] == port) {
                currentStep++; // Passer à l'étape suivante
                if (currentStep == knockPorts.length) {
                    return true; // Séquence de knocking complète
                }
            } else {
                currentStep = 0; // Réinitialiser la séquence si le knock ne correspond pas
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
            knockPorts[i] = scanner.nextInt(); // Lire la séquence de ports de knocking
        }

        try {
            PortKnockingServer server = new PortKnockingServer(knockPorts);
            server.startServer(); // Démarrer le serveur de port knocking
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
