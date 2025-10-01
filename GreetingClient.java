// File Name: GreetingClient.java
import java.io.*;
import java.net.Socket;

/**
 * Client application that connects to a server and handles
 * customer/driver signup and login functionality.
 */
public class GreetingClient {

    /**
     * Main method to run the client application.
     *
     * @param args Command line arguments: [serverName, port]
     */
    public static void main(String[] args) {
        // Validate command line arguments
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
            Socket client = new Socket(serverName, port);
            System.out.println("Just connected to " + client.getRemoteSocketAddress());

            // Display main menu and get user choice
            System.out.println("Please choose whether you are a Customer or a Driver by either entering 1 or 2");
            System.out.println("1. Customer");
            System.out.println("2. Driver");

            BufferedReader mainBuffReader = new BufferedReader(new InputStreamReader(System.in));
            int mainChoice = Integer.parseInt(mainBuffReader.readLine());

            // Send main choice to server
            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
            outToServer.writeUTF(String.valueOf(mainChoice));

            if (mainChoice == 1) {
                if(handleCustomer(client))
                {
                    System.out.println("Welcome to the Customer Menu!");
                    System.out.println("1. Request Ride");
                    System.out.println("2. View ride status");
                    System.out.println("3. Disconnect");
                    System.out.println("Please enter your choice:");
                    int menuChoice = Integer.parseInt(mainBuffReader.readLine());
                    outToServer.writeUTF(String.valueOf(menuChoice));
                    CustomerMenu(client, menuChoice);
                }
            } else if (mainChoice == 2) {
                if(handleDriver(client))
                {
                    System.out.println("Welcome to the Driver Menu!");
                    System.out.println("1. Offer fare for ride requests");
                    System.out.println("2. Update ride status");
                    System.out.println("3. Disconnect");
                    System.out.println("Please enter your choice:");
                    int menuChoice = Integer.parseInt(mainBuffReader.readLine());
                    outToServer.writeUTF(String.valueOf(menuChoice));
                    DriverMenu(client, menuChoice);
                }
            } else {
                System.out.println("Invalid choice. Exiting.");
                client.close();
                return;
            }

            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle Customer signup or login.
     *
     * @param client Socket connection to server
     * @return
     * @throws IOException If there's an error with I/O operations
     */
    public static boolean handleCustomer(Socket client) throws IOException {
        boolean isCustomer = true;
        System.out.println("You chose Customer");
        System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
        System.out.println("1. Sign up");
        System.out.println("2. Log in");

        BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
        int choice = Integer.parseInt(buffReader.readLine());
        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        outToServer.writeUTF(String.valueOf(choice));

        if (choice == 1) {
            handleSignup(client, buffReader, isCustomer);
        } else if (choice == 2) {
            handleLogin(client, buffReader, isCustomer);
        } else {
            System.out.println("Invalid choice. Exiting.");
            client.close();
            return false;
        }
        return true;
    }

    /**
     * Handle Driver signup or login.
     *
     * @param client Socket connection to server
     * @throws IOException If there's an error with I/O operations
     */
    private static boolean handleDriver(Socket client) throws IOException {
        System.out.println("You chose Driver");
        System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
        System.out.println("1. Sign up");
        System.out.println("2. Log in");

        BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
        int choice = Integer.parseInt(buffReader.readLine());
        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        outToServer.writeUTF(String.valueOf(choice));

        boolean isCustomer = false;
        if (choice == 1) {
            handleSignup(client, buffReader, isCustomer);
        } else if (choice == 2) {
            handleLogin(client, buffReader, isCustomer);
        } else {
            System.out.println("Invalid choice. Exiting.");
            client.close();
            return false;
        }
        return true;
    }

    /**
     * Handle signup process.
     *
     * @param client     Socket connection to server
     * @param buffReader BufferedReader for reading user input
     * @param isCustomer
     * @throws IOException If there's an error with I/O operations
     */
    public static boolean handleSignup(Socket client, BufferedReader buffReader, boolean isCustomer) throws IOException {
        System.out.println("You chose to Sign up");

        System.out.println("Please enter your email:");
        String email = buffReader.readLine();

        System.out.println("Please enter your username:");
        String username = buffReader.readLine();

        System.out.println("Please enter your password:");
        String password = buffReader.readLine();

        // Send signup data to server
        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        outToServer.writeUTF(email);
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        // Read response from server
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);
        System.out.println("To Proceed, please log in with your username and password");

        if (handleLogin(client, buffReader, isCustomer)) {
            return true;
        }
        return false;
    }

