// File Name: GreetingServer.java
        import java.io.*;
        import java.net.*;
        import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;
        import java.util.concurrent.atomic.AtomicInteger;

        public class GreetingServer extends Thread {
            private ServerSocket serverSocket = null;
            private static final int TIMEOUT = 1000000;
            private static final AtomicInteger rideIdCounter = new AtomicInteger(1);

            // Active client connections
            private static ConcurrentHashMap<String, ClientHandler> activeCustomers = new ConcurrentHashMap<>();
            private static ConcurrentHashMap<String, ClientHandler> activeDrivers = new ConcurrentHashMap<>();

            static class Customer {
                private final String username;
                private final String email;
                private final String password;
                public static ConcurrentHashMap<String, Customer> ccredentials = new ConcurrentHashMap<>();

                public Customer(String email, String username, String password) {
                    this.username = username;
                    this.email = email;
                    this.password = password;
                }

                public String getUsername() { return username; }
                public String getEmail() { return email; }
                public String getPassword() { return password; }

                public void putCredentials(Customer customer) {
                    ccredentials.put(getUsername(), customer);
                }
            }

            static class Driver {
                private final String username;
                private final String email;
                private final String password;
                private String status = "available";
                public static ConcurrentHashMap<String, Driver> dcredentials = new ConcurrentHashMap<>();

                public Driver(String email, String username, String password, String status) {
                    this.username = username;
                    this.email = email;
                    this.password = password;
                    this.status = status;
                }

                public String getUsername() { return username; }
                public String getEmail() { return email; }
                public String getPassword() { return password; }
                public String getStatus() { return status; }
                public void setStatus(String status) { this.status = status; }

                public void putCredentials(Driver driver) {
                    dcredentials.put(getUsername(), driver);
                }
            }

            static class Ride {
                private final String rideId;
                private final String pickupLocation;
                private final String dropLocation;
                private final String customerUsername;
                private String driverUsername;
                private final String customerFare;
                private String status = "PENDING"; // PENDING, ASSIGNED, IN_PROGRESS, COMPLETED
                // Fixed: Changed from List<Object> to List<Bid>
                private final List<Bid> bids = Collections.synchronizedList(new ArrayList<Bid>());
                public static ConcurrentHashMap<String, Ride> rides = new ConcurrentHashMap<>();

                public Ride(String rideId, String pickupLocation, String dropLocation,
                            String customerUsername, String customerFare) {
                    this.rideId = rideId;
                    this.pickupLocation = pickupLocation;
                    this.dropLocation = dropLocation;
                    this.customerUsername = customerUsername;
                    this.customerFare = customerFare;
                }

                public String getRideId() { return rideId; }
                public String getPickupLocation() { return pickupLocation; }
                public String getDropLocation() { return dropLocation; }
                public String getCustomerUsername() { return customerUsername; }
                public String getDriverUsername() { return driverUsername; }
                public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
                public String getCustomerFare() { return customerFare; }
                public String getStatus() { return status; }
                public void setStatus(String status) { this.status = status; }
                // Fixed: Changed return type from List<Object> to List<Bid>
                public List<Bid> getBids() { return bids; }

                public void addBid(Bid bid) {
                    bids.add(bid);
                }

                public void putRide(Ride ride) {
                    rides.put(ride.getRideId(), ride);
                }
            }

            static class Bid {
                private final String driverUsername;
                private final String fareOffer;
                private final String rideId;

                public Bid(String driverUsername, String fareOffer, String rideId) {
                    this.driverUsername = driverUsername;
                    this.fareOffer = fareOffer;
                    this.rideId = rideId;
                }

                public String getDriverUsername() { return driverUsername; }
                public String getFareOffer() { return fareOffer; }
                public String getRideId() { return rideId; }
            }

            public GreetingServer(int port) throws IOException {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(TIMEOUT);
                serverSocket.setReuseAddress(true);
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
                        Socket client = serverSocket.accept();
                        System.out.println("Just connected to " + client.getRemoteSocketAddress());

                        ClientHandler clientHandler = new ClientHandler(client);
                        new Thread(clientHandler).start();

                    } catch (SocketTimeoutException s) {
                        System.out.println("Socket timed out!");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }

            private static class ClientHandler implements Runnable {
                private final Socket clientSocket;
                private DataInputStream in;
                private DataOutputStream out;
                private Customer customer;
                private Driver driver;
                private boolean isCustomer;
                private String username;
                private volatile boolean running = true;

                public ClientHandler(Socket socket) {
                    this.clientSocket = socket;
                }

                public void sendMessage(String message) {
                    try {
                        if (out != null) {
                            out.writeUTF(message);
                            out.flush();
                        }
                    } catch (IOException e) {
                        System.out.println("Error sending message to " + username + ": " + e.getMessage());
                    }
                }

                public void run() {
                    try {
                        in = new DataInputStream(clientSocket.getInputStream());
                        out = new DataOutputStream(clientSocket.getOutputStream());

                        int mainChoice = Integer.parseInt(in.readUTF());

                        if (mainChoice == 1) {
                            isCustomer = true;
                            if (handleCustomerRequest()) {
                                activeCustomers.put(username, this);
                                customerMainLoop();
                            }
                        } else if (mainChoice == 2) {
                            isCustomer = false;
                            if (handleDriverRequest()) {
                                activeDrivers.put(username, this);
                                driverMainLoop();
                            }
                        } else {
                            System.out.println("Invalid choice received from client.");
                            out.writeUTF("Invalid choice");
                        }

                    } catch (IOException e) {
                        System.out.println("Client disconnected: " + username);
                    } finally {
                        cleanup();
                    }
                }

                private void customerMainLoop() {
                    try {
                        while (running) {
                            int menuChoice = Integer.parseInt(in.readUTF());

                            switch (menuChoice) {
                                case 1:
                                    processRideRequest();
                                    break;
                                case 2:
                                    viewRideStatus();
                                    break;
                                case 3:
                                    acceptDriverBid();
                                    break;
                                case 4:
                                    System.out.println("Customer " + username + " disconnecting...");
                                    running = false;
                                    break;
                                default:
                                    out.writeUTF("Invalid menu choice");
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Customer " + username + " connection error: " + e.getMessage());
                    }
                }

                private void driverMainLoop() {
                    try {
                        while (running) {
                            int menuChoice = Integer.parseInt(in.readUTF());

                            switch (menuChoice) {
                                case 1:
                                    offerRideFare();
                                    break;
                                case 2:
                                    updateRideStatus();
                                    break;
                                case 3:
                                    System.out.println("Driver " + username + " disconnecting...");
                                    running = false;
                                    break;
                                default:
                                    out.writeUTF("Invalid menu choice");
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Driver " + username + " connection error: " + e.getMessage());
                    }
                }

                private boolean handleCustomerRequest() throws IOException {
                    System.out.println("Client chose Customer");
                    int choice = Integer.parseInt(in.readUTF());

                    if (choice == 1) {
                        return processSignup(1);
                    } else if (choice == 2) {
                        return processLogin(1);
                    } else {
                        out.writeUTF("Invalid choice");
                        return false;
                    }
                }

                private boolean handleDriverRequest() throws IOException {
                    System.out.println("Client chose Driver");
                    int choice = Integer.parseInt(in.readUTF());

                    if (choice == 1) {
                        return processSignup(2);
                    } else if (choice == 2) {
                        return processLogin(2);
                    } else {
                        out.writeUTF("Invalid choice");
                        return false;
                    }
                }

                private boolean processSignup(int userType) throws IOException {
                    System.out.println("Client chose to Sign up");
                    String email = in.readUTF();
                    String username = in.readUTF();
                    String password = in.readUTF();

                    if (userType == 1) {
                        if (Customer.ccredentials.containsKey(username)) {
                            out.writeUTF("FAILURE: Username already exists");
                            return false;
                        } else {
                            customer = new Customer(email, username, password);
                            customer.putCredentials(customer);
                            System.out.println("Customer signup: " + username);
                            out.writeUTF("SUCCESS: Customer signup successful");
                            return processLogin(1);
                        }
                    } else {
                        if (Driver.dcredentials.containsKey(username)) {
                            out.writeUTF("FAILURE: Username already exists");
                            return false;
                        } else {
                            driver = new Driver(email, username, password, "available");
                            driver.putCredentials(driver);
                            System.out.println("Driver signup: " + username);
                            out.writeUTF("SUCCESS: Driver signup successful");
                            return processLogin(2);
                        }
                    }
                }

                private boolean processLogin(int userType) throws IOException {
                    System.out.println("Client chose to Log in");
                    String loginUsername = in.readUTF();
                    String password = in.readUTF();

                    if (userType == 1) {
                        if (Customer.ccredentials.containsKey(loginUsername) &&
                                Customer.ccredentials.get(loginUsername).getPassword().equals(password)) {
                            this.customer = Customer.ccredentials.get(loginUsername);
                            this.username = loginUsername;
                            System.out.println("Customer login successful: " + username);
                            out.writeUTF("SUCCESS: Customer login successful");
                            return true;
                        } else {
                            out.writeUTF("FAILURE: Invalid username or password");
                            return false;
                        }
                    } else {
                        if (Driver.dcredentials.containsKey(loginUsername) &&
                                Driver.dcredentials.get(loginUsername).getPassword().equals(password)) {
                            this.driver = Driver.dcredentials.get(loginUsername);
                            this.username = loginUsername;
                            System.out.println("Driver login successful: " + username);
                            out.writeUTF("SUCCESS: Driver login successful");
                            return true;
                        } else {
                            out.writeUTF("FAILURE: Invalid username or password");
                            return false;
                        }
                    }
                }

private void processRideRequest() throws IOException {
                    String pickupLocation = in.readUTF();
                    String dropLocation = in.readUTF();
                    String customerFare = in.readUTF();

                    String rideId = "RIDE" + rideIdCounter.getAndIncrement();
                    Ride ride = new Ride(rideId, pickupLocation, dropLocation, username, customerFare);
                    ride.putRide(ride);

                    System.out.println("Processing customer menu selection: 1");
                    System.out.println("Client chose to Request a Ride");
                    System.out.println("Received Ride request: ID: " + rideId +
                            ", Pickup: " + pickupLocation +
                            ", Dropoff: " + dropLocation +
                            ", Customer: " + username +
                            ", Suggested fare: " + customerFare);

                    out.writeUTF("SUCCESS: Ride request posted with ID: " + rideId +
                            ". Suggested fare: $" + customerFare +
                            ". Waiting for driver bids.");

                    // Create a separate thread to monitor for new bids
                    Thread bidMonitorThread = new Thread(() -> {
                        try {
                            int lastSentBidCount = 0;
                            while (running && ride.getStatus().equals("PENDING")) {
                                // Check if there are new bids
                                int currentBidCount = ride.getBids().size();
                                if (currentBidCount > lastSentBidCount) {
                                    // Get only the new bids
                                    List<Bid> newBids = ride.getBids().subList(lastSentBidCount, currentBidCount);

                                    // Send notifications for each new bid
                                    for (Bid bid : newBids) {
                                        sendMessage("BID_NOTIFICATION:" + bid.getDriverUsername() + ":" +
                                                bid.getFareOffer() + ":" + rideId);
                                    }

                                    // Update the counter
                                    lastSentBidCount = currentBidCount;
                                }

                                // Wait for 1 second before checking again
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            System.out.println("Bid monitoring thread interrupted for ride: " + rideId);
                        }
                    });

                    // Set as daemon thread so it terminates when main thread ends
                    bidMonitorThread.setDaemon(true);
                    bidMonitorThread.start();
                }
                private void viewRideStatus() throws IOException {
                    // Find the customer's active ride
                    Ride activeRide = null;
                    for (Ride ride : Ride.rides.values()) {
                        if (ride.getCustomerUsername().equals(username) &&
                                !ride.getStatus().equals("COMPLETED")) {
                            activeRide = ride;
                            break;
                        }
                    }

                    if (activeRide == null) {
                        out.writeUTF("INFO: You have no active rides.");
                    } else {
                        StringBuilder status = new StringBuilder();
                        status.append("INFO: Ride ").append(activeRide.getRideId())
                                .append(" - Status: ").append(activeRide.getStatus())
                                .append(", From: ").append(activeRide.getPickupLocation())
                                .append(" To: ").append(activeRide.getDropLocation());

                        if (activeRide.getDriverUsername() != null) {
                            status.append(", Driver: ").append(activeRide.getDriverUsername());
                        }

                        status.append(", Bids received: ").append(activeRide.getBids().size());
                        out.writeUTF(status.toString());
                    }
                }

            private void acceptDriverBid() throws IOException {
                    String driverUsername = in.readUTF();

                    // First check if driver exists and is available
                    Driver assignedDriver = Driver.dcredentials.get(driverUsername);
                    if (assignedDriver == null) {
                        out.writeUTF("FAILURE: Driver not found in the system.");
                        return;
                    }

                    if (!assignedDriver.getStatus().equals("available")) {
                        out.writeUTF("FAILURE: Driver is no longer available. They may have accepted another ride.");
                        return;
                    }

                    // Find customer's pending ride
                    Ride pendingRide = null;
                    for (Ride ride : Ride.rides.values()) {
                        if (ride.getCustomerUsername().equals(username) &&
                                ride.getStatus().equals("PENDING")) {
                            pendingRide = ride;
                            break;
                        }
                    }

                    if (pendingRide == null) {
                        out.writeUTF("FAILURE: No pending ride found.");
                        return;
                    }

                    // Find the bid from this driver
                    Bid acceptedBid = null;
                    for (Bid bid : pendingRide.getBids()) {
                        if (bid.getDriverUsername().equals(driverUsername)) {
                            acceptedBid = bid;
                            break;
                        }
                    }

                    if (acceptedBid == null) {
                        out.writeUTF("FAILURE: No bid found from driver " + driverUsername);
                        return;
                    }

                    // Assign the ride
                    pendingRide.setDriverUsername(driverUsername);
                    pendingRide.setStatus("ASSIGNED");
                    assignedDriver.setStatus("busy");

                    // Send confirmation to customer
                    out.writeUTF("SUCCESS: Ride " + pendingRide.getRideId() +
                            " assigned to driver " + driverUsername +
                            " with fare $" + acceptedBid.getFareOffer());

                    // Notify the driver
                    ClientHandler driverHandler = activeDrivers.get(driverUsername);
                    if (driverHandler != null) {
                        driverHandler.sendMessage("SUCCESS: Customer " + username +
                                " accepted your bid of $" + acceptedBid.getFareOffer() +
                                " for ride " + pendingRide.getRideId() +
                                " from " + pendingRide.getPickupLocation() +
                                " to " + pendingRide.getDropLocation());
                    }

                    // Notify other drivers who bid on this ride that it's been assigned
                    for (Bid bid : pendingRide.getBids()) {
                        String otherDriverUsername = bid.getDriverUsername();
                        if (!otherDriverUsername.equals(driverUsername)) {
                            ClientHandler otherDriverHandler = activeDrivers.get(otherDriverUsername);
                            if (otherDriverHandler != null) {
                                otherDriverHandler.sendMessage("INFO: Ride " + pendingRide.getRideId() +
                                        " has been assigned to another driver.");
                            }
                        }
                    }

                    System.out.println("Ride " + pendingRide.getRideId() + " assigned to driver " + driverUsername);
                }
                private void offerRideFare() throws IOException {
                    System.out.println("Processing driver menu selection: 1");
                    System.out.println("Driver chose to Offer Ride Fare");

                    Driver currentDriver = Driver.dcredentials.get(username);
                    if (currentDriver == null || !currentDriver.getStatus().equals("available")) { //SOMETHING WRONG HAPPENING HERE //I GET NO RESPONSE BACK FROM SERVER
                        out.writeUTF("FAILURE: You are not available to bid on rides.");
                        return;
                    }

                    // Get all pending rides
                    List<Ride> pendingRides = new ArrayList<>();
                    for (Ride ride : Ride.rides.values()) {
                        if (ride.getStatus().equals("PENDING")) {
                            pendingRides.add(ride);  //NEED THIS TO BE REGULARLY UPDATED AND SHOWED
                        }
                    }

                    if (pendingRides.isEmpty()) {
                        out.writeUTF("INFO: No ride requests available at this time.");
                        return;
                    }

                    // Send rides to driver
                    StringBuilder ridesInfo = new StringBuilder("RIDE_REQUESTS:");
                    for (int i = 0; i < pendingRides.size(); i++) {
                        Ride ride = pendingRides.get(i);
                        if (i > 0) ridesInfo.append("|");
                        ridesInfo.append("Customer: ").append(ride.getCustomerUsername())
                                .append(", From: ").append(ride.getPickupLocation())
                                .append(", To: ").append(ride.getDropLocation())
                                .append(", Customer's suggested fare: $").append(ride.getCustomerFare())
                                .append(", ID: ").append(ride.getRideId());
                    }
                    out.writeUTF(ridesInfo.toString());

                    // Get driver's choice
                    String choiceStr = in.readUTF();
                    int choice = Integer.parseInt(choiceStr);

                    if (choice == 0) {
                        out.writeUTF("INFO: Bid cancelled.");
                        return;
                    }

                    if (choice < 1 || choice > pendingRides.size()) {
                        out.writeUTF("FAILURE: Invalid selection");
                        return;
                    }

                    Ride selectedRide = pendingRides.get(choice - 1);
                    out.writeUTF("Selected ride: Customer: " + selectedRide.getCustomerUsername() +
                            ", From: " + selectedRide.getPickupLocation() +
                            ", To: " + selectedRide.getDropLocation() +
                            ", Customer's fare: $" + selectedRide.getCustomerFare());

                    // Get fare offer
                    String fareOffer = in.readUTF();

                    // Create and store the bid
                    Bid newBid = new Bid(username, fareOffer, selectedRide.getRideId());
                    selectedRide.addBid(newBid);

                    out.writeUTF("SUCCESS: Your bid has been submitted successfully. You will be notified if the customer accepts.");

                    System.out.println("Driver " + username + " offered fare $" + fareOffer +
                            " for ride " + selectedRide.getRideId());

                    // Notify the customer
                    ClientHandler customerHandler = activeCustomers.get(selectedRide.getCustomerUsername());
                    if (customerHandler != null) {
                        customerHandler.sendMessage("BID_NOTIFICATION:" + username + ":" +
                                fareOffer + ":" + selectedRide.getRideId());
                    }
                }

                private void updateRideStatus() throws IOException {
                    out.writeUTF("INFO: Feature not implemented yet");
                }

                private void cleanup() {
                    running = false;
                    if (username != null) {
                        if (isCustomer) {
                            activeCustomers.remove(username);
                            System.out.println("Customer " + username + " disconnected");
                        } else {
                            activeDrivers.remove(username);
                            System.out.println("Driver " + username + " disconnected");
                        }
                    }
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                        if (clientSocket != null) clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
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