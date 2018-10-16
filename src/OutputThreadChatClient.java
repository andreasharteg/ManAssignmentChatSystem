import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
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

public class OutputThreadChatClient implements Runnable {

    private static String clientName        = null;
    private static Socket clientSocket      = null;
    private static PrintStream os           = null;
    private String host                     = null;
    private int portNumber;

    private Timer timer;
    ChatClientInterface chatClientInterface;
    private InputThreadChatClient inputThreadChatClient;

    private static boolean acceptedOnServer = false;
    private static boolean closingThread = false;
    private static boolean outputClosed = false;

    public OutputThreadChatClient(String host, int portNumber, ChatClientInterface chatClientInterface) {
        this.host = host;
        this.portNumber = portNumber;
        this.chatClientInterface = chatClientInterface;

        try {
            clientSocket = new Socket(host, portNumber);
            os = new PrintStream(clientSocket.getOutputStream());

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    os.println("IMAV"); // Sending heartbeat
                }
            }, 2000, 1000);

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //Main output thread loop
    @Override
    public void run() {
        if(clientSocket != null && os != null) {

            inputThreadChatClient = new InputThreadChatClient(clientSocket, chatClientInterface);
            new Thread(inputThreadChatClient).start();

            while (!closingThread) {
                os.println(outputProtocolFormatter(chatClientInterface.sendMessage()));
            }
            os.close();
            outputClosed = true;
        }
    }

    private String outputProtocolFormatter(String clientOutput) {

        if (clientOutput.equals("QUIT")) {
            //Setting input to close when next main loop completes
            inputThreadChatClient.setClosingThread(true);
            //Canceling timer before setting loop to close, so the last message sent is "QUIT" and not "IMAV"
            timer.cancel();
            //Setting output loop to close when next main loop completes
            closingThread = true;
            return "QUIT";
        }
        if (!acceptedOnServer) {
            return "JOIN " + clientOutput + ", " + host + ":" + portNumber;
        } else {
            return "DATA " + clientName + ": " + clientOutput;
        }
    }

    public static void setClientName(String clientName) {
        OutputThreadChatClient.clientName = clientName;
    }

    public static void setAcceptedOnServer(boolean acceptedOnServer) {
        OutputThreadChatClient.acceptedOnServer = acceptedOnServer;
    }

    //Closing socket when both input and output streams has closed
    public static void closeSocket() {
        while (true) {
            if (outputClosed) {
                try {
                    clientSocket.close();
                    break;
                } catch (IOException e) {
                    System.out.println(e);
                    break;
                }
            }
        }
    }
}

class InputThreadChatClient implements Runnable {

    private String clientName        = null;
    private Socket clientSocket      = null;
    private Scanner sis              = null;
    private boolean closingThread    = false;

    ChatClientInterface chatClientInterface;

    public InputThreadChatClient(Socket clientSocket, ChatClientInterface chatClientInterface) {
        this.clientSocket = clientSocket;
        this.chatClientInterface = chatClientInterface;

        try {
            sis = new Scanner(clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //Main input thread loop
    @Override
    public void run() {

        while (!closingThread) {
            inputProtocolFilter();
        }

        //Closing input thread
        sis.close();
        //Closing client socket
        OutputThreadChatClient.closeSocket();
        System.out.println("Connection closed");

    }

    private void inputProtocolFilter() {

        String line;
        String command;
        String message;

        while(!closingThread) {
            line = sis.nextLine();
            command = line.substring(0,4);

            if (line.length() > 4) {
                message = line.substring(5);
            } else {
                message = "";
            }

            switch (command) {
                case "J_OK":
                    //Client thread saves username returned from server
                    OutputThreadChatClient.setAcceptedOnServer(true);
                    clientName = message;
                    OutputThreadChatClient.setClientName(message);
                    chatClientInterface.printClientAccepted(true);
                    break;
                case "J_ER":
                    chatClientInterface.printErrorMessage(message);
                    break;
                case "DATA":
                    chatClientInterface.printChatMessage(message);
                    break;
                case "LIST":
                    String[] userlist = message.split("\\s+");
                    chatClientInterface.printUserList(userlist);
                    break;
                case "QUIT":
                    //Setting input to close when next main loop completes
                    closingThread = true;
                    break;
                default:
                    chatClientInterface.printDefaultMsg(line);

            }
        }
    }

    public void setClosingThread(boolean closingThread) {
        this.closingThread = closingThread;
    }
}