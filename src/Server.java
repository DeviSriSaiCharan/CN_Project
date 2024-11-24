import java.net.*;
import java.util.*;
import java.io.*;
import static java.util.Collections.synchronizedList;

public class Server {
    private static final List<ClientHandler> clients = synchronizedList(new ArrayList<>());
    private static final int PORT = 3000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started at port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                client.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ClientHandler> getClients() {
        return clients;
    }

    public static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static ClientHandler findClientByUsername(String username) {
        return clients.stream()
                .filter(client -> client.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}