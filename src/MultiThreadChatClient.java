import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/* @Author Andreas Harteg
 * Starting chat client and implementing console user interface
 */
public class MultiThreadChatClient implements ChatClientInterface {

    private static BufferedReader inputLine = null;
    private static String host;
    private static int portNumber;

    public static void main(String[] args) {

        host = "localhost";
        portNumber = 2222;

        if (args.length == 2) {
            host = args[0];
            portNumber = Integer.valueOf(args[1]).intValue();
            System.out.println("Now using host: " + host + " with port number=" + portNumber);
        } else {
            System.out.println("Usage: java MultiThreadChatClient <host> <portNumber>\n"
                    + "Now using host: " + host + " with port number=" + portNumber);
        }


        inputLine = new BufferedReader(new InputStreamReader(System.in));
        ChatClientInterface chatClientInterface = new MultiThreadChatClient();
        ChatClientInterface chatClientInterfaceGUI = new ClientMainFrame();
        new Thread(new OutputThreadChatClient(host, 2222, chatClientInterfaceGUI)).start();

    }

    //Wait for and send client keyboard input
    @Override
    public String sendMessage() {
        try {
            return inputLine.readLine().trim();
        } catch (Exception e) {
            System.out.println(e);
        }
        return "sendMessage() exception";
    }



    //Print is client accepted
    @Override
    public void printClientAccepted(boolean accepted) {
        if (accepted) {
            System.out.println("Client accepted");
        } else {
            System.out.println("Client not accepted");
        }
    }

    //Print error message
    @Override
    public void printErrorMessage(String err_msg) {
        System.out.println("Err " + err_msg);
    }

    //Print data message
    @Override
    public void printChatMessage(String chat_msg) {
        System.out.println(chat_msg);
    }

    //Print user list
    @Override
    public void printUserList(String[] userList) {
        String userListString = "";
        for (int i = 0; i < userList.length; i++) {
            userListString = userListString + userList[i] + " ";
        }
        System.out.println("List of users: " + userListString);
    }

    //Print input without protocol tag
    @Override
    public void printDefaultMsg(String defaultMsg) {
        System.out.println(defaultMsg);
    }
}