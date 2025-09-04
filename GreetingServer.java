// File Name: GreetingServer.java
                    import java.io.*;
                    import java.net.*;
                    import java.util.Iterator;
                    import java.util.Map;
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
                                this.status=status;
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
                            public void putdcredentials(Driver driver) {
                                dcredentials.put(getusername(), driver);
                            }
                        }

                        static class Ride {
                            private final String pickupLocation;
                            private final String dropLocation;
                            private final String customerUsername;
                            private String driverUsername;
                            public static ConcurrentHashMap<String, Ride> rides = new ConcurrentHashMap<String, Ride>();

                            public Ride(String pickupLocation, String dropLocation, String customerUsername, String driverUsername) {
                                this.pickupLocation = pickupLocation;
                                this.dropLocation = dropLocation;
                                this.customerUsername = customerUsername;
                                this.driverUsername = driverUsername;
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
                            public void putRide(Ride ride) {
                                rides.put(ride.getCustomerUsername(), ride);
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
                        //all logic for handling client requests is moved here
                        private static class ClientHandler implements Runnable {
                            private final Socket clientSocket;

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
                                        if (handleCustomerRequest(in, out))
                                        {
                                            int menuChoice = Integer.parseInt(in.readUTF());

                                            processMenuSelection(in, out,menuChoice);
                                        }
                                    }
                                    else if (mainChoice == 2) {
                                        handleDriverRequest(in, out);
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
                             *
                             * @param in DataInputStream to read from client
                             * @param out DataOutputStream to send responses
                             * @throws IOException If there's an error reading from socket
                             */

                            Customer c = new Customer(null, null, null);

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
                             *
                             * @param in DataInputStream to read from client
                             * @param out DataOutputStream to send responses
                             * @throws IOException If there's an error reading from socket
                             */
                            private void handleDriverRequest(DataInputStream in, DataOutputStream out) throws IOException {
                                System.out.println("Client chose Driver");
                                int d = 2;
                                int choice = Integer.parseInt(in.readUTF());

                                if (choice == 1) {
                                    processSignup(d, in, out);
                                } else if (choice == 2) {
                                    processLogin(d, in, out);
                                } else {
                                    System.out.println("Invalid choice received from client.");
                                    out.writeUTF("Invalid choice");
                                }
                            }

                            /**
                             * Process user signup data.
                             *
                             * @param d   Type of user (1 for Customer, 2 for Driver)
                             * @param in  DataInputStream to read user data
                             * @param out DataOutputStream to send response
                             * @return
                             * @throws IOException If there's an error with I/O operations
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
                                     //   Customer c = new Customer(email, username, password);
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
                                        Driver dr = new Driver(email, username, password,status);
                                        dr.putdcredentials(dr);
                                        System.out.println("Received Sign up data: Email: " + dr.getemail() +
                                                ", Username: " + dr.getusername() + ", Password: " + dr.getpassword()+", Status: " + dr.getstatus());
                                        out.writeUTF("SUCCESS: Driver signup successful");
                                        processLogin(d, in, out);
                                        return true;
                                    }
                                }
                            }

                            /**
                             * Process user login data.
                             *
                             * @param d   Type of user (1 for Customer, 2 for Driver)
                             * @param in  DataInputStream to read user data
                             * @param out DataOutputStream to send response
                             * @return
                             * @throws IOException If there's an error with I/O operations
                             */
                            private <bool> boolean processLogin(int d, DataInputStream in, DataOutputStream out) throws IOException {
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
                                        out.writeUTF("SUCCESS: Driver login successful");
                                        return true;
                                    } else {
                                        System.out.println("Invalid username or password.");
                                        out.writeUTF("FAILURE: Invalid username or password");
                                        return false;
                                    }
                                }
                            }

                            private  void processMenuSelection(DataInputStream in, DataOutputStream out, int menuChoice) throws IOException {
                                System.out.println("Waiting for menu selection from client...");
                                 //menuChoice = Integer.parseInt(in.readUTF());
                                if (menuChoice == 1) {
                                    processRideRequest(in, out);
                                }
                                else if (menuChoice == 2) {
                                    // Future implementation for viewing ride history
                                    out.writeUTF("Feature not implemented yet");
                                }
                                else if (menuChoice == 3) {
                                    // Future implementation for updating profile
                                    out.writeUTF("Feature not implemented yet");
                                }
                                else {
                                    System.out.println("Invalid menu choice received from client.");
                                    out.writeUTF("Invalid menu choice");
                                }
                            }

                            private void processRideRequest(DataInputStream in, DataOutputStream out) throws IOException {

                                System.out.println("Client chose to Request a Ride");
                                String pickupLocation = in.readUTF();
                                String dropLocation = in.readUTF();
                                String customerUsername = c.getusername();
                                String driverUsername = null;

                                Ride ride = new Ride(pickupLocation, dropLocation, customerUsername, driverUsername);
                                ride.putRide(ride);
                                System.out.println("Received Ride data: Pickup Location: " + ride.getPickupLocation() +
                                        ", Drop Location: " + ride.getDropLocation() +
                                        ", Customer Username: " + ride.getCustomerUsername());

                                // Find an available driver
                                Iterator<Map.Entry<String, Driver>> iterator = Driver.dcredentials.entrySet().iterator();
                                while (iterator.hasNext()) {

                                    Map.Entry<String, Driver> entry = iterator.next();
                                    Driver driver = entry.getValue();

                                    if (driver.getstatus().equals("available")) {
                                        driverUsername = driver.getusername();
                                        // Update driver status to unavailable
                                        ride.driverUsername=driverUsername;
                                        driver.status = "unavailable";
                                        out.writeUTF("SUCCESS: Ride request successful");
                                        out.writeUTF("Driver assigned: " + driverUsername);
                                        break;
                                    }

                                }
                                if (driverUsername == null) {
                                    System.out.println("No available drivers at the moment.");
                                    out.writeUTF("FAILURE: No available drivers at the moment");
                                    return;
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