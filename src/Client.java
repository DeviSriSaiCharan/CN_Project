import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private static final int BUFFER_SIZE = 4096;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            out.writeUTF(username);

            // Start message receiving thread
            new Thread(this::receiveMessages).start();

            // Main loop for sending messages
            while (true) {
                String input = scanner.nextLine();
                if (input.equals("exit")) {
                    out.writeUTF("exit");
                    break;
                } else if (input.startsWith("/file")) {
                    handleFileSend(input);
                } else {
                    out.writeUTF(input);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                String message = in.readUTF();

                if (message.startsWith("/file")) {
                    handleFileReceive(message);
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    private void handleFileSend(String input) {
        try {
            String[] parts = input.split(" ", 2);
            if (parts.length != 2) {
                System.out.println("Usage: /file <filename>");
                return;
            }

            File file = new File(parts[1]);
            if (!file.exists()) {
                System.out.println("File not found: " + parts[1]);
                return;
            }

            // Send file transfer request
            out.writeUTF("/file " + file.getName());
            out.writeLong(file.length());

            // Send file data
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
            System.out.println("File sent successfully");
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private void handleFileReceive(String message) {
        try {
            String[] parts = message.split(" ");
            if (parts.length != 4) return;

            String filename = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            String sender = parts[3];

            System.out.println("Receiving file '" + filename + "' from " + sender);
            System.out.println("File size: " + fileSize + " bytes");

            File file = new File("received_" + filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = fileSize;

                while (remaining > 0) {
                    int bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            System.out.println("File received successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 3000);
            client.start();
        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }

}