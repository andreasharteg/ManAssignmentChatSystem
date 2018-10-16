import junit.framework.TestCase;

public class TestServerClientThread extends TestCase{

    public void testIllegalCharMethod() {
        System.out.println(1);
        assertEquals(illegalChar("12charname12"), false);
        System.out.println(2);
        assertEquals(illegalChar("name11-_"), false);
        System.out.println(3);
        assertEquals(illegalChar("14charname1111"), true);
        System.out.println(4);
        assertEquals(illegalChar("illegal*@"), true);
        System.out.println(5);
        assertEquals(illegalChar("tooLongWithIllegal)?#"), true);
    }

    /*
     * Checks if the username contains any illegal characters.
     * Username is only allowed to be max 12 characters long
     * and only contain letters, digits, '-' and '_'.
     *
     * @param   username
     * @return  true if username contains any illegal characters
     */
    public boolean illegalChar(String username) {
        boolean illegalChar = false;

        if(username.length() > 12) {
            System.out.println("* Use max 12 characters");
            illegalChar = true;
        }

        //runs through string and checks if the char integer value is NOT inside of allowed ancii table ranges
        for (int i = 0; i < username.length(); i++) {
            int c = (int) username.charAt(i);
            //ancii table: digits       capital-letters         small-letters       '-'      '_'
            if( !((47 < c && c < 58) || (64 < c && c < 91) || (96 < c && c < 123) || c==45 || c==95)) {
                System.out.println("* Only letters, digits, '-' and '_' allowed");
                illegalChar = true;
                break;
            }
        }

        return illegalChar;
    }
}