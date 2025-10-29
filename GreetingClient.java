import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GreetingClient {
    private static Socket client;
    private static DataOutputStream outToServer;
    private static DataInputStream inFromServer;
    private static BufferedReader userInput;
    private static volatile boolean running = true;
    private static String currentUsername = null;
    private static boolean isCustomer = false;
    private static List<String[]> receivedBids = new ArrayList<>();

    // Queue for synchronous responses
    private static BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private static ServerListener serverListener;

    static class Menu {
        private final String title;
        private final List<String> options;
        private final BufferedReader input;

        public Menu(String title, List<String> options, BufferedReader input) {
            this.title = title;
            this.options = options;
            this.input = input;
        }

        public int display() throws IOException {
            System.out.println("\n=== " + title + " ===");
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ". " + options.get(i));
            }
            System.out.print("Please enter your choice: ");
            return Integer.parseInt(input.readLine());
        }

        public static Menu createMainMenu(BufferedReader input) {
            return new Menu("Main Menu",
                    List.of("Customer", "Driver"), input);
        }

        public static Menu createAuthMenu(BufferedReader input) {
            return new Menu("Authentication",
                    List.of("Sign up", "Log in"), input);
        }

        public static Menu createCustomerMenu(BufferedReader input) {
            return new Menu("Customer Menu",
                    List.of("Request a Ride", "View Ride Status", "Accept Driver Bid", "Exit"), input);
        }

        public static Menu createDriverMenu(BufferedReader input) {
            return new Menu("Driver Menu",
                    List.of("Offer Ride Fare", "Update Ride Status", "Exit"), input);
        }

        public static Menu createRideStatusMenu(BufferedReader input) {
            return new Menu("Update Ride Status",
                    List.of("Start Ride", "Complete Ride", "Cancel"), input);
        }
    }

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

            Menu mainMenu = Menu.createMainMenu(userInput);
            int mainChoice = getValidMenuChoice(mainMenu, 1, 2);
            outToServer.writeUTF(String.valueOf(mainChoice));

            if (mainChoice == 1) {
                if (handleCustomer()) {
                    // Start ServerListener AFTER authentication
                    serverListener = new ServerListener();
                    new Thread(serverListener).start();
                    customerMainLoop();
                }
            } else {
                if (handleDriver()) {
                    // Start ServerListener AFTER authentication
                    serverListener = new ServerListener();
                    new Thread(serverListener).start();
                    driverMainLoop();
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static int getValidMenuChoice(Menu menu, int min, int max) {
        while (true) {
            try {
                int choice = menu.display();
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.println("Error: Please enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number");
                try {
                    userInput.readLine(); // Clear buffer
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
        }
    }

    private static String getNonEmptyInput(String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String input = userInput.readLine();
            if (input != null && !input.trim().isEmpty()) {
                return input.trim();
            }
            System.out.println("Error: Input cannot be empty. Please try again.");
        }
    }

    private static String getValidNumber(String prompt) throws IOException {
        while (true) {
            String input = getNonEmptyInput(prompt);
            try {
                Double.parseDouble(input);
                return input;
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            }
        }
    }

    // Helper method to read synchronous responses from queue
    private static String readSyncResponse() {
        try {
            return responseQueue.take(); // Blocks until response is available
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for response");
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // Helper method to read synchronous responses with timeout
    private static String readSyncResponse(long timeoutMs) {
        try {
            String response = responseQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (response == null) {
                System.out.println("Timeout waiting for server response");
            }
            return response;
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for response");
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static void customerMainLoop() {
        try {
            Menu customerMenu = Menu.createCustomerMenu(userInput);
            while (running) {
                int choice = getValidMenuChoice(customerMenu, 1, 4);
                outToServer.writeUTF(String.valueOf(choice));
                outToServer.flush();

                if (choice == 4) {
                    System.out.println("Disconnecting...");
                    running = false;
                    break;
                }

                switch (choice) {
                    case 1:
                        handleRideRequest();
                        break;
                    case 2:
                        handleViewRideStatus();
                        break;
                    case 3:
                        handleAcceptBid();
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void driverMainLoop() {
        try {
            Menu driverMenu = Menu.createDriverMenu(userInput);
            while (running) {
                int choice = getValidMenuChoice(driverMenu, 1, 3);
                outToServer.writeUTF(String.valueOf(choice));
                outToServer.flush();

                if (choice == 3) {
                    System.out.println("Disconnecting...");
                    running = false;
                    break;
                }

                switch (choice) {
                    case 1:
                        handleOfferFare();
                        break;
                    case 2:
                        handleUpdateRideStatus();
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static boolean handleCustomer() throws IOException {
        System.out.println("You chose Customer");
        Menu authMenu = Menu.createAuthMenu(userInput);
        int choice = getValidMenuChoice(authMenu, 1, 2);
        outToServer.writeUTF(String.valueOf(choice));
        outToServer.flush();

        if (choice == 1) {
            return handleSignup(true);
        } else {
            return handleLogin(true);
        }
    }

    private static boolean handleDriver() throws IOException {
        System.out.println("You chose Driver");
        Menu authMenu = Menu.createAuthMenu(userInput);
        int choice = getValidMenuChoice(authMenu, 1, 2);
        outToServer.writeUTF(String.valueOf(choice));
        outToServer.flush();

        if (choice == 1) {
            return handleSignup(false);
        } else {
            return handleLogin(false);
        }
    }

    private static boolean handleSignup(boolean isCustomerFlag) throws IOException {
        System.out.println("You chose to Sign up");

        String email = getNonEmptyInput("Please enter your email: ");
        String username = getNonEmptyInput("Please enter your username: ");
        String password = getNonEmptyInput("Please enter your password: ");

        outToServer.writeUTF(email);
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);
        outToServer.flush();

        // Direct read during authentication (before ServerListener starts)
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.startsWith("SUCCESS")) {
            currentUsername = username;
            isCustomer = isCustomerFlag;
            return true;
        }
        return false;
    }

    private static boolean handleLogin(boolean isCustomerFlag) throws IOException {
        System.out.println("You chose to Log in");

        String username = getNonEmptyInput("Please enter your username: ");
        String password = getNonEmptyInput("Please enter your password: ");

        outToServer.writeUTF(username);
        outToServer.writeUTF(password);
        outToServer.flush();

        // Direct read during authentication (before ServerListener starts)
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.startsWith("SUCCESS")) {
            currentUsername = username;
            isCustomer = isCustomerFlag;
            return true;
        }
        return false;
    }

    private static void handleRideRequest() {
        try {
            String pickupLocation = getNonEmptyInput("Enter pickup location: ");
            String dropLocation = getNonEmptyInput("Enter drop-off location: ");
            String fare = getValidNumber("Enter your suggested fare (in dollars): ");

            outToServer.writeUTF(pickupLocation);
            outToServer.writeUTF(dropLocation);
            outToServer.writeUTF(fare);
            outToServer.flush();

            // Read response from queue (ServerListener will put it there)
            String response = readSyncResponse();
            if (response != null) {
                System.out.println(response);
            }

        } catch (IOException e) {
            System.out.println("Error requesting ride: " + e.getMessage());
        }
    }

    private static void handleViewRideStatus() {
        try {
            // Read response from queue
            String statusMessage = readSyncResponse();
            if (statusMessage != null) {
                System.out.println(statusMessage);
            }
        } catch (Exception e) {
            System.out.println("Error viewing ride status: " + e.getMessage());
        }
    }

    private static void handleAcceptBid() {
        try {
            if (receivedBids.isEmpty()) {
                System.out.println("No bids received yet.");
                // Still need to read server response for this menu choice
                String response = readSyncResponse();
                if (response != null) {
                    System.out.println(response);
                }
                return;
            }

            System.out.println("\n=== Received Bids ===");
            for (int i = 0; i < receivedBids.size(); i++) {
                String[] bid = receivedBids.get(i);
                System.out.println((i + 1) + ". Driver: " + bid[0] + ", Fare: $" + bid[1] + ", Ride ID: " + bid[2]);
            }

            String choice = getNonEmptyInput("Enter the number of the bid to accept (or 0 to cancel): ");
            int bidIndex;

            try {
                bidIndex = Integer.parseInt(choice) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                return;
            }

            if (bidIndex < 0 || bidIndex >= receivedBids.size()) {
                System.out.println("Invalid choice or cancelled.");
                return;
            }

            String[] selectedBid = receivedBids.get(bidIndex);
            String driverUsername = selectedBid[0];

            outToServer.writeUTF(driverUsername);
            outToServer.flush();

            String response = readSyncResponse();
            if (response != null) {
                System.out.println(response);

                if (response.startsWith("SUCCESS")) {
                    receivedBids.clear();
                }
            }
        } catch (IOException e) {
            System.out.println("Error accepting bid: " + e.getMessage());
        }
    }

    private static void handleOfferFare() {
        try {
            System.out.println("Checking your availability status...");

            String availabilityResponse = readSyncResponse();
            if (availabilityResponse == null) return;

            System.out.println(availabilityResponse);

            if (availabilityResponse.startsWith("FAILURE:")) {
                return;
            }

            String rideRequestsMessage = readSyncResponse();
            if (rideRequestsMessage == null) return;

            if (rideRequestsMessage.startsWith("INFO: No ride requests")) {
                System.out.println(rideRequestsMessage);
                return;
            }

            if (rideRequestsMessage.startsWith("RIDE_REQUESTS:")) {
                String ridesData = rideRequestsMessage.substring("RIDE_REQUESTS:".length());
                String[] rides = ridesData.split("\\|");

                System.out.println("\n=== Available Ride Requests ===");
                for (int i = 0; i < rides.length; i++) {
                    System.out.println((i + 1) + ". " + rides[i]);
                }

                String choice = getNonEmptyInput("Enter the number of the ride to bid on (or 0 to cancel): ");
                outToServer.writeUTF(choice);
                outToServer.flush();

                String selectedRideResponse = readSyncResponse();
                if (selectedRideResponse == null) return;

                if (selectedRideResponse.startsWith("INFO:") || selectedRideResponse.startsWith("FAILURE:")) {
                    System.out.println(selectedRideResponse);
                    return;
                }

                if (selectedRideResponse.startsWith("Selected ride:")) {
                    System.out.println(selectedRideResponse);

                    String fareOffer = getValidNumber("Enter your fare offer (in dollars): ");
                    outToServer.writeUTF(fareOffer);
                    outToServer.flush();

                    String bidConfirmation = readSyncResponse();
                    if (bidConfirmation != null) {
                        System.out.println(bidConfirmation);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error processing ride bids: " + e.getMessage());
        }
    }

    private static void handleUpdateRideStatus() {
        try {
            String statusMessage = readSyncResponse();
            if (statusMessage == null) return;

            if (statusMessage.startsWith("INFO: You have no active ride")) {
                System.out.println(statusMessage);
                return;
            }

            if (statusMessage.startsWith("RIDE_STATUS_MENU:")) {
                String currentStatus = statusMessage.substring("RIDE_STATUS_MENU:".length());
                System.out.println(currentStatus);

                Menu statusMenu = Menu.createRideStatusMenu(userInput);
                int choice = getValidMenuChoice(statusMenu, 1, 3);
                outToServer.writeUTF(String.valueOf(choice));
                outToServer.flush();

                String response = readSyncResponse();
                if (response != null) {
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            System.out.println("Error updating ride status: " + e.getMessage());
        }
    }

    private static void cleanup() {
        running = false;
        try {
            // Give ServerListener time to finish
            Thread.sleep(100);

            if (userInput != null) userInput.close();
            if (outToServer != null) outToServer.close();
            if (inFromServer != null) inFromServer.close();
            if (client != null) client.close();

            System.out.println("Connection closed successfully.");
        } catch (IOException e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                while (running) {
                    String message = inFromServer.readUTF();

                    // Handle asynchronous notifications (pushed by server without request)
                    if (message.startsWith("BID_NOTIFICATION:")) {
                        handleBidNotification(message);
                    }
                    // Handle other async messages that don't expect a response
                    else if (isAsyncNotification(message)) {
                        System.out.println("\n[SERVER] " + message);
                        System.out.print("Please enter your choice: ");
                        System.out.flush();
                    }
                    // All other messages are synchronous responses - put in queue
                    else {
                        responseQueue.offer(message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("\nConnection to server lost: " + e.getMessage());
                    running = false;
                }
            }
        }

        private boolean isAsyncNotification(String message) {
            // These are messages pushed by server without a direct client request
            return (message.startsWith("INFO:") && message.contains("has been assigned to another driver"))
                    || (message.startsWith("INFO:") && message.contains("driver has started"))
                    || (message.startsWith("SUCCESS:") && message.contains("ride has been completed"))
                    || (message.startsWith("SUCCESS:") && message.contains("accepted your bid"));
        }

        private void handleBidNotification(String message) {
            try {
                String[] parts = message.substring("BID_NOTIFICATION:".length()).split(":");
                if (parts.length >= 3) {
                    String driverUsername = parts[0];
                    String fareOffer = parts[1];
                    String rideId = parts[2];

                    receivedBids.add(new String[]{driverUsername, fareOffer, rideId});

                    System.out.println("\n╔════════════════════════════════════════╗");
                    System.out.println("║           NEW BID RECEIVED!            ║");
                    System.out.println("╠════════════════════════════════════════╣");
                    System.out.println("║ Driver: " + String.format("%-30s", driverUsername) + "║");
                    System.out.println("║ Fare:   $" + String.format("%-29s", fareOffer) + "║");
                    System.out.println("║ Ride:   " + String.format("%-30s", rideId) + "║");
                    System.out.println("╚════════════════════════════════════════╝");
                    System.out.print("Please enter your choice: ");
                    System.out.flush();
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR] Failed to parse bid notification: " + e.getMessage());
            }
        }
    }
}