import java.io.*;
                    import java.net.*;
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.Arrays;

                    public class GreetingClient {
                        private static Socket client;
                        private static DataOutputStream outToServer;
                        private static DataInputStream inFromServer;
                        private static BufferedReader userInput;
                        private static volatile boolean running = true;
                        private static String currentUsername = null;
                        private static boolean isCustomer = false;
                        private static List<String[]> receivedBids = new ArrayList<>();

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
                                        Arrays.asList("Customer", "Driver"),
                                        input);
                            }

                            public static Menu createAuthMenu(BufferedReader input) {
                                return new Menu("Authentication",
                                        Arrays.asList("Sign up", "Log in"),
                                        input);
                            }

                            public static Menu createCustomerMenu(BufferedReader input) {
                                return new Menu("Customer Menu",
                                        Arrays.asList("Request a Ride", "View Ride Status", "Accept Driver Bid", "Exit"),
                                        input);
                            }

                            public static Menu createDriverMenu(BufferedReader input) {
                                return new Menu("Driver Menu",
                                        Arrays.asList("Offer Ride Fare", "Update Ride Status", "Exit"),
                                        input);
                            }

                            public static Menu createRideStatusMenu(BufferedReader input) {
                                return new Menu("Update Ride Status",
                                        Arrays.asList("Start Ride", "Complete Ride", "Cancel"),
                                        input);
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
                                    isCustomer = true;
                                    if (handleCustomer()) {
                                        Thread listenerThread = new Thread(new ServerListener());
                                        listenerThread.setDaemon(true);
                                        listenerThread.start();
                                        customerMainLoop();
                                    }
                                } else if (mainChoice == 2) {
                                    isCustomer = false;
                                    if (handleDriver()) {
                                        Thread listenerThread = new Thread(new ServerListener());
                                        listenerThread.setDaemon(true);
                                        listenerThread.start();
                                        driverMainLoop();
                                    }
                                }

                            } catch (IOException e) {
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
                                    System.out.println("Error: Please enter a valid number.");
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

                        private static void customerMainLoop() {
                            try {
                                Menu customerMenu = Menu.createCustomerMenu(userInput);
                                while (running) {
                                    int menuChoice = getValidMenuChoice(customerMenu, 1, 4);
                                    outToServer.writeUTF(String.valueOf(menuChoice));

                                    switch (menuChoice) {
                                        case 1:
                                            handleRideRequest();
                                            break;
                                        case 2:
                                            handleViewRideStatus();
                                            break;
                                        case 3:
                                            handleAcceptBid();
                                            break;
                                        case 4:
                                            System.out.println("Disconnecting...");
                                            running = false;
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
                                    int menuChoice = getValidMenuChoice(driverMenu, 1, 3);
                                    outToServer.writeUTF(String.valueOf(menuChoice));

                                    switch (menuChoice) {
                                        case 1:
                                            handleOfferFare();
                                            break;
                                        case 2:
                                            handleUpdateRideStatus();
                                            break;
                                        case 3:
                                            System.out.println("Disconnecting...");
                                            running = false;
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

                            if (choice == 1) {
                                return handleSignup(false);
                            } else {
                                return handleLogin(false);
                            }
                        }

                        private static boolean handleSignup(boolean isCustomer) throws IOException {
                            System.out.println("You chose to Sign up");

                            String email = getNonEmptyInput("Please enter your email: ");
                            String username = getNonEmptyInput("Please enter your username: ");
                            String password = getNonEmptyInput("Please enter your password: ");

                            outToServer.writeUTF(email);
                            outToServer.writeUTF(username);
                            outToServer.writeUTF(password);

                            String serverResponse = inFromServer.readUTF();
                            System.out.println("Server response: " + serverResponse);

                            if (serverResponse.startsWith("SUCCESS")) {
                                currentUsername = username;
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

                                String response = inFromServer.readUTF();
                                System.out.println(response);
                            } catch (IOException e) {
                                System.out.println("Error requesting ride: " + e.getMessage());
                            }
                        }

                        private static void handleViewRideStatus() {
                            try {
                                String response = inFromServer.readUTF();
                                System.out.println(response);
                            } catch (IOException e) {
                                System.out.println("Error viewing ride status: " + e.getMessage());
                            }
                        }

                        private static void handleAcceptBid() {
                            try {
                                if (receivedBids.isEmpty()) {
                                    System.out.println("No bids have been received yet.");
                                    return;
                                }

                                System.out.println("\nReceived bids:");
                                for (int i = 0; i < receivedBids.size(); i++) {
                                    String[] bid = receivedBids.get(i);
                                    System.out.println((i + 1) + ". Driver: " + bid[0] +
                                            ", Fare: $" + bid[1] +
                                            ", Ride ID: " + bid[2]);
                                }

                                String driverUsername = getNonEmptyInput("Enter the driver's username to accept their bid: ");
                                outToServer.writeUTF(driverUsername);

                                String response = inFromServer.readUTF();
                                System.out.println(response);

                                if (response.startsWith("SUCCESS")) {
                                    receivedBids.clear();
                                }
                            } catch (IOException e) {
                                System.out.println("Error accepting bid: " + e.getMessage());
                            }
                        }

                        private static void handleOfferFare() {
                            try {
                                System.out.println("Checking your availability status...");

                                String availabilityResponse = inFromServer.readUTF();
                                System.out.println(availabilityResponse);

                                if (availabilityResponse.startsWith("FAILURE:")) {
                                    return;
                                }

                                String rideRequestsMessage = inFromServer.readUTF();

                                if (rideRequestsMessage.startsWith("INFO: No ride requests")) {
                                    System.out.println(rideRequestsMessage);
                                    return;
                                }

                                if (rideRequestsMessage.startsWith("RIDE_REQUESTS:")) {
                                    String[] rides = rideRequestsMessage.substring("RIDE_REQUESTS:".length()).split("\\|");
                                    System.out.println("\nAvailable ride requests:");
                                    for (int i = 0; i < rides.length; i++) {
                                        System.out.println((i + 1) + ". " + rides[i]);
                                    }

                                    String choice = getValidNumber("Enter the number of the ride you want to bid on (or 0 to cancel): ");
                                    outToServer.writeUTF(choice);

                                    if (choice.equals("0")) {
                                        String cancelResponse = inFromServer.readUTF();
                                        System.out.println(cancelResponse);
                                        return;
                                    }

                                    String selectedRideResponse = inFromServer.readUTF();
                                    System.out.println(selectedRideResponse);

                                    if (selectedRideResponse.startsWith("FAILURE:")) {
                                        return;
                                    }

                                    if (selectedRideResponse.startsWith("Selected ride:")) {
                                        String fare = getValidNumber("Enter your fare offer (in dollars): ");
                                        outToServer.writeUTF(fare);

                                        String bidConfirmation = inFromServer.readUTF();
                                        System.out.println(bidConfirmation);
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Error processing ride bids: " + e.getMessage());
                            }
                        }

                        private static void handleUpdateRideStatus() {
                            try {
                                String statusMessage = inFromServer.readUTF();

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

                                    String response = inFromServer.readUTF();
                                    System.out.println(response);
                                }
                            } catch (IOException e) {
                                System.out.println("Error updating ride status: " + e.getMessage());
                            }
                        }

                        private static void cleanup() {
                            running = false;
                            try {
                                if (userInput != null) userInput.close();
                                if (outToServer != null) outToServer.close();
                                if (inFromServer != null) inFromServer.close();
                                if (client != null) client.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        static class ServerListener implements Runnable {
                            @Override
                            public void run() {
                                try {
                                    while (running) {
                                        String message = inFromServer.readUTF();

                                        if (message.startsWith("BID_NOTIFICATION:")) {
                                            String[] parts = message.substring("BID_NOTIFICATION:".length()).split(":");
                                            if (parts.length == 3) {
                                                String driverUsername = parts[0];
                                                String fareOffer = parts[1];
                                                String rideId = parts[2];
                                                receivedBids.add(new String[]{driverUsername, fareOffer, rideId});
                                                System.out.println("\n[NEW BID] Driver " + driverUsername +
                                                        " offered $" + fareOffer + " for ride " + rideId);
                                                System.out.print("Please enter your choice: ");
                                            }
                                        } else if (message.startsWith("SUCCESS:") || message.startsWith("INFO:")) {
                                            System.out.println("\n[SERVER] " + message);
                                            System.out.print("Please enter your choice: ");
                                        }
                                    }
                                } catch (IOException e) {
                                    if (running) {
                                        System.out.println("Connection to server lost: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }