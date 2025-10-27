import java.io.*;
                                import java.net.*;
                                import java.util.*;
                                import java.util.concurrent.ConcurrentHashMap;
                                import java.util.concurrent.atomic.AtomicInteger;

                                public class GreetingServer extends Thread {
                                    private ServerSocket serverSocket = null;
                                    private static final int TIMEOUT = 0;
                                    private static final AtomicInteger rideIdCounter = new AtomicInteger(1);

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
                                        private String status = "PENDING";
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

                                    static class MenuHandler {
                                        private final ClientHandler handler;

                                        public MenuHandler(ClientHandler handler) {
                                            this.handler = handler;
                                        }

                                        public void handleCustomerMenu(int choice) throws IOException {
                                            switch (choice) {
                                                case 1:
                                                    handler.processRideRequest();
                                                    break;
                                                case 2:
                                                    handler.viewRideStatus();
                                                    break;
                                                case 3:
                                                    handler.acceptDriverBid();
                                                    break;
                                                case 4:
                                                    System.out.println("Customer " + handler.username + " disconnecting...");
                                                    handler.running = false;
                                                    break;
                                                default:
                                                    handler.out.writeUTF("FAILURE: Invalid menu choice");
                                            }
                                        }

                                        public void handleDriverMenu(int choice) throws IOException {
                                            switch (choice) {
                                                case 1:
                                                    handler.offerRideFare();
                                                    break;
                                                case 2:
                                                    handler.updateRideStatus();
                                                    break;
                                                case 3:
                                                    System.out.println("Driver " + handler.username + " disconnecting...");
                                                    handler.running = false;
                                                    break;
                                                default:
                                                    handler.out.writeUTF("FAILURE: Invalid menu choice");
                                            }
                                        }
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
                                        private MenuHandler menuHandler;

                                        public ClientHandler(Socket socket) {
                                            this.clientSocket = socket;
                                            this.menuHandler = new MenuHandler(this);
                                        }

                                        private String readNonEmptyString() throws IOException {
                                            String input = in.readUTF();
                                            if (input == null || input.trim().isEmpty()) {
                                                throw new IOException("Empty input received");
                                            }
                                            return input.trim();
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
                                                    out.writeUTF("FAILURE: Invalid choice");
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
                                                    menuHandler.handleCustomerMenu(menuChoice);
                                                }
                                            } catch (IOException e) {
                                                System.out.println("Customer " + username + " connection error: " + e.getMessage());
                                            }
                                        }

                                        private void driverMainLoop() {
                                            try {
                                                while (running) {
                                                    int menuChoice = Integer.parseInt(in.readUTF());
                                                    menuHandler.handleDriverMenu(menuChoice);
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
                                                out.writeUTF("FAILURE: Invalid choice");
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
                                                out.writeUTF("FAILURE: Invalid choice");
                                                return false;
                                            }
                                        }

                                        private boolean processSignup(int userType) throws IOException {
                                            System.out.println("Client chose to Sign up");

                                            try {
                                                String email = readNonEmptyString();
                                                String signupUsername = readNonEmptyString();
                                                String password = readNonEmptyString();

                                                if (userType == 1) {
                                                    if (Customer.ccredentials.containsKey(signupUsername)) {
                                                        out.writeUTF("FAILURE: Username already exists");
                                                        return false;
                                                    } else {
                                                        customer = new Customer(email, signupUsername, password);
                                                        customer.putCredentials(customer);
                                                        this.username = signupUsername;
                                                        System.out.println("Customer signup and auto-login: " + username);
                                                        out.writeUTF("SUCCESS: Customer signup successful. You are now logged in.");
                                                        return true;
                                                    }
                                                } else {
                                                    if (Driver.dcredentials.containsKey(signupUsername)) {
                                                        out.writeUTF("FAILURE: Username already exists");
                                                        return false;
                                                    } else {
                                                        driver = new Driver(email, signupUsername, password, "available");
                                                        driver.putCredentials(driver);
                                                        this.username = signupUsername;
                                                        System.out.println("Driver signup and auto-login: " + username);
                                                        out.writeUTF("SUCCESS: Driver signup successful. You are now logged in.");
                                                        return true;
                                                    }
                                                }
                                            } catch (IOException e) {
                                                out.writeUTF("FAILURE: Invalid input - fields cannot be empty");
                                                return false;
                                            }
                                        }

                                        private boolean processLogin(int userType) throws IOException {
                                            System.out.println("Client chose to Log in");

                                            try {
                                                String loginUsername = readNonEmptyString();
                                                String password = readNonEmptyString();

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
                                            } catch (IOException e) {
                                                out.writeUTF("FAILURE: Invalid input - fields cannot be empty");
                                                return false;
                                            }
                                        }

                                        private void processRideRequest() throws IOException {
                                            try {


                                                String pickupLocation = readNonEmptyString();
                                                String dropLocation = readNonEmptyString();
                                                String customerFare = readNonEmptyString();

                                                String rideId = "RIDE" + rideIdCounter.getAndIncrement();
                                                Ride newRide = new Ride(rideId, pickupLocation, dropLocation, username, customerFare);
                                                newRide.putRide(newRide);

                                                // Check if any drivers are registered in the system
                                                if (Driver.dcredentials.isEmpty()) {
                                                    out.writeUTF("FAILURE: No available drivers at the moment. Please try again later.");
                                                    System.out.println("Ride request rejected: No drivers registered in the system");
                                                    return;
                                                }

                                                out.writeUTF("SUCCESS: Ride request created with ID " + rideId +
                                                        ". Pickup: " + pickupLocation + ", Drop-off: " + dropLocation +
                                                        ", Your fare: $" + customerFare + ". Waiting for driver bids...");

                                                System.out.println("Customer " + username + " requested ride " + rideId +
                                                        " from " + pickupLocation + " to " + dropLocation +
                                                        " with fare $" + customerFare);
                                            } catch (IOException e) {
                                                out.writeUTF("FAILURE: Error processing ride request - " + e.getMessage());
                                                System.out.println("Error processing ride request for customer " + username + ": " + e.getMessage());
                                            }
                                        }

                                        private void viewRideStatus() throws IOException {
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
                                            try {
                                                String driverUsername = readNonEmptyString();

                                                Driver assignedDriver = Driver.dcredentials.get(driverUsername);
                                                if (assignedDriver == null) {
                                                    out.writeUTF("FAILURE: Driver not found in the system.");
                                                    return;
                                                }

                                                if (!assignedDriver.getStatus().equals("available")) {
                                                    out.writeUTF("FAILURE: Driver is no longer available. They may have accepted another ride.");
                                                    return;
                                                }

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

                                                pendingRide.setDriverUsername(driverUsername);
                                                pendingRide.setStatus("ASSIGNED");
                                                assignedDriver.setStatus("busy");

                                                out.writeUTF("SUCCESS: Ride " + pendingRide.getRideId() +
                                                        " assigned to driver " + driverUsername +
                                                        " with fare $" + acceptedBid.getFareOffer());

                                                ClientHandler driverHandler = activeDrivers.get(driverUsername);
                                                if (driverHandler != null) {
                                                    driverHandler.sendMessage("SUCCESS: Customer " + username +
                                                            " accepted your bid of $" + acceptedBid.getFareOffer() +
                                                            " for ride " + pendingRide.getRideId() +
                                                            " from " + pendingRide.getPickupLocation() +
                                                            " to " + pendingRide.getDropLocation());
                                                }

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
                                            } catch (IOException e) {
                                                out.writeUTF("FAILURE: Driver username cannot be empty");
                                            }
                                        }

                                        private void offerRideFare() throws IOException {
                                            System.out.println("Processing driver menu selection: 1");
                                            System.out.println("Driver chose to Offer Ride Fare");

                                            Driver currentDriver = Driver.dcredentials.get(username);
                                            if (currentDriver == null || !currentDriver.getStatus().equals("available")) {
                                                out.writeUTF("FAILURE: You are not available to bid on rides.");
                                                return;
                                            } else {
                                                out.writeUTF("INFO: You are available and can bid on rides.");
                                            }

                                            List<Ride> pendingRides = new ArrayList<>();
                                            for (Ride ride : Ride.rides.values()) {
                                                if (ride.getStatus().equals("PENDING")) {
                                                    pendingRides.add(ride);
                                                }
                                            }

                                            if (pendingRides.isEmpty()) {
                                                out.writeUTF("INFO: No ride requests available at this time.");
                                                return;
                                            }

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

                                            String fareOffer = in.readUTF();

                                            Bid newBid = new Bid(username, fareOffer, selectedRide.getRideId());
                                            selectedRide.addBid(newBid);

                                            out.writeUTF("SUCCESS: Your bid has been submitted successfully. You will be notified if the customer accepts.");

                                            System.out.println("Driver " + username + " offered fare $" + fareOffer +
                                                    " for ride " + selectedRide.getRideId());

                                            ClientHandler customerHandler = activeCustomers.get(selectedRide.getCustomerUsername());
                                            if (customerHandler != null) {
                                                customerHandler.sendMessage("BID_NOTIFICATION:" + username + ":" +
                                                        fareOffer + ":" + selectedRide.getRideId());
                                            }
                                        }

                                        private void updateRideStatus() throws IOException {
                                            Ride assignedRide = null;
                                            for (Ride ride : Ride.rides.values()) {
                                                if (username.equals(ride.getDriverUsername()) &&
                                                        !ride.getStatus().equals("COMPLETED")) {
                                                    assignedRide = ride;
                                                    break;
                                                }
                                            }

                                            if (assignedRide == null) {
                                                out.writeUTF("INFO: You have no active ride to update.");
                                                return;
                                            }

                                            out.writeUTF("RIDE_STATUS_MENU:Current Status: " + assignedRide.getStatus());

                                            String statusChoice = in.readUTF();

                                            switch (statusChoice) {
                                                case "1":
                                                    if (assignedRide.getStatus().equals("ASSIGNED")) {
                                                        assignedRide.setStatus("IN_PROGRESS");
                                                        out.writeUTF("SUCCESS: Ride status updated to IN_PROGRESS");

                                                        ClientHandler customerHandler = activeCustomers.get(assignedRide.getCustomerUsername());
                                                        if (customerHandler != null) {
                                                            customerHandler.sendMessage("INFO: Your driver has started the ride.");
                                                        }
                                                    } else {
                                                        out.writeUTF("FAILURE: Ride cannot be started from current status.");
                                                    }
                                                    break;

                                                case "2":
                                                    if (assignedRide.getStatus().equals("IN_PROGRESS")) {
                                                        assignedRide.setStatus("COMPLETED");
                                                        driver.setStatus("available");
                                                        out.writeUTF("SUCCESS: Ride completed. You are now available for new rides.");

                                                        ClientHandler customerHandler = activeCustomers.get(assignedRide.getCustomerUsername());
                                                        if (customerHandler != null) {
                                                            customerHandler.sendMessage("SUCCESS: Your ride has been completed by driver " + username);
                                                        }
                                                    } else {
                                                        out.writeUTF("FAILURE: Ride must be in progress to complete.");
                                                    }
                                                    break;

                                                case "3":
                                                    out.writeUTF("INFO: Status update cancelled.");
                                                    break;

                                                default:
                                                    out.writeUTF("FAILURE: Invalid choice.");
                                            }
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