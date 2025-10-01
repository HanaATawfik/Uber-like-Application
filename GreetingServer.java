// File Name: GreetingServer.java
                import java.io.*;
                import java.net.*;
                import java.util.*;
                import java.util.concurrent.ConcurrentHashMap;

                /**
                 * Server application that handles client connections
                 * and processes customer/driver signup and login requests.
                 */
                public class GreetingServer extends Thread {
                    private ServerSocket serverSocket = null;
                    private static final int TIMEOUT = 1000000;

                    // Class names should start with uppercase letter
                    static class Customer {
                        private final String username;
                        private final String email;
                        private final String password;
                        public static ConcurrentHashMap<String, Customer> ccredentials = new ConcurrentHashMap<String, Customer>();

                        public Customer(String email, String username, String password) {
                            this.username = username;
                            this.email = email;
                            this.password = password;
                        }

                        public String getusername() {
                            return username;
                        }
                        public String getemail() {
                            return email;
                        }
                        public String getpassword() {
                            return password;
                        }
                        public void putccredentials(Customer customer) {
                            ccredentials.put(getusername(), customer);
                        }
                    }

                    static class Driver {
                        private final String username;
                        private final String email;
                        private final String password;
                        private String status = "available";
                        public static ConcurrentHashMap<String, Driver> dcredentials = new ConcurrentHashMap<String, Driver>();

                        public Driver(String email, String username, String password, String status) {
                            this.username = username;
                            this.email = email;
                            this.password = password;
                            this.status = status;
                        }

                        public String getusername() {
                            return username;
                        }
                        public String getemail() {
                            return email;
                        }
                        public String getpassword() {
                            return password;
                        }
                        public String getstatus() {
                            return status;
                        }
                        public void setstatus(String status) {
                            this.status = status;
                        }
                        public void putdcredentials(Driver driver) {
                            dcredentials.put(getusername(), driver);
                        }
                    }

                    static class Bid {
                        private final String driverUsername;
                        private final String fare;
                        private final long timestamp;

                        public Bid(String driverUsername, String fare) {
                            this.driverUsername = driverUsername;
                            this.fare = fare;
                            this.timestamp = System.currentTimeMillis();
                        }

                        public String getDriverUsername() {
                            return driverUsername;
                        }

                        public String getFare() {
                            return fare;
                        }

                        public long getTimestamp() {
                            return timestamp;
                        }
                    }

                    static class Ride {
                        private final String pickupLocation;
                        private final String dropLocation;
                        private final String customerUsername;
                        private String driverUsername;
                        private String agreedFare = null;
                        private String customerFare = null;
                        private String status = "pending"; // pending, matched, started, completed
                        private final String rideId;
                        private final List<Bid> bids = new ArrayList<>();

                        // Store pending ride requests (not yet assigned to a driver)
                        public static ConcurrentHashMap<String, Ride> pendingRides = new ConcurrentHashMap<>();
                        // Store all rides (including assigned ones)
                        public static ConcurrentHashMap<String, Ride> allRides = new ConcurrentHashMap<>();
                        // Counter for generating ride IDs
                        private static int rideCounter = 1;

                        public Ride(String pickupLocation, String dropLocation, String customerUsername, String customerFare) {
                            this.pickupLocation = pickupLocation;
                            this.dropLocation = dropLocation;
                            this.customerUsername = customerUsername;
                            this.customerFare = customerFare;
                            this.rideId = "RIDE" + (rideCounter++);
                        }

                        public String getRideId() {
                            return rideId;
                        }

                        public String getPickupLocation() {
                            return pickupLocation;
                        }

                        public String getDropLocation() {
                            return dropLocation;
                        }

                        public String getCustomerUsername() {
                            return customerUsername;
                        }

                        public String getDriverUsername() {
                            return driverUsername;
                        }

                        public void setDriverUsername(String driverUsername) {
                            this.driverUsername = driverUsername;
                        }

                        public String getCustomerFare() {
                            return customerFare;
                        }

                        public String getAgreedFare() {
                            return agreedFare;
                        }

                        public void setAgreedFare(String agreedFare) {
                            this.agreedFare = agreedFare;
                        }

                        public String getStatus() {
                            return status;
                        }

                        public void setStatus(String status) {
                            this.status = status;
                        }

                        public void addBid(String driverUsername, String fare) {
                            bids.add(new Bid(driverUsername, fare));
                        }

                        public List<Bid> getBids() {
                            return bids;
                        }

                        public void addToPendingRides() {
                            pendingRides.put(this.rideId, this);
                            allRides.put(this.rideId, this);
                        }

                        public void removeFromPendingRides() {
                            pendingRides.remove(this.rideId);
                        }

                        public static Ride getRideByCustomer(String customerUsername) {
                            for (Ride ride : allRides.values()) {
                                if (ride.getCustomerUsername().equals(customerUsername) &&
                                   !ride.getStatus().equals("completed")) {
                                    return ride;
                                }
                            }
                            return null;
                        }

                        public static Ride getRideByDriver(String driverUsername) {
                            for (Ride ride : allRides.values()) {
                                if (driverUsername.equals(ride.getDriverUsername()) &&
                                   !ride.getStatus().equals("completed")) {
                                    return ride;
                                }
                            }
                            return null;
                        }
                    }

                    /**
                     * Constructor to initialize server on specified port.
                     *
                     * @param port The port number to listen on
                     * @throws IOException If server socket cannot be created
                     */
                    public GreetingServer(int port) throws IOException {
                        serverSocket = new ServerSocket(port);
                        serverSocket.setSoTimeout(TIMEOUT);
                        serverSocket.setReuseAddress(true);
                    }

                    @Override
                    public void run() {
                        while (true) {
                            try {
                                System.out.println("Waiting for client on port " +
                                        serverSocket.getLocalPort() + "...");

                                Socket client = serverSocket.accept();
                                System.out.println("Just connected to " + client.getRemoteSocketAddress());

                                // Create a new thread object to handle this client
                                ClientHandler clientHandler = new ClientHandler(client);

                                // Start a new thread for this client
                                new Thread(clientHandler).start();

                                // Don't close serverSocket here - it would stop the server
                            } catch (SocketTimeoutException s) {
                                System.out.println("Socket timed out!");
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }

                    // ClientHandler class to handle each client in a separate thread
                    private static class ClientHandler implements Runnable {
                        private final Socket clientSocket;
                        private Customer c = new Customer(null, null, null);
                        private Driver d = new Driver(null, null, null, null);

                        // Constructor
                        public ClientHandler(Socket socket) {
                            this.clientSocket = socket;
                        }

                        public void run() {
                            DataInputStream in = null;
                            DataOutputStream out = null;

                            try {
                                // Get input and output streams
                                in = new DataInputStream(clientSocket.getInputStream());
                                out = new DataOutputStream(clientSocket.getOutputStream());

                                // Process client request
                                int mainChoice = Integer.parseInt(in.readUTF());

                                if (mainChoice == 1) {
                                    if (handleCustomerRequest(in, out)) {
                                        int menuChoice = Integer.parseInt(in.readUTF());
                                        processCustomerMenuSelection(in, out, menuChoice);
                                    }
                                }
                                else if (mainChoice == 2) {
                                    if(handleDriverRequest(in, out)) {
                                        int menuChoice = Integer.parseInt(in.readUTF());
                                        processDriverMenuSelection(in, out, menuChoice);
                                    }
                                }
                                else {
                                    System.out.println("Invalid choice received from client.");
                                    out.writeUTF("Invalid choice");
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (in != null) {
                                        in.close();
                                    }
                                    if (out != null) {
                                        out.close();
                                    }
                                    if (clientSocket != null) {
                                        clientSocket.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        /**
                         * Handle customer signup or login requests.
                         */
                        private boolean handleCustomerRequest(DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Client chose Customer");
                            int d = 1;
                            int choice = Integer.parseInt(in.readUTF());
                            if (choice == 1) {
                                if (processSignup(d, in, out)) {
                                    return true;
                                }
                            }
                            else if (choice == 2) {
                                if (processLogin(d, in, out)) {
                                    return true;
                                }
                            }
                            else {
                                System.out.println("Invalid choice received from client.");
                                out.writeUTF("Invalid choice");
                                return false;
                            }
                            return false;
                        }

                        /**
                         * Handle driver signup or login requests.
                         */
                        private boolean handleDriverRequest(DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Client chose Driver");
                            int d = 2;
                            int choice = Integer.parseInt(in.readUTF());

                            if (choice == 1) {
                                if(processSignup(d, in, out)) {
                                    return true;
                                }
                            }
                            else if (choice == 2) {
                                if (processLogin(d, in, out)) {
                                    return true;
                                }
                            }
                            else {
                                System.out.println("Invalid choice received from client.");
                                out.writeUTF("Invalid choice");
                                return false;
                            }
                            return false;
                        }

                        /**
                         * Process user signup data.
                         */
                        private boolean processSignup(int d, DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Client chose to Sign up");

                            if (d == 1) {
                                System.out.println("Customer");
                                String email = in.readUTF();
                                String username = in.readUTF();
                                String password = in.readUTF();

                                if (Customer.ccredentials.containsKey(username)) {
                                    System.out.println("Username already exists");
                                    out.writeUTF("FAILURE: Username already exists");
                                    return false;
                                } else {
                                    c = new Customer(email, username, password);
                                    c.putccredentials(c);
                                    System.out.println("Received Sign up data: Email: " + c.getemail() +
                                            ", Username: " + c.getusername() + ", Password: " + c.getpassword());
                                    out.writeUTF("SUCCESS: Customer signup successful");
                                    processLogin(d, in, out);
                                    return true;
                                }
                            } else {
                                System.out.println("Driver");
                                String email = in.readUTF();
                                String username = in.readUTF();
                                String password = in.readUTF();

                                if (Driver.dcredentials.containsKey(username)) {
                                    System.out.println("Username already exists");
                                    out.writeUTF("FAILURE: Username already exists");
                                    return false;
                                } else {
                                    String status = "available";
                                    // Fix: Use a different variable name to avoid conflict with parameter 'd'
                                    Driver driverObj = new Driver(email, username, password, status);
                                    this.d = driverObj;
                                    this.d.putdcredentials(this.d);
                                    System.out.println("Received Sign up data: Email: " + this.d.getemail() +
                                            ", Username: " + this.d.getusername() + ", Password: " + this.d.getpassword()+", Status: " + this.d.getstatus());
                                    out.writeUTF("SUCCESS: Driver signup successful");
                                    processLogin(2, in, out);
                                    return true;
                                }
                            }
                        }

                        /**
                         * Process user login data.
                         */
                        private boolean processLogin(int d, DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Client chose to Log in");
                            String username = in.readUTF();
                            String password = in.readUTF();
                            System.out.println("Received Log in data: Username: " + username +
                                    ", Password: " + password);

                            if (d == 1) {
                                System.out.println("Customer");
                                if (Customer.ccredentials.containsKey(username) &&
                                        Customer.ccredentials.get(username).getpassword().equals(password)) {
                                    System.out.println("Login successful customer!");
                                    // attach logged-in customer to this session for later ride requests
                                    this.c = Customer.ccredentials.get(username);
                                    out.writeUTF("SUCCESS: Customer login successful");
                                    return true;
                                } else {
                                    System.out.println("Invalid username or password.");
                                    out.writeUTF("FAILURE: Invalid username or password");
                                    return false;
                                }
                            } else {
                                System.out.println("Driver");
                                if (Driver.dcredentials.containsKey(username) &&
                                        Driver.dcredentials.get(username).getpassword().equals(password)) {
                                    System.out.println("Login successful driver!");
                                    // attach logged-in driver to this session
                                    this.d = Driver.dcredentials.get(username);
                                    out.writeUTF("SUCCESS: Driver login successful");
                                    return true;
                                } else {
                                    System.out.println("Invalid username or password.");
                                    out.writeUTF("FAILURE: Invalid username or password");
                                    return false;
                                }
                            }
                        }

                        private void processCustomerMenuSelection(DataInputStream in, DataOutputStream out, int menuChoice) throws IOException {
                            System.out.println("Processing customer menu selection: " + menuChoice);

                            switch(menuChoice) {
                                case 1:
                                    processRideRequest(in, out);
                                    break;
                                case 2:
                                    viewCurrentRide(in, out);
                                    break;
                                case 3:
                                    viewBidsForRide(in, out);
                                    break;
                                case 4:
                                    // Future implementation for viewing ride history
                                    out.writeUTF("Feature not implemented yet");
                                    break;
                                default:
                                    System.out.println("Invalid menu choice received from client.");
                                    out.writeUTF("Invalid menu choice");
                            }
                        }

                        private void processRideRequest(DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Client chose to Request a Ride");
                            String pickupLocation = in.readUTF();
                            String dropLocation = in.readUTF();
                            String customerFare = in.readUTF();
                            String customerUsername = c.getusername();

                            // Check if customer already has an active ride
                            Ride existingRide = Ride.getRideByCustomer(customerUsername);
                            if (existingRide != null && !existingRide.getStatus().equals("completed")) {
                                out.writeUTF("FAILURE: You already have an active ride request. Please cancel it first or wait for completion.");
                                return;
                            }

                            // Create new ride and add to pending rides
                            Ride ride = new Ride(pickupLocation, dropLocation, customerUsername, customerFare);
                            ride.addToPendingRides();

                            System.out.println("Received Ride request: ID: " + ride.getRideId() +
                                    ", Pickup: " + ride.getPickupLocation() +
                                    ", Dropoff: " + ride.getDropLocation() +
                                    ", Customer: " + ride.getCustomerUsername() +
                                    ", Suggested fare: " + customerFare);

                            out.writeUTF("SUCCESS: Ride request posted with ID: " + ride.getRideId() +
                                    ". Suggested fare: $" + customerFare +
                                    ". Waiting for driver bids.");
                        }

                        private void viewCurrentRide(DataInputStream in, DataOutputStream out) throws IOException {
                            String customerUsername = c.getusername();
                            Ride ride = Ride.getRideByCustomer(customerUsername);

                            if (ride == null) {
                                out.writeUTF("FAILURE: You don't have any active rides.");
                                return;
                            }

                            StringBuilder details = new StringBuilder("SUCCESS: ");
                            details.append("Ride ID: ").append(ride.getRideId())
                                  .append(", Status: ").append(ride.getStatus())
                                  .append(", From: ").append(ride.getPickupLocation())
                                  .append(", To: ").append(ride.getDropLocation())
                                  .append(", Suggested fare: $").append(ride.getCustomerFare());

                            if (ride.getDriverUsername() != null) {
                                details.append(", Driver: ").append(ride.getDriverUsername());
                                details.append(", Agreed fare: $").append(ride.getAgreedFare());
                            }

                            out.writeUTF(details.toString());
                        }

                        private void viewBidsForRide(DataInputStream in, DataOutputStream out) throws IOException {
                            String customerUsername = c.getusername();
                            Ride ride = Ride.getRideByCustomer(customerUsername);

                            if (ride == null) {
                                out.writeUTF("FAILURE: You don't have any active ride requests.");
                                return;
                            }

                            List<Bid> bids = ride.getBids();
                            if (bids.isEmpty()) {
                                out.writeUTF("NO_BIDS: No bids received yet for your ride request.");
                                return;
                            }

                            // Format all bids for sending to client
                            StringBuilder bidInfo = new StringBuilder();
                            for (int i = 0; i < bids.size(); i++) {
                                Bid bid = bids.get(i);
                                bidInfo.append(bid.getDriverUsername()).append(",")
                                       .append(bid.getFare());
                                if (i < bids.size() - 1) {
                                    bidInfo.append(";");
                                }
                            }

                            out.writeUTF("BIDS:" + bidInfo.toString());

                            // Wait for customer's choice
                            int bidChoice = in.readInt();
                            if (bidChoice == 0) {
                                out.writeUTF("CANCELLED: You've rejected all bids.");
                                return;
                            }

                            // Process customer's choice of bid
                            if (bidChoice > 0 && bidChoice <= bids.size()) {
                                Bid selectedBid = bids.get(bidChoice - 1);
                                String driverUsername = selectedBid.getDriverUsername();
                                String agreedFare = selectedBid.getFare();

                                // Update ride status
                                ride.setDriverUsername(driverUsername);
                                ride.setAgreedFare(agreedFare);
                                ride.setStatus("matched");
                                ride.removeFromPendingRides();

                                // Update driver status
                                Driver matchedDriver = Driver.dcredentials.get(driverUsername);
                                if (matchedDriver != null) {
                                    matchedDriver.setstatus("busy");
                                    Driver.dcredentials.put(driverUsername, matchedDriver);
                                }

                                // Send confirmation to customer
                                out.writeUTF("SUCCESS:" + driverUsername + "," + agreedFare);

                                System.out.println("Match made! Customer " + customerUsername +
                                                   " accepted bid from driver " + driverUsername +
                                                   " for fare $" + agreedFare);
                            } else {
                                out.writeUTF("FAILURE: Invalid bid selection.");
                            }
                        }

                        private void processDriverMenuSelection(DataInputStream in, DataOutputStream out, int menuChoice) throws IOException {
                            System.out.println("Processing driver menu selection: " + menuChoice);

                            switch(menuChoice) {
                                case 1:
                                    offerRideFare(in, out);
                                    break;
                                case 2:
                                    viewCurrentAssignment(in, out);
                                    break;
                                case 3:
                                    updateRideStatus(in, out);
                                    break;
                                case 4:
                                    // Future implementation for viewing ride history
                                    out.writeUTF("Feature not implemented yet");
                                    break;
                                default:
                                    System.out.println("Invalid menu choice received from client.");
                                    out.writeUTF("Invalid menu choice");
                            }
                        }

                        private void offerRideFare(DataInputStream in, DataOutputStream out) throws IOException {
                            System.out.println("Driver chose to Offer Ride Fare");
                            String driverUsername = d.getusername();

                            // Check if driver is available
                            if (!d.getstatus().equals("available")) {
                                out.writeUTF("not available");
                                return;
                            }

                            out.writeUTF("available");

                            // Get all pending ride requests
                            if (Ride.pendingRides.isEmpty()) {
                                out.writeUTF("NO_REQUESTS");
                                return;
                            }

                            // Send all pending rides to driver
                            StringBuilder ridesList = new StringBuilder();
                            int counter = 1;
                            List<String> rideIds = new ArrayList<>();

                            for (Ride ride : Ride.pendingRides.values()) {
                                rideIds.add(ride.getRideId());
                                if (counter > 1) ridesList.append(";");
                                ridesList.append(ride.getCustomerUsername()).append(",")
                                        .append(ride.getPickupLocation()).append(",")
                                        .append(ride.getDropLocation()).append(",")
                                        .append(ride.getCustomerFare());
                                counter++;
                            }
                            out.writeUTF(ridesList.toString());

                            // Get driver's choice
                            int rideChoice = in.readInt();

                            if (rideChoice <= 0 || rideChoice > rideIds.size()) {
                                out.writeUTF("FAILURE: Invalid selection");
                                return;
                            }

                            String selectedRideId = rideIds.get(rideChoice - 1);
                            Ride selectedRide = Ride.pendingRides.get(selectedRideId);

                            if (selectedRide == null) {
                                out.writeUTF("FAILURE: Ride no longer available");
                                return;
                            }

                            out.writeUTF("Customer: " + selectedRide.getCustomerUsername() +
                                         ", From: " + selectedRide.getPickupLocation() +
                                         ", To: " + selectedRide.getDropLocation() +
                                         ", Customer's fare: $" + selectedRide.getCustomerFare());

                            // Get driver's fare offer
                            String driverFare = in.readUTF();

                            if (driverFare.isEmpty()) {
                                out.writeUTF("FAILURE: Fare cannot be empty");
                                return;
                            }

                            // Add bid to ride
                            selectedRide.addBid(driverUsername, driverFare);

                            System.out.println("Driver " + driverUsername + " offered fare $" + driverFare +
                                    " for ride " + selectedRideId);

                            out.writeUTF("SUCCESS: Your bid has been submitted successfully. You will be notified if the customer accepts.");
                        }

                        private void viewCurrentAssignment(DataInputStream in, DataOutputStream out) throws IOException {
                            String driverUsername = d.getusername();
                            Ride ride = Ride.getRideByDriver(driverUsername);

                            if (ride == null) {
                                out.writeUTF("FAILURE: You don't have any active assignments.");
                                return;
                            }

                            StringBuilder details = new StringBuilder("SUCCESS: ");
                            details.append("Ride ID: ").append(ride.getRideId())
                                  .append(", Status: ").append(ride.getStatus())
                                  .append(", Customer: ").append(ride.getCustomerUsername())
                                  .append(", From: ").append(ride.getPickupLocation())
                                  .append(", To: ").append(ride.getDropLocation())
                                  .append(", Agreed fare: $").append(ride.getAgreedFare());

                            out.writeUTF(details.toString());
                        }

                        private void updateRideStatus(DataInputStream in, DataOutputStream out) throws IOException {
                            String driverUsername = d.getusername();
                            Ride ride = Ride.getRideByDriver(driverUsername);

                            if (ride == null) {
                                out.writeUTF("FAILURE: You don't have any active assignments.");
                                return;
                            }

                            out.writeUTF("SUCCESS: Current ride status: " + ride.getStatus());
                            out.writeUTF("Options: 1. Started ride  2. Completed ride");

                            int statusChoice = in.readInt();
                            switch (statusChoice) {
                                case 1:
                                    if (!ride.getStatus().equals("matched")) {
                                        out.writeUTF("FAILURE: Can only start rides that are in 'matched' status.");
                                        return;
                                    }
                                    ride.setStatus("started");
                                    out.writeUTF("SUCCESS: Ride status updated to 'started'");
                                    break;

                                case 2:
                                    if (!ride.getStatus().equals("started")) {
                                        out.writeUTF("FAILURE: Can only complete rides that are in 'started' status.");
                                        return;
                                    }
                                    ride.setStatus("completed");
                                    d.setstatus("available");
                                    Driver.dcredentials.put(driverUsername, d);
                                    out.writeUTF("SUCCESS: Ride completed successfully. Your status is now 'available'.");
                                    break;

                                default:
                                    out.writeUTF("FAILURE: Invalid status choice.");
                            }
                        }
                    }

                    public static void main(String[] args) {
                        if (args.length < 1) {
                            System.out.println("Usage: java GreetingServer <port>");
                            return;
                        }

                        try {
                            int port = Integer.parseInt(args[0]);
                            Thread t = new GreetingServer(port);
                            t.start();
                        } catch (NumberFormatException e) {
                            System.out.println("Error: Port must be a valid number");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }