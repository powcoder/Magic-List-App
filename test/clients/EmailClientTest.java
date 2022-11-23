https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import controllers.BaseTestController;
import model.user.User;
import org.junit.Before;
import org.junit.Test;
import com.typesafe.config.Config;
import play.Environment;
import play.libs.ws.WSClient;
import utilities.ListUtility;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class EmailClientTest extends BaseTestController {

    private WSClient client;
    private Config config;
    private Environment environment;

    @Before
    public void setup() {
        client = app.injector().instanceOf(WSClient.class);
        config = app.injector().instanceOf(Config.class);
        environment = app.injector().instanceOf(Environment.class);
    }

    @Test
    public void sendAdminEmailForCreateAccount() throws Exception {
        String name = "Corey Caplan";
        String email = "coreycaplan3@gmail.com";
        String companyName = "AXA Advisors";

        List<User> administratorEmails = ListUtility.asList(
                new User(null, "coreycaplan3@gmail.com", "Magic List Team", -1),
                new User(null, "cdc218@lehigh.edu", "Corey Caplan", -1)
        );

        boolean result = new EmailClient(client)
                .sendEmailForCreateAccountToAdmin(name, email, companyName, administratorEmails)
                .toCompletableFuture()
                .get();
        assertTrue(result);
    }

    @Test
    public void sendEmailForCreateAccount() throws Exception {
        String name = "Daniel Bead";
        String email = "web-8ifxe@mail-tester.com";
//        String email = "electricstapler3@hotmail.com";
        String verifyEmailLink = "ver_bc123";

        boolean result = new EmailClient(client)
                .sendEmailForCreateAccount(name, email, verifyEmailLink)
                .toCompletableFuture()
                .get();
        assertTrue(result);
    }

    @Test
    public void sendEmailForResetPassword() throws Exception {
        String name = "Dirty Daniel";
        String email = "coreycaplan3@gmail.com";
        String token = "abc123";
        String userId = "def123";

        boolean result = new EmailClient(client)
                .sendEmailForResetPassword(name, email, token, userId)
                .toCompletableFuture()
                .get();
        assertTrue(result);
    }

}