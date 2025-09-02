// File Name: GreetingServer.java
                    import java.io.*;
                    import java.net.*;
                    import java.util.Hashtable;

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
                            public static Hashtable<String, Customer> ccredentials = new Hashtable<String, Customer>();

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
                            public static Hashtable<String, Driver> dcredentials = new Hashtable<String, Driver>();

                            public Driver(String email, String username, String password) {
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
                            public void putdcredentials(Driver driver) {
                                dcredentials.put(getusername(), driver);
                            }
                        }

                        static class Ride {
                            private final String pickupLocation;
                            private final String dropLocation;
                            private final String customerUsername;
                            private final String driverUsername;
                            public static Hashtable<String, Ride> rides = new Hashtable<String, Ride>();

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
                                        handleCustomerRequest(in, out);
                                    } else if (mainChoice == 2) {
                                        handleDriverRequest(in, out);
                                    } else {
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
                            private void handleCustomerRequest(DataInputStream in, DataOutputStream out) throws IOException {
                                System.out.println("Client chose Customer");
                                int d = 1;
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
                             * @param d Type of user (1 for Customer, 2 for Driver)
                             * @param in DataInputStream to read user data
                             * @param out DataOutputStream to send response
                             * @throws IOException If there's an error with I/O operations
                             */
                            private void processSignup(int d, DataInputStream in, DataOutputStream out) throws IOException {
                                System.out.println("Client chose to Sign up");

                                if (d == 1) {
                                    System.out.println("Customer");
                                    String email = in.readUTF();
                                    String username = in.readUTF();
                                    String password = in.readUTF();

                                    if (Customer.ccredentials.containsKey(username)) {
                                        System.out.println("Username already exists");
                                        out.writeUTF("FAILURE: Username already exists");
                                    } else {
                                        Customer c = new Customer(email, username, password);
                                        c.putccredentials(c);
                                        System.out.println("Received Sign up data: Email: " + c.getemail() +
                                                ", Username: " + c.getusername() + ", Password: " + c.getpassword());
                                        out.writeUTF("SUCCESS: Customer signup successful");
                                        processLogin(d, in, out);
                                    }
                                } else {
                                    System.out.println("Driver");
                                    String email = in.readUTF();
                                    String username = in.readUTF();
                                    String password = in.readUTF();

                                    if (Driver.dcredentials.containsKey(username)) {
                                        System.out.println("Username already exists");
                                        out.writeUTF("FAILURE: Username already exists");
                                    } else {
                                        Driver dr = new Driver(email, username, password);
                                        dr.putdcredentials(dr);
                                        System.out.println("Received Sign up data: Email: " + dr.getemail() +
                                                ", Username: " + dr.getusername() + ", Password: " + dr.getpassword());
                                        out.writeUTF("SUCCESS: Driver signup successful");
                                        processLogin(d, in, out);
                                    }
                                }
                            }

                            /**
                             * Process user login data.
                             *
                             * @param d Type of user (1 for Customer, 2 for Driver)
                             * @param in DataInputStream to read user data
                             * @param out DataOutputStream to send response
                             * @throws IOException If there's an error with I/O operations
                             */
                            private void processLogin(int d, DataInputStream in, DataOutputStream out) throws IOException {
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
                                    } else {
                                        System.out.println("Invalid username or password.");
                                        out.writeUTF("FAILURE: Invalid username or password");
                                    }
                                } else {
                                    System.out.println("Driver");
                                    if (Driver.dcredentials.containsKey(username) &&
                                        Driver.dcredentials.get(username).getpassword().equals(password)) {
                                        System.out.println("Login successful driver!");
                                        out.writeUTF("SUCCESS: Driver login successful");
                                    } else {
                                        System.out.println("Invalid username or password.");
                                        out.writeUTF("FAILURE: Invalid username or password");
                                    }
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