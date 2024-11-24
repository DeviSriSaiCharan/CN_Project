import java.io.*;
import java.net.Socket;

class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private ClientHandler connectedClient;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Get username from client
            username = in.readUTF();
            System.out.println(username + " connected");

            // Broadcast updated user list to all clients
            broadcastConnectedUsers();

            while (true) {
                String msg = in.readUTF();

                if (msg.equals("exit")) {
                    disconnect();
                    break;
                } else if (msg.startsWith("/connect")) {
                    handleConnectionRequest(msg);
                } else if (msg.equals("/list")) {
                    listConnectedUsers();
                } else if (msg.startsWith("/approve")) {
                    handleConnectionApproval(msg);
                } else if (msg.startsWith("/file")) {
                    handleFileTransfer(msg);/
                } else {
                    if (connectedClient != null) {
                        // Only send messages to connected client
                        connectedClient.sendMessage(username + ": " + msg);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + username);
            disconnect();
        }
    }

    private void handleFileTransfer(String msg) throws IOException {
        String[] parts = msg.split(" ", 2);
        if (parts.length != 2) return;

        String filename = parts[1];
        long fileSize = in.readLong();

        if (connectedClient != null) {
            // Notify recipient about incoming file
            connectedClient.sendMessage("/file " + filename + " " + fileSize + " " + username);

            // Read file data and forward to connected client
            byte[] buffer = new byte[4096];
            long remaining = fileSize;

            while (remaining > 0) {
                int bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead == -1) break;
                connectedClient.out.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            connectedClient.out.flush();
            sendMessage("File sent successfully");
        }
    }

    void sendMessage(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastConnectedUsers() {
        StringBuilder userList = new StringBuilder("/users ");
        for (ClientHandler client : Server.getClients()) {
            userList.append(client.username).append(",");
        }
        if (userList.length() > 7) {
            userList.setLength(userList.length() - 1);
        }
        Server.broadcastMessage(userList.toString());
    }

    private void handleConnectionRequest(String msg) throws IOException {
        String[] parts = msg.split(" ", 2);
        if (parts.length != 2) return;

        String targetUsername = parts[1];
        ClientHandler target = Server.findClientByUsername(targetUsername);

        if (target != null) {
            target.sendMessage("/connect_request " + username);
        } else {
            sendMessage("User not found: " + targetUsername);
        }
    }

    private void handleConnectionApproval(String msg) throws IOException {
        String[] parts = msg.split(" ", 2);
        if (parts.length != 2) return;

        String requesterUsername = parts[1];
        ClientHandler requester = Server.findClientByUsername(requesterUsername);

        if (requester != null) {
            this.connectedClient = requester;
            requester.connectedClient = this;
            requester.sendMessage(username + " has accepted your connection request");
            sendMessage("Connection established with " + requesterUsername);
        } else {
            sendMessage("User not found");
        }
    }

    private void listConnectedUsers() throws IOException {
        StringBuilder userList = new StringBuilder("Connected users: ");
        for (ClientHandler client : Server.getClients()) {
            if (client != this) {
                userList.append(client.username).append(", ");
            }
        }
        if (userList.length() > 16) {
            userList.setLength(userList.length() - 2);
        }
        sendMessage(userList.toString());
    }

    private void disconnect() {
        try {
            Server.getClients().remove(this);
            if (connectedClient != null) {
                connectedClient.connectedClient = null;
                connectedClient.sendMessage(username + " has disconnected");
            }
            broadcastConnectedUsers();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}