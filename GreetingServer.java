// File Name: GreetingServer.java
                import java.io.DataInputStream;
                import java.io.DataOutputStream;
                import java.io.IOException;
                import java.net.ServerSocket;
                import java.net.Socket;
                import java.net.SocketTimeoutException;
                import java.util.Hashtable;

                /**
                 * Server application that handles client connections
                 * and processes customer/driver signup and login requests.
                 */
                final class customer {
                    private final String username;
                    private final String email;
                    private final String password;
                    public static Hashtable<String, customer> ccredentials = new Hashtable<String, customer>();

                    public customer(String email, String username, String password) {
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
                    public void putccredentials(customer customer) {
                        ccredentials.put(getusername(), customer);
                    }
                }

                final class driver {
                    private final String username;
                    private final String email;
                    private final String password;
                    public static Hashtable<String, driver> dcredentials = new Hashtable<String, driver>();

                    public driver(String email, String username, String password) {
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
                    public void putdcredentials(driver driver) {
                        dcredentials.put(getusername(), driver);
                    }
                }

                public class GreetingServer extends Thread {
                    private ServerSocket serverSocket;
                    private static final int TIMEOUT = 1000000;

                    /**
                     * Constructor to initialize server on specified port.
                     *
                     * @param port The port number to listen on
                     * @throws IOException If server socket cannot be created
                     */
                    public GreetingServer(int port) throws IOException {
                        serverSocket = new ServerSocket(port);
                       serverSocket.setSoTimeout(TIMEOUT);
                    }

                    @Override
                    public void run() {
                        while (true) {
                            try {
                                System.out.println("Waiting for client on port " +
                                        serverSocket.getLocalPort() + "...");

                                Socket server = serverSocket.accept();
                                System.out.println("Just connected to " + server.getRemoteSocketAddress());

                                // Process client request
                                DataInputStream mainIn = new DataInputStream(server.getInputStream());
                                int mainChoice = Integer.parseInt(mainIn.readUTF());

                                if (mainChoice == 1) {
                                    handleCustomerRequest(server);
                                } else if (mainChoice == 2) {
                                    handleDriverRequest(server);
                                } else {
                                    System.out.println("Invalid choice received from client.");
                                }

                                // Removing the goodbye message since we're now sending specific responses
                                server.close();
                            } catch (SocketTimeoutException s) {
                                System.out.println("Socket timed out!");
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }

                    /**
                     * Handle customer signup or login requests.
                     *
                     * @param server The client socket
                     * @throws IOException If there's an error reading from socket
                     */
                    public void handleCustomerRequest(Socket server) throws IOException {
                        System.out.println("Client chose Customer");
                        int d = 1;
                        DataInputStream in = new DataInputStream(server.getInputStream());
                        DataOutputStream out = new DataOutputStream(server.getOutputStream());
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
                     * @param server The client socket
                     * @throws IOException If there's an error reading from socket
                     */
                    public void handleDriverRequest(Socket server) throws IOException {
                        System.out.println("Client chose Driver");
                        int d = 2;
                        DataInputStream in = new DataInputStream(server.getInputStream());
                        DataOutputStream out = new DataOutputStream(server.getOutputStream());
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

                            if (customer.ccredentials.containsKey(username)) {
                                System.out.println("Username already exists");
                                out.writeUTF("FAILURE: Username already exists");
                            } else {
                                customer c = new customer(email, username, password);
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

                            if (driver.dcredentials.containsKey(username)) {
                                System.out.println("Username already exists");
                                out.writeUTF("FAILURE: Username already exists");
                            } else {
                                driver dr = new driver(email, username, password);
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
                            if (customer.ccredentials.containsKey(username) &&
                                customer.ccredentials.get(username).getpassword().equals(password)) {
                                System.out.println("Login successful customer!");
                                out.writeUTF("SUCCESS: Customer login successful");
                            } else {
                                System.out.println("Invalid username or password.");
                                out.writeUTF("FAILURE: Invalid username or password");
                            }
                        } else {
                            System.out.println("Driver");
                            if (driver.dcredentials.containsKey(username) &&
                                driver.dcredentials.get(username).getpassword().equals(password)) {
                                System.out.println("Login successful driver!");
                                out.writeUTF("SUCCESS: Driver login successful");
                            } else {
                                System.out.println("Invalid username or password.");
                                out.writeUTF("FAILURE: Invalid username or password");
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