https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordEncryptionTest {

    private static final String PASSWORD = "renelovescorey";

    @Test
    public void createHash() throws Exception {
        String hash = PasswordEncryption.createHash(PASSWORD.toCharArray());
        System.out.println("hash = " + hash);

        assertTrue(PasswordEncryption.verifyPassword(PASSWORD.toCharArray(), hash));
    }

}