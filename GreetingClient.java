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
                    CustomerMenu(client,menuChoice);
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
                    DriverMenu(client,menuChoice);
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
        // outToServer.writeUTF(String.valueOf(choice));
        outToServer.writeUTF(email);
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        // Read response from server
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);
        System.out.println("To Proceed, please log in with your username and password");
        //   choice=2;
        if( handleLogin(client, buffReader, isCustomer))
        {
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
        //  outToServer.writeUTF(String.valueOf(choice)); ------------------------------------------------------------
        outToServer.writeUTF(username);
        outToServer.writeUTF(password);

        // Read response from server
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        String serverResponse = inFromServer.readUTF();
        System.out.println("Server response: " + serverResponse);
        if(isCustomer && serverResponse.equals("SUCCESS: Customer login successful"))
        {
            System.out.println("Welcome customer, " + username + "!");
            return true;
            //  Menu();
        }
        else if (isCustomer == false && serverResponse.equals("SUCCESS: Driver login successful"))
        {
            System.out.println("Welcome driver, " + username + "!");
            //  Menu();
        }
        else
        {
            System.out.println("Login failed. Please try again.");
            return false;
        }
        return true;
    }

    private static void handleRideRequest(Socket client) {
        System.out.println("Please enter your pickup location:");
        BufferedReader rideReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String pickupLocation = rideReader.readLine();
            System.out.println("Please enter your drop-off location:");
            String dropoffLocation = rideReader.readLine();
            System.out.println("Ride requested from " + pickupLocation + " to " + dropoffLocation);
            String customerFare= rideReader.readLine();
            System.out.println("Customer fare: " + customerFare);
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

            // FIXED: Only read second response if first one indicates success
            if (serverResponse.startsWith("SUCCESS")) {
                //String serverResponse2 = inFromServer.readUTF();
                System.out.println("waiting for offers from drivers: " );
                String serverResponse2 = inFromServer.readUTF();
                System.out.println("Server response: " + serverResponse2);
                if (serverResponse2.startsWith("SUCCESS")) {
                    System.out.println("Ride request processed successfully.");
                    String driverUsername = inFromServer.readUTF();
                    String driverFare = inFromServer.readUTF();
                    System.out.println("Driver accepted ride request.");
                    System.out.println("Driver: " + driverUsername);
                    System.out.println("Fare offered by driver: " + driverFare);
                }
                else {
                    System.out.println("No drivers accepted the ride request.");
                }
            } else {
                // Failure path: do not attempt a second read
                System.out.println("Ride request failed.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Logic for requesting a ride can be added here

    }
    private static void CustomerMenu(Socket client,int menuChoice) {
        //     System.out.println("Welcome to the Customer Menu!");
        //   System.out.println("1. Request Ride");
        //  System.out.println("2. View ride status");
        //  System.out.println("3. Disconnect");
        // System.out.println("Please enter your choice:");
        //  BufferedReader mainBuffReader = new BufferedReader(new InputStreamReader(System.in));
        //  int menuChoice = Integer.parseInt(mainBuffReader.readLine());

        // Send menu choice to server
        //    DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
        //    outToServer.writeUTF(String.valueOf(menuChoice));
        switch (menuChoice) {
            case 1:
                System.out.println("Requesting ride...");
                handleRideRequest(client);
                // Logic for requesting a ride can be added here
                break;
            case 2:
                System.out.println("Viewing ride status...");
                // Logic for viewing ride status can be added here
                break;
            case 3:
                System.out.println("Disconnecting...");
                // Logic for disconnecting can be added here
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                CustomerMenu(client,menuChoice);
                break;
        }
    }
    private static void DriverMenu(Socket client,int menuChoice) throws IOException {
        switch (menuChoice) {
            case 1:
                System.out.println("Viewing ride requests...");
                OfferFare(client);
                break;
            case 2:
                System.out.println("Updating ride status...");
                // Logic for updating ride status can be added here
                break;
            case 3:
                System.out.println("Disconnecting...");
                // Logic for disconnecting can be added here
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                DriverMenu(client,menuChoice);
                break;
        }
    }

    private static void OfferFare(Socket client) throws IOException {
       System.out.println("checking if you are available to offer fare for ride requests...");
        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
        String serverResponse = inFromServer.readUTF();
        if(serverResponse.equals("available")){
            System.out.println("you are available to offer fare for ride requests.");
            System.out.println("checking for ride requests...");
            String rideRequest = inFromServer.readUTF();
            System.out.println("Ride requests received: " +rideRequest);
            System.out.println("Please choose a ride to offer a fare by entering the desired customer name:");
            BufferedReader customerChoice = new BufferedReader(new InputStreamReader(System.in));

            String rideIndex = customerChoice.readLine();

            // Send ride choice to server
            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
            outToServer.writeUTF(String.valueOf(rideIndex));
            System.out.println("ride of this customer sent to server " + rideIndex);

            // Read response from server
            String serverResponse2 = inFromServer.readUTF();
            System.out.println("Server response: " + serverResponse2);

            if (serverResponse2.startsWith("SUCCESS")) {
                System.out.println("You can now offer a fare for the selected ride.");
                System.out.println("Please enter your fare amount:");
                String fareAmount = customerChoice.readLine();
                outToServer.writeUTF(String.valueOf(fareAmount));
                System.out.println("Fare amount sent to server: " + fareAmount);

                String serverResponse3 = inFromServer.readUTF();
                System.out.println("Server response: " + serverResponse3);

                String driverUsername = inFromServer.readUTF();
                String driverFare = inFromServer.readUTF();
                String customerUsername = inFromServer.readUTF();
                String  pickupLocation = inFromServer.readUTF();
                String dropoffLocation = inFromServer.readUTF();

                String decision= inFromServer.readUTF();
                if(decision.startsWith("SUCCESS"))
                {
                    System.out.println("Fare offered successfully.");
                    System.out.println("Fare offered by driver: " + driverUsername);
                    System.out.println("Fare offered by customer: " + customerUsername);
                    System.out.println("Pickup location: " + pickupLocation);
                    System.out.println("Drop-off location: " + dropoffLocation);
                    System.out.println("Fare: " + driverFare);
                    System.out.println("Ride status updated successfully.");
                }
                else {
                    System.out.println("Failed to offer fare. Please try again.");
                }
            } else {
                System.out.println("Failed to select ride. Please try again.");
            }

        }
        else
        {
            System.out.println("You are not available to offer fare for ride requests at the moment.");
            return;
        }

    }

}