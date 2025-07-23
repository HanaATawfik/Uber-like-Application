// File Name GreetingServer.java
import java.net.*;
import java.io.*;

public class GreetingServer extends Thread {
    private ServerSocket serverSocket;

    public GreetingServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(100000);
    }

    public void run() {
        while(true) {
            try {
               System.out.println("Waiting for client on port " +
                      serverSocket.getLocalPort() + "...");

                Socket server = serverSocket.accept();

                System.out.println("Just connected to " + server.getRemoteSocketAddress());

                DataInputStream in = new DataInputStream(server.getInputStream());
                int choice = Integer.parseInt(in.readUTF());

                if (choice==1)
                {
                    System.out.println("Client chose to Sign up");
                    String email = in.readUTF();
                    String username = in.readUTF();
                    String password = in.readUTF();
                    System.out.println("Received Sign up data: Email: " + email + ", Username: " + username + ", Password: " + password);
                }
                else if (choice==2)
                {
                    System.out.println("Client chose to Log in");
                    String loginData = in.readUTF();
                    String[] parts = loginData.split(":");
                    String username = parts[1];
                    String password = parts[2];
                    System.out.println("Received Log in data: Username: " + username + ", Password: " + password);
                }
                else
                {
                    System.out.println("Invalid choice received from client.");
                }

                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                    + "\nGoodbye!");

                server.close();

            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String [] args) {
        int port = Integer.parseInt(args[0]);
        try {
            Thread t = new GreetingServer(port);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}