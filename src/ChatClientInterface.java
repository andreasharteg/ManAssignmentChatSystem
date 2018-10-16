public interface ChatClientInterface {

    String sendMessage();

    void printClientAccepted(boolean accepted);
    void printErrorMessage(String err_msg);
    void printChatMessage(String chat_msg);
    void printUserList(String[] userList);    //String array of user names
    void printDefaultMsg(String defaultMsg);
}