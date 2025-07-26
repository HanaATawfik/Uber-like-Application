// File Name: GreetingClient.java
            import java.io.BufferedReader;
            import java.io.DataOutputStream;
            import java.io.IOException;
            import java.io.InputStreamReader;
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
                            handleCustomer(client);
                        } else if (mainChoice == 2) {
                            handleDriver(client);
                        } else {
                            System.out.println("Invalid choice. Exiting.");
                            client.close();
                            return;
                        }

                        // Commented out code for receiving server response
                        // InputStream inFromServer = client.getInputStream();
                        // DataInputStream in = new DataInputStream(inFromServer);
                        // System.out.println("Server says " + in.readUTF());

                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                /**
                 * Handle Customer signup or login.
                 *
                 * @param client Socket connection to server
                 * @throws IOException If there's an error with I/O operations
                 */
                private static void handleCustomer(Socket client) throws IOException {
                    System.out.println("You chose Customer");
                    System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
                    System.out.println("1. Sign up");
                    System.out.println("2. Log in");

                    BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
                    int choice = Integer.parseInt(buffReader.readLine());

                    if (choice == 1) {
                        handleSignup(client, buffReader, choice);
                    } else if (choice == 2) {
                        handleLogin(client, buffReader, choice);
                    } else {
                        System.out.println("Invalid choice. Exiting.");
                        client.close();
                    }
                }

                /**
                 * Handle Driver signup or login.
                 *
                 * @param client Socket connection to server
                 * @throws IOException If there's an error with I/O operations
                 */
                private static void handleDriver(Socket client) throws IOException {
                    System.out.println("You chose Driver");
                    System.out.println("Please choose whether you want to Sign up or Log in by either entering 1 or 2");
                    System.out.println("1. Sign up");
                    System.out.println("2. Log in");

                    BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
                    int choice = Integer.parseInt(buffReader.readLine());

                    if (choice == 1) {
                        handleSignup(client, buffReader, choice);
                    } else if (choice == 2) {
                        handleLogin(client, buffReader, choice);
                    } else {
                        System.out.println("Invalid choice. Exiting.");
                        client.close();
                    }
                }

                /**
                 * Handle signup process.
                 *
                 * @param client Socket connection to server
                 * @param buffReader BufferedReader for reading user input
                 * @param choice User's choice (1 for signup)
                 * @throws IOException If there's an error with I/O operations
                 */
                private static void handleSignup(Socket client, BufferedReader buffReader, int choice) throws IOException {
                    System.out.println("You chose to Sign up");

                    System.out.println("Please enter your email:");
                    String email = buffReader.readLine();

                    System.out.println("Please enter your username:");
                    String username = buffReader.readLine();

                    System.out.println("Please enter your password:");
                    String password = buffReader.readLine();

                    // Send signup data to server
                    DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
                    outToServer.writeUTF(String.valueOf(choice));
                    outToServer.writeUTF(email);
                    outToServer.writeUTF(username);
                    outToServer.writeUTF(password);
                }

                /**
                 * Handle login process.
                 *
                 * @param client Socket connection to server
                 * @param buffReader BufferedReader for reading user input
                 * @param choice User's choice (2 for login)
                 * @throws IOException If there's an error with I/O operations
                 */
                private static void handleLogin(Socket client, BufferedReader buffReader, int choice) throws IOException {
                    System.out.println("You chose to Log in");

                    System.out.println("Please enter your username:");
                    String username = buffReader.readLine();

                    System.out.println("Please enter your password:");
                    String password = buffReader.readLine();

                    // Send login data to server
                    DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
                    outToServer.writeUTF(String.valueOf(choice));
                    outToServer.writeUTF(username);
                    outToServer.writeUTF(password);
                }
            }