    /**
     * Handle login process.
     *
     * @param client     Socket connection to server
     * @param buffReader BufferedReader for reading user input
     * @param isCustomer
     * @throws IOException If there's an error with I/O operations
     */
    public static boolean handleLogin(Socket client, BufferedReader buffReader, boolean isCustomer) throws IOException {
        System.out.println("You chose to Log in");

        System.out.println("Please enter your username:");
        String username = buffReader.readLine();

        System.out.println("Please enter your password:");
        String password = buffReader.readLine();

        // Send login data to server
        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        // Read response from server
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);

        if (isCustomer && serverResponse.equals("SUCCESS: Customer login successful")) {
            System.out.println("Welcome customer, " + username + "!");
            return true;
        }
        else if (!isCustomer && serverResponse.equals("SUCCESS: Driver login successful")) {
            System.out.println("Welcome driver, " + username + "!");
            return true;
        }
        else {
            System.out.println("Login failed. Please try again.");
            return false;
        }
    }

    private static void handleRideRequest(Socket client) {
        try {
            System.out.println("Please enter your pickup location:");
            BufferedReader rideReader = new BufferedReader(new InputStreamReader(System.in));
            String pickupLocation = rideReader.readLine();

            System.out.println("Please enter your drop-off location:");
            String dropoffLocation = rideReader.readLine();

            System.out.println("Please enter your suggested fare (in dollars):");
            String customerFare = rideReader.readLine();

            System.out.println("Ride requested from " + pickupLocation + " to " + dropoffLocation);
            System.out.println("Your suggested fare: $" + customerFare);

            // Send data to server
            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
            outToServer.writeUTF(pickupLocation);
            outToServer.writeUTF(dropoffLocation);
            outToServer.writeUTF(customerFare);

            System.out.println("Ride request sent to server.");

            // Read response from server
            DataInputStream inFromServer = new DataInputStream(client.getInputStream());
            String serverResponse = inFromServer.readUTF();
            System.out.println("Server response: " + serverResponse);

            if (serverResponse.startsWith("SUCCESS")) {
                System.out.println("Your ride request has been posted. Waiting for driver bids...");

                // Wait for bids
                String bidsResponse = inFromServer.readUTF();

                if (bidsResponse.equals("NO_BIDS")) {
                    System.out.println("No bids received yet. Please check status later.");
                    return;
                }

                if (bidsResponse.startsWith("SUCCESS")) {
                    System.out.println("You've received bids from drivers!");

                    // Get the list of bids
                    String bidsList = inFromServer.readUTF();
                    String[] bids = bidsList.split(";");

                    System.out.println("\nAvailable driver bids:");
                    for (int i = 0; i < bids.length; i++) {
                        String[] bidDetails = bids[i].split(",");
                        System.out.println((i+1) + ". Driver: " + bidDetails[0] + ", Fare: $" + bidDetails[1]);
                    }

                    System.out.println("\nSelect a bid to accept (1-" + bids.length + ") or 0 to reject all:");
                    int bidChoice = Integer.parseInt(rideReader.readLine());

                    outToServer.writeInt(bidChoice);

                    String matchResult = inFromServer.readUTF();
                    if (matchResult.startsWith("SUCCESS")) {
                        String[] matchDetails = matchResult.substring(8).split(",");
                        System.out.println("Ride confirmed with driver: " + matchDetails[0]);
                        System.out.println("Agreed fare: $" + matchDetails[1]);
                        System.out.println("Your driver will pick you up shortly. Enjoy your ride!");
                    } else {
                        System.out.println(matchResult);
                    }
                }
            } else {
                System.out.println("Failed to post ride request: " + serverResponse);
            }
        } catch (IOException e) {
            System.out.println("Error processing ride request: " + e.getMessage());
        }
    }

    private static void viewRideStatus(Socket client) {
        try {
            DataInputStream inFromServer = new DataInputStream(client.getInputStream());
            String statusResponse = inFromServer.readUTF();

            System.out.println(statusResponse);

            if (statusResponse.startsWith("FAILURE")) {
                System.out.println("No active rides found.");
            }
        } catch (IOException e) {
            System.out.println("Error checking ride status: " + e.getMessage());
        }
    }

    private static void CustomerMenu(Socket client, int menuChoice) {
        switch (menuChoice) {
            case 1:
                handleRideRequest(client);
                break;
            case 2:
                viewRideStatus(client);
                break;
            case 3:
                System.out.println("Disconnecting from server...");
                break;
            default:
                System.out.println("Invalid option selected.");
        }
    }

    private static void DriverMenu(Socket client, int menuChoice) throws IOException {
        switch (menuChoice) {
            case 1:
                OfferFare(client);
                break;
            case 2:
                updateRideStatus(client);
                break;
            case 3:
                System.out.println("Disconnecting from server...");
                break;
            default:
                System.out.println("Invalid option selected.");
        }
    }

    private static void OfferFare(Socket client) throws IOException {
        System.out.println("Checking your availability status...");
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        String availabilityResponse = inFromServer.readUTF();

        if (availabilityResponse.equals("available")) {
            System.out.println("You are available to offer fares for ride requests.");
            System.out.println("Checking for ride requests...");

            String rideRequests = inFromServer.readUTF();

            if (rideRequests.equals("NO_REQUESTS")) {
                System.out.println("No ride requests available at the moment.");
                return;
            }

            // Parse and display ride requests
            System.out.println("Available ride requests:");
            String[] requests = rideRequests.split(";");
            for (int i = 0; i < requests.length; i++) {
                String[] requestInfo = requests[i].split(",");
                System.out.println((i+1) + ". Customer: " + requestInfo[0] +
                                   ", From: " + requestInfo[1] +
                                   ", To: " + requestInfo[2] +
                                   ", Customer's suggested fare: $" + requestInfo[3]);
            }

            // Ask driver to select a ride to bid on
            System.out.println("Enter the number of the ride you want to bid on (or 0 to cancel):");
            int rideChoice = Integer.parseInt(input.readLine());

            outToServer.writeInt(rideChoice);

            if (rideChoice == 0) {
                System.out.println("Bid cancelled.");
                return;
            }

            // Get ride details and ask for bid
            String selectedRideDetails = inFromServer.readUTF();
            System.out.println("Selected ride: " + selectedRideDetails);

            System.out.println("Enter your fare offer (must be a number in dollars):");
            String fareOffer = input.readLine();
            outToServer.writeUTF(fareOffer);

            // Wait for server response
            String bidResponse = inFromServer.readUTF();
            System.out.println(bidResponse);

            if (bidResponse.startsWith("SUCCESS")) {
                System.out.println("Your bid has been submitted. Waiting for customer's decision...");
                String customerDecision = inFromServer.readUTF();

                if (customerDecision.startsWith("ACCEPTED")) {
                    String[] rideInfo = customerDecision.substring(9).split(",");
                    System.out.println("\nGreat news! The customer has accepted your bid.");
                    System.out.println("Ride details:");
                    System.out.println("Customer: " + rideInfo[0]);
                    System.out.println("Pickup: " + rideInfo[1]);
                    System.out.println("Dropoff: " + rideInfo[2]);
                    System.out.println("Agreed fare: $" + rideInfo[3]);
                    System.out.println("Your status has been updated to 'busy'");
                    System.out.println("Please proceed to pick up the customer.");
                } else {
                    System.out.println(customerDecision);
                }
            }
        } else {
            System.out.println("You are not available to offer fares at the moment.");
            System.out.println("Current status: " + availabilityResponse);
        }
    }

    private static void updateRideStatus(Socket client) {
        try {
            DataInputStream inFromServer = new DataInputStream(client.getInputStream());
            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            String statusResponse = inFromServer.readUTF();
            System.out.println(statusResponse);

            if (statusResponse.startsWith("FAILURE")) {
                System.out.println("No active assignments found.");
                return;
            }

            String optionsResponse = inFromServer.readUTF();
            System.out.println(optionsResponse);

            int choice = Integer.parseInt(input.readLine());
            outToServer.writeInt(choice);

            String updateResponse = inFromServer.readUTF();
            System.out.println(updateResponse);

        } catch (IOException e) {
            System.out.println("Error updating ride status: " + e.getMessage());
        }
    }
}