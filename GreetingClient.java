// File Name GreetingClient.java
import java.net.*;
import java.io.*;
import java.util.Scanner;

public class GreetingClient {

    public static void main(String [] args) {
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        try {
           System.out.println("Connecting to " + serverName + " on port " + port);

            Socket client = new Socket(serverName, port);

            System.out.println("Just connected to " + client.getRemoteSocketAddress());

            System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
            System.out.println("1. Sign up");
            System.out.println("2. Log in");

            BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
            int choice = Integer.parseInt(buffReader.readLine());

            if (choice == 1) {
                System.out.println("You chose to Sign up");
                System.out.println("Please enter your email:");
                String email = buffReader.readLine();
                System.out.println("Please enter your username:");
                String username = buffReader.readLine();
                System.out.println("Please enter your password:");
                String password = buffReader.readLine();

                // Send sign-up data to server
                DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
                outToServer.writeUTF("SignUp:" + email + ":" + username + ":" + password);
            } else if (choice == 2) {
                System.out.println("You chose to Log in");
                System.out.println("Please enter your username:");
                String username = buffReader.readLine();
                System.out.println("Please enter your password:");
                String password = buffReader.readLine();

                // Send login data to server
                DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
                outToServer.writeUTF("Login:" + username + ":" + password);
            } else {
                System.out.println("Invalid choice. Exiting.");
                client.close();
                return;
            }


            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            System.out.println("Server says " + in.readUTF());


            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}