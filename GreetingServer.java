// File Name: GreetingServer.java
                import java.io.DataInputStream;
                import java.io.DataOutputStream;
                import java.io.IOException;
                import java.net.ServerSocket;
                import java.net.Socket;
                import java.net.SocketTimeoutException;

                /**
                 * Server application that handles client connections
                 * and processes customer/driver signup and login requests.
                 */
                public class GreetingServer extends Thread {
                    private ServerSocket serverSocket;
                    private static final int TIMEOUT = 100000;

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

                                // Send response to client
                                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                                out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                                        + "\nGoodbye!");

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
                    private void handleCustomerRequest(Socket server) throws IOException {
                        System.out.println("Client chose Customer");
                        DataInputStream in = new DataInputStream(server.getInputStream());
                        int choice = Integer.parseInt(in.readUTF());

                        if (choice == 1) {
                            processSignup(in);
                        } else if (choice == 2) {
                            processLogin(in);
                        } else {
                            System.out.println("Invalid choice received from client.");
                        }
                    }

                    /**
                     * Handle driver signup or login requests.
                     *
                     * @param server The client socket
                     * @throws IOException If there's an error reading from socket
                     */
                    private void handleDriverRequest(Socket server) throws IOException {
                        System.out.println("Client chose Driver");
                        DataInputStream in = new DataInputStream(server.getInputStream());
                        int choice = Integer.parseInt(in.readUTF());

                        if (choice == 1) {
                            processSignup(in);
                        } else if (choice == 2) {
                            processLogin(in);
                        } else {
                            System.out.println("Invalid choice received from client.");
                        }
                    }

                    /**
                     * Process user signup data.
                     *
                     * @param in DataInputStream to read user data
                     * @throws IOException If there's an error reading from stream
                     */
                    private void processSignup(DataInputStream in) throws IOException {
                        System.out.println("Client chose to Sign up");
                        String email = in.readUTF();
                        String username = in.readUTF();
                        String password = in.readUTF();
                        System.out.println("Received Sign up data: Email: " + email +
                                ", Username: " + username + ", Password: " + password);
                    }

                    /**
                     * Process user login data.
                     *
                     * @param in DataInputStream to read user data
                     * @throws IOException If there's an error reading from stream
                     */
                    private void processLogin(DataInputStream in) throws IOException {
                        System.out.println("Client chose to Log in");
                        String username = in.readUTF();
                        String password = in.readUTF();
                        System.out.println("Received Log in data: Username: " + username +
                                ", Password: " + password);
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