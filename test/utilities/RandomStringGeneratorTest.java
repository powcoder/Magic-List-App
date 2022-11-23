https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class RandomStringGeneratorTest {

    @Test
    public void testGetNextRandomUserId() throws Exception{
        String userId = RandomStringGenerator.getInstance().getNextRandomUserId();
        System.out.println("userId = " + userId);
        assertTrue(userId.substring(0, 3).equals("usr"));
    }

    @Test
    public void templateTest() throws Exception {
        String s = "<p>Best regards,</p>\n" +
                " \n" +
                " <p>Corey Caplan</p>\n" +
                " ";
    }

}