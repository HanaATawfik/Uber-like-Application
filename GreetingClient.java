// File Name: GreetingClient.java
import java.io.*;
import java.net.Socket;

public class GreetingClient {
    private static Socket client;
    private static DataOutputStream outToServer;
    private static DataInputStream inFromServer;
    private static BufferedReader userInput;
    private static volatile boolean running = true;
    private static String currentUsername = null;
    private static boolean isCustomer = false;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java GreetingClient <server> <port>");
            return;
        }

        String serverName = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a valid number");
            return;
        }

        try {
            System.out.println("Connecting to " + serverName + " on port " + port);
            client = new Socket(serverName, port);
            System.out.println("Just connected to " + client.getRemoteSocketAddress());

            outToServer = new DataOutputStream(client.getOutputStream());
            inFromServer = new DataInputStream(client.getInputStream());
            userInput = new BufferedReader(new InputStreamReader(System.in));

            // Display main menu
            System.out.println("Please choose whether you are a Customer or a Driver by either entering 1 or 2");
            System.out.println("1. Customer");
            System.out.println("2. Driver");

            int mainChoice = Integer.parseInt(userInput.readLine());
            outToServer.writeUTF(String.valueOf(mainChoice));

            if (mainChoice == 1) {
                isCustomer = true;
                if (handleCustomer()) {
                    // Start listener thread for incoming messages
                    Thread listenerThread = new Thread(new ServerListener());
                    listenerThread.setDaemon(true);
                    listenerThread.start();

                    customerMainLoop();
                }
            } else if (mainChoice == 2) {
                isCustomer = false;
                if (handleDriver()) {
                    // Start listener thread for incoming messages
                    Thread listenerThread = new Thread(new ServerListener());
                    listenerThread.setDaemon(true);
                    listenerThread.start();

                    driverMainLoop();
                }
            } else {
                System.out.println("Invalid choice. Exiting.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static void customerMainLoop() {
        try {
            while (running) {
                System.out.println("\n=== Customer Menu ===");
                System.out.println("1. Request Ride");
                System.out.println("2. View ride status");
                System.out.println("3. Accept a driver bid");
                System.out.println("4. Disconnect");
                System.out.print("Please enter your choice: ");

                int menuChoice = Integer.parseInt(userInput.readLine());
                outToServer.writeUTF(String.valueOf(menuChoice));

                switch (menuChoice) {
                    case 1:
                        handleRideRequest();
                        break;
                    case 2:
                        System.out.println("Checking ride status...");
                        break;
                    case 3:
                        handleAcceptBid();
                        break;
                    case 4:
                        System.out.println("Disconnecting...");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void driverMainLoop() {
        try {
            while (running) {
                System.out.println("\n=== Driver Menu ===");
                System.out.println("1. View and bid on ride requests");
                System.out.println("2. Update ride status");
                System.out.println("3. Disconnect");
                System.out.print("Please enter your choice: ");

                int menuChoice = Integer.parseInt(userInput.readLine());
                outToServer.writeUTF(String.valueOf(menuChoice));

                switch (menuChoice) {
                    case 1:
                        handleOfferFare();
                        break;
                    case 2:
                        System.out.println("Feature not implemented yet");
                        break;
                    case 3:
                        System.out.println("Disconnecting...");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static boolean handleCustomer() throws IOException {
        System.out.println("You chose Customer");
        System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
        System.out.println("1. Sign up");
        System.out.println("2. Log in");

        int choice = Integer.parseInt(userInput.readLine());
        outToServer.writeUTF(String.valueOf(choice));

        if (choice == 1) {
            return handleSignup(true);
        } else if (choice == 2) {
            return handleLogin(true);
        } else {
            System.out.println("Invalid choice. Exiting.");
            return false;
        }
    }

    private static boolean handleDriver() throws IOException {
        System.out.println("You chose Driver");
        System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
        System.out.println("1. Sign up");
        System.out.println("2. Log in");

        int choice = Integer.parseInt(userInput.readLine());
        outToServer.writeUTF(String.valueOf(choice));

        if (choice == 1) {
            return handleSignup(false);
        } else if (choice == 2) {
            return handleLogin(false);
        } else {
            System.out.println("Invalid choice. Exiting.");
            return false;
        }
    }

    private static boolean handleSignup(boolean isCustomer) throws IOException {
        System.out.println("You chose to Sign up");

        System.out.print("Please enter your email: ");
        String email = userInput.readLine();

        System.out.print("Please enter your username: ");
        String username = userInput.readLine();

        System.out.print("Please enter your password: ");
        String password = userInput.readLine();

        outToServer.writeUTF(email);
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.startsWith("SUCCESS")) {
            System.out.println("To proceed, please log in with your username and password");
            return handleLogin(isCustomer);
        }
        return false;
    }

    private static boolean handleLogin(boolean isCustomerFlag) throws IOException {
        System.out.println("You chose to Log in");

        System.out.print("Please enter your username: ");
        String username = userInput.readLine();

        System.out.print("Please enter your password: ");
        String password = userInput.readLine();

        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.startsWith("SUCCESS")) {
            currentUsername = username;
            System.out.println("Welcome " + (isCustomerFlag ? "customer" : "driver") + ", " + username + "!");
            return true;
        } else {
            System.out.println("Login failed. Please try again.");
            return false;
        }
    }

    private static void handleRideRequest() {
        try {
            System.out.print("Please enter your pickup location: ");
            String pickupLocation = userInput.readLine();

            System.out.print("Please enter your drop-off location: ");
            String dropoffLocation = userInput.readLine();

            System.out.print("Please enter your suggested fare (in dollars): ");
            String customerFare = userInput.readLine();

            outToServer.writeUTF(pickupLocation);
            outToServer.writeUTF(dropoffLocation);
            outToServer.writeUTF(customerFare);

            System.out.println("Ride requested from " + pickupLocation + " to " + dropoffLocation);
            System.out.println("Your suggested fare: $" + customerFare);
            System.out.println("Ride request sent to server. Waiting for driver bids...");

        } catch (IOException e) {
            System.out.println("Error processing ride request: " + e.getMessage());
        }
    }

    private static void handleAcceptBid() {
        try {
            System.out.print("Enter the driver username whose bid you want to accept: ");
            String driverUsername = userInput.readLine();
            outToServer.writeUTF(driverUsername);
        } catch (IOException e) {
            System.out.println("Error accepting bid: " + e.getMessage());
        }
    }

    private static void handleOfferFare() {
        try {
            System.out.println("Checking your availability status...");
            // Server will respond with availability and ride requests
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void cleanup() {
        running = false;
        try {
            if (outToServer != null) outToServer.close();
            if (inFromServer != null) inFromServer.close();
            if (client != null) client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listener thread to handle incoming messages from server
    static class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                while (running) {
                    String message = inFromServer.readUTF();

                    if (message.startsWith("BID_NOTIFICATION:")) {
                        // Format: BID_NOTIFICATION:driverUsername:fare:rideId
                        String[] parts = message.substring(17).split(":");
                        System.out.println("\n*** NEW BID RECEIVED ***");
                        System.out.println("Driver: " + parts[0]);
                        System.out.println("Offered Fare: $" + parts[1]);
                        System.out.println("Ride ID: " + parts[2]);
                        System.out.println("Use option 3 to accept this bid.");
                        System.out.println("************************\n");
                    } else if (message.startsWith("RIDE_REQUESTS:")) {
                        // For drivers - display available rides
                        String[] rides = message.substring(14).split("\\|");
                        System.out.println("\nAvailable ride requests:");
                        for (int i = 0; i < rides.length; i++) {
                            System.out.println((i + 1) + ". " + rides[i]);
                        }
                        System.out.print("Enter the number of the ride you want to bid on (or 0 to cancel): ");

                        // Read user's choice
                        String choice = userInput.readLine();
                        outToServer.writeUTF(choice);

                        if (!choice.equals("0")) {
                            String response = inFromServer.readUTF();
                            System.out.println(response);

                            if (response.startsWith("Selected ride:")) {
                                System.out.print("Enter your fare offer (in dollars): ");
                                String fare = userInput.readLine();
                                outToServer.writeUTF(fare);
                            }
                        }
                    } else if (message.startsWith("SUCCESS:") || message.startsWith("FAILURE:") || message.startsWith("INFO:")) {
                        System.out.println("\n" + message);
                    } else {
                        System.out.println("\nServer: " + message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("Connection to server lost.");
                    running = false;
                }
            }
        }
    }
}