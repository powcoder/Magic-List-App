https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import model.account.Account;
import org.junit.Before;
import org.junit.Test;

import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class SearchesControllerTest extends BaseTestController {

    private Account account;

    @Before
    public void setUp() throws Exception {
        account = login();
    }

    @Test
    public void getSearches() throws Exception {

    }

}