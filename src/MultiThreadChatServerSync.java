import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/* @Author Andreas Harteg
*
** Mandatory Assignment 1 - Chat system (SWC3)
* You should code a Chat system, with a chat client that can connect to a chat server.
* You may need to use threads in client and/or in server.
* The client should at the start ask the user his/her chat-name(username) and then send
* a join message to the server.
* The server should accept clients to join the chat system, using a protocol specified
* below.
* When a client joins, the server should maintain and update a list of all active clients.
* The server will need to save for each client the user name, IP address and Port number.
* If a new user tries to join with the same name as an already active user, then an error
* message should be sent back to client. Client can try again with a new name.
* An active client can send user text message to the server that will just send a copy to all
* active clients in the client list.
* The Client must send a “heartbeat alive” message once every minute to the Server.
* The server should (maybe with a specialized thread) check the active list, and delete clients
* that stop sending heartbeat messages. Maybe the active list should include last heartbeat time.
* The Client must send a Quit message when it is closing.

The solution will use TCP to communicate.
Protocol between Chat server and client:
List of allowed messages (and their meaning):

JOIN <>, <>:<>
From client to server.
The user name is given by the user. Username is max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed.

J_OK
From server to client.
Client is accepted.

J_ER <>: <>
From server to client.
Client not accepted. Duplicate username, unknown command, bad command or any other errors.

DATA <>: <>
From client to server.
From server to all clients.
First part of message indicates from which user it is, the colon(:) indicates where the user message begins.
Max 250 user characters.

IMAV
From client to server.
Client sends this heartbeat alive every 1 minute.

QUIT
From client to server.
Client is closing down and leaving the group.

LIST <>
From server to client.
A list of all active user names is sent to all clients, each time the list at the server changes.
Note:
This notation <> indicates a placeholder, and they need to be replaced with appropriate content.
E.g.:
JOIN <>, <>:<>
Might look like this:
JOIN alice_92, 172.168.168.12:4578
 */

public class MultiThreadChatServerSync {

    private static ServerSocket serverSocket = null;
    private static Socket       clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 20;
    private static final ClientThread[] threads = new ClientThread[maxClientsCount];

    public static void main(String[] args) {

        int portNumber = 2222;      // The default server port number
        if (args.length < 1) {
            System.out.println("Usage: java MultiThreadChatServerSync <portNumber>\n"
                    + "Now using port number=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        /*
         * Open a server socket on the portNumber (default 2222)
         */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Create a clientSocket for a connection on the serverSocket.
         * Check if there is enough room on the server pass the connection to a new client thread.
         */
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                // Find a place for new user
                int i = 0;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new ClientThread(clientSocket, threads, maxClientsCount)).start();
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy... Try later.");
                    os.close();
                    clientSocket.close();
                }

            } catch (IOException e) {
                System.out.println(e);
            }

        }
    }

}

class ClientThread extends Thread {

    private String clientName       = null;
    private Socket clientSocket     = null;
    private Scanner sis             = null;
    private PrintStream os          = null;
    private final ClientThread[] threads;
    private int maxClientsCount;
    private String userList;

    private boolean acceptedClient = false;
    private boolean closingThread = false;

    private long lastHeartbeat      = System.currentTimeMillis();

    //test
    public ClientThread(String clientName, ClientThread[] threads) {
        this.clientName = clientName;
        this.threads = threads;
    }

    public ClientThread(Socket clientSocket, ClientThread[] threads, int maxClientsCount) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.maxClientsCount = maxClientsCount;
    }

    public void run() {

        try {
            /*
             * Create input and output streams for this client
             */
            sis = new Scanner(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());

            /*
             * Starts the heartbeat timer.
             * Checks every 2 minutes if there has been a heartbeat message from the client
             * within the last 2 minutes.
             * Calls closeThread() if no heartbeat has been received.
             */
            Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if ((System.currentTimeMillis() - lastHeartbeat) > 120000) {
                        System.out.println("Heartbeat timer closing connection");

                        os.println("Heartbeat timer closing connection");

                        for (int i = 0; i < maxClientsCount; i++) {
                            if (threads[i] != null) {
                                threads[i].os.println("DATA " + clientName + ": - has left the chat");
                            }
                        }

                        closingThread = true;

                        closeThread(t);

                    }
                }
            }, 2000, 120000);


            /*
             * Asks the client for username.
             * Username is max 12 chars long, only letters, digits, '-' and '_' allowed.
            */
            os.println("Welcome to the chat server!");

            String name;
            boolean breakLoop = true;
            while (true) {

                breakLoop = true;
                os.println("Enter your user name:");
                name = inputProtocolFilter().trim();

                //check if username contain illegal character
                if (illegalChar(name)) {
                    continue;
                }

                //check if the username already exists ignoring case
                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] != this && threads[i].clientName.equalsIgnoreCase(name)) {
                            //send error
                            sendErrMsg(203, "User already exits on chat server");
                            breakLoop = false;
                            break;
                        }
                    }
                }
                if (breakLoop) {
                    break;
                }
            }

            //Client has joined server
            acceptedClient = true;

            //Send J_OK to client - acceptance message
            os.println("J_OK " + name);

            /* Welcome the new client. */
            os.println("Welcome " + name + " to our chat room.\nTo leave enter QUIT in a new line.");

            synchronized (this) {
                //save username
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = name;
                        break;
                    }
                }

                //Collecting active user names and saving in string
                userList = "";
                for (int i = 0; i < maxClientsCount ; i++) {
                    if(threads[i] != null && threads[i].clientName != null) {
                        userList = userList + threads[i].clientName +" ";
                    }
                }
                System.out.println("Userlist: " + userList);

                //Sending user list to all clients when new user joins the chat
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null) {
                        threads[i].os.println("LIST " + userList);
                    }
                }
            }

            // Open for conversation - main loop
            // Echoing received data from client to all clients in chat
            System.out.println("Conversation open");
            while (!closingThread) {
                String line;
                line = inputProtocolFilter().trim();
                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] != this) {
                            threads[i].os.println(line);
                        }
                    }
                }
                //Echo back to sending client
                //If closing thread is set, server tells client to QUIT
                if (!closingThread) {
                    os.println(line);
                } else {
                    os.println("QUIT");
                }

                System.out.println(line);
            }

            //Closing thread
            closeThread(t);

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /*
     * Protocol error messages:
     * J_ER <<err_code>>: <<err_msg>>
     * 100: Bad command - Client must join server before sending data
     * 101: Bad command - Client has already joined server
     * 102: Bad command - Unknown command
     * 200: Bad input - No input entered
     * 201: Bad input - Username can max be 12 characters long
     * 202: Bad input - Only letters, digits, '-' and '_' allowed in username
     * 203: Bad input - User already exits on chat server
     */
    private void sendErrMsg(int err_code, String err_msg) {
        String errMessages = "J_ER " + err_code + ": " + err_msg;
        os.println(errMessages);
    }

    /*
     * Protocol - client to server:
     * JOIN <<user_name>>, <<server_ip>>:<<server_port>>
     * DATA <<user_name>>: <<free text…>>
     * IMAV
     * QUIT
     */
    private String inputProtocolFilter() {

        String line;
        String command;
        while (true) {
            line = sis.nextLine();
            if(line.length() < 4) {
                command = line;
            } else {
                command = line.substring(0,4);
            }

            switch (command)
            {
                case "JOIN":
                    if (!acceptedClient) {
                        if (line.length() < 5 || line.indexOf(',') == -1) {
                            sendErrMsg(204, "JOIN message protocol: JOIN <<user_name>>, <<server_ip>>:<<server_port>>");
                            return "";
                        }
                        return line.substring(5, line.indexOf(','));
                    } else {
                        sendErrMsg(101, "Client is already joined");
                        break;
                    }
                case "DATA":
                    if(acceptedClient) {
                        return line;
                    } else {
                        sendErrMsg(100, "Client must join server before sending data");
                        break;
                    }
                case "IMAV":
                    lastHeartbeat = System.currentTimeMillis();
                    break;
                case "QUIT":
                    closingThread = true;
                    return "DATA " + clientName + ": - has left the chat";
                default:
                    sendErrMsg(102, "Unknown command");
            }
        }
    }

    /*
     * Checks if the username contains any illegal characters.
     * Username is only allowed to be max 12 characters long
     * and only contain letters, digits, '-' and '_'.
     *
     * @param   username
     * @return  true if username contains any illegal characters
     */
    private boolean illegalChar(String username) {
        boolean illegalChar = false;

        if(username.length() == 0) {
            sendErrMsg(200, "No input entered");
            illegalChar = true;
        }

        if(username.length() > 12) {
            sendErrMsg(201, "Username can max be 12 characters long");
            illegalChar = true;
        }

        //runs through string and checks if the char integer value is inside of allowed ancii table ranges
        for (int i = 0; i < username.length(); i++) {
            int c = (int) username.charAt(i);
            //ancii table: digits       capital-letters         small-letters       '-'      '_'
            if( !((47 < c && c < 58) || (64 < c && c < 91) || (96 < c && c < 123) || c==45 || c==95)) {
                sendErrMsg(202, "Only letters, digits, '-' and '_' allowed in username");
                illegalChar = true;
                break;
            }
        }

        return illegalChar;
    }

    private void closeThread(Timer t) {

        /*
         * Clean up. Set the current thread variable to null so that a new client
         * could be accepted by the server.
         */
        synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] == this) {
                    threads[i] = null;
                }
            }
        }

        //Collecting and sending user list when client leave the chat
        synchronized (this) {
            String userList = "";
            for (int i = 0; i < maxClientsCount ; i++) {
                if(threads[i] != null && threads[i].clientName != null) {
                    userList = userList + threads[i].clientName +" ";
                }
            }
            System.out.println("Userlist: " + userList);

            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null) {
                    threads[i].os.println("LIST " + userList);
                }
            }
        }

        try {

            sis.close();
            os.close();
            clientSocket.close();
            t.cancel();
            System.out.println("Connection closed");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
