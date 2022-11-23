https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.account.Account;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.*;

/**
 *
 */
public class UserApiControllerTest extends BaseTestController {

    private static String URL_TO_GET_CUSTOMER_TOKEN = "https://stripe.com/docs/custom-form";

    @Before
    public void Setup() {

    }

    @Test
    public void testCreateAccount() {
        String name = "Corey Caplan";
        String email = "developer.coreycaplan@gmail.com";
        String password = "dopey1019";
        String token = "tok_19gr1BEhPVxm6CsD4GwiKKO2";
        ObjectNode node = Json.newObject()
                .put(Account.TOKEN, token)
                .put(Account.ACCOUNT_NAME, name)
                .put(Account.EMAIL, email)
                .put(Account.PASSWORD, password);

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/api/user/create")
                .bodyJson(node);

        Result result = route(app, requestBuilder);

        System.out.println(contentAsString(result));

        assertEquals(200, result.status());
    }

    @Test
    public void testLogin() {
        String email = "developer.coreycaplan@gmail.com";
        String password = "dopey1019";
        ObjectNode node = Json.newObject()
                .put(Account.EMAIL, email)
                .put(Account.PASSWORD, password);

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/api/user/login")
                .bodyJson(node);

        Result result = route(app, requestBuilder);

        System.out.println(contentAsString(result));

        assertEquals(200, result.status());
    }

}
