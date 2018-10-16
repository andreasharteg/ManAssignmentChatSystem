import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientMainFrame extends JFrame implements ChatClientInterface{

    //Components' instance variables
    private JTextField usernameInput;
    private JButton loginBtn;
    private JButton logoutBtn;
    private JTextArea clientMsgIn;
    private JList<String> userList;
    private JTextArea clientMsgOut;
    private JButton sendBtn;

    private Object syncObject = new Object();   //Object for sync hronizing threads

    private String[] userlist = {"- no users"};
    private int activeUsers = 0;
    private boolean clientAccepted = false;
    private String clientMsg = "";

    public ClientMainFrame() {
        super("Chat Client");

        //Set layout manager
        setLayout(new BorderLayout());

        //Create Swing Panels
        JPanel upperPanel = new JPanel();
        JPanel middlePanel = new JPanel();
        JPanel lowerPanel = new JPanel();

        //Add panels to content pane
        Container c = getContentPane();
        c.add(upperPanel, BorderLayout.NORTH);
        c.add(middlePanel, BorderLayout.CENTER);
        c.add(lowerPanel, BorderLayout.SOUTH);

        //Create components and set their default states
        usernameInput = new JTextField(14);
        loginBtn = new JButton("Login");
        logoutBtn = new JButton("Logout");
        logoutBtn.setEnabled(false);
        clientMsgIn = new JTextArea(18, 24);
        clientMsgIn.setLineWrap(true);
        clientMsgIn.setEditable(false);
        userList = new JList<String>(userlist);
        userList.setBorder(BorderFactory.createTitledBorder("Users online: " + activeUsers));
        userList.setFixedCellWidth(110);
        clientMsgOut = new JTextArea(3, 24);
        clientMsgOut.setLineWrap(true);
        clientMsgOut.setEditable(false);
        sendBtn = new JButton("Send");
        sendBtn.setEnabled(false);

        //Add components to respective panels
        upperPanel.add(usernameInput);
        upperPanel.add(loginBtn);
        upperPanel.add(logoutBtn);
        middlePanel.add(new JScrollPane(clientMsgIn));
        middlePanel.add(userList);
        lowerPanel.add(clientMsgOut);
        lowerPanel.add(sendBtn);

        //Add action listeners
        /* Login button */
        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (syncObject) {
                    clientMsg = usernameInput.getText();
                    syncObject.notify();
                }
            }
        });

        /* Logout button */
        logoutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (syncObject) {
                    clientMsg = "QUIT";
                    syncObject.notify();
                    loginBtn.setEnabled(true);
                    logoutBtn.setEnabled(false);

                }
            }
        });

        /* Send button */
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (syncObject) {
                    clientMsg = clientMsgOut.getText();
                    clientMsgOut.setText("");
                    syncObject.notify();
                }
            }
        });

        pack();
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public String sendMessage() {
        String sendMsg = "";

        /*
         * ved ikke helt hvordan det her virker...
         * Thread waits for actionPerformed on sendButton
         * when user presses send button clientMsg string
         * is set and syncObject is notified
         * Then sendMsg = clientMsg
         * and sendMsg is returned
         */

        try {
            synchronized (syncObject) {
                while (clientMsg.length() == 0) {
                    syncObject.wait();
                }
                sendMsg = clientMsg;
                clientMsg = "";
                return sendMsg;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "interruptedException...";
        }
    }

    @Override
    public void printClientAccepted(boolean accepted) {
        clientAccepted = accepted;
        clientMsgOut.setEditable(true);
        clientMsgOut.requestFocus();
        sendBtn.setEnabled(true);
        logoutBtn.setEnabled(true);
        loginBtn.setEnabled(false);

        clientMsgIn.append("\n" + accepted);
    }

    @Override
    public void printErrorMessage(String err_msg) {
        clientMsgIn.append("\n" + err_msg);
    }

    @Override
    public void printChatMessage(String chat_msg) {
        clientMsgIn.append("\n" + chat_msg);
    }

    @Override
    public void printUserList(String[] userlist) {
        if(userlist.length == 0) {
            userlist[0] = "- no users";
            userList.setListData(userlist);
            activeUsers = 0;
            userList.setBorder(BorderFactory.createTitledBorder("Users online: " + activeUsers));
        } else {
            userList.setListData(userlist);
            activeUsers = userlist.length;
            userList.setBorder(BorderFactory.createTitledBorder("Users online: " + activeUsers));
        }
    }

    @Override
    public void printDefaultMsg(String defaultMsg) {
        clientMsgIn.append("\n" + defaultMsg);
    }
}