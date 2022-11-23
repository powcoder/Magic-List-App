https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import clients.StripeClient;
import com.stripe.Stripe;
import database.AccountDBAccessor;
import database.AdminDBAccessor;
import model.account.AccountMetaData;
import model.user.User;
import org.junit.Before;
import org.junit.Test;
import play.db.Database;
import play.test.WithApplication;

import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;

/**
 *
 */
public class StripeClientTest extends WithApplication {

    private Database database;

    @Before
    public void setup() {
        database = app.injector().instanceOf(Database.class);
    }

    @Test
    public void isStatusValid() throws Exception {
    }

    @Test
    public void isSubscriptionActive() throws Exception {
    }

    @Test
    public void cancelAllCancelledSubscriptions() throws Exception {
        StripeClient client = new StripeClient();
        AccountDBAccessor accountDBAccessor = new AccountDBAccessor(database);
        new AdminDBAccessor(database)
                .getAllAccounts("usr_CI9451qinsCIa2MHx6tpOn90k7EM9Kew")
                .forEach(account -> {
                    accountDBAccessor.getAccountMetaData(account);
                    if (StripeClient.STATUS_CANCELED.equalsIgnoreCase(account.getMetaData().getSubscriptionStatus())) {
                        try {
                            client.getCustomerById(account.getCustomerId()).cancelSubscription();
                        } catch (Exception e) {
                            System.out.println("Found exception: " + e.getMessage());
                        }
                    }
                });
    }

    @Test
    public void cancelAllSubscriptions() throws Exception {
        StripeClient client = new StripeClient();
        client.database = database;
        assertTrue(client.cancelAllSubscriptions());
    }

    @Test
    public void moveAllSubscriptions() throws Exception {
        StripeClient client = new StripeClient();
        boolean isSuccessful = client.moveAllSubscriptions("magic_list_monthly_temporary", "plan_Ee05CCI4rrKmZI");
//        boolean isSuccessful = client.moveAllSubscriptions("plan_Ee05CCI4rrKmZI", "plan_Ee05CCI4rrKmZI");
        assertTrue(isSuccessful);
    }

    @Test
    public void deleteCustomer() throws Exception {
    }

    @Test
    public void getCustomerById() throws Exception {
    }

    @Test
    public void verifyHasPaymentMethod() throws Exception {
    }

    @Test
    public void getSubscriptionStatus() throws Exception {
    }

    @Test
    public void getMostRecentInvoice() throws Exception {
    }

    @Test
    public void getInvoiceById() throws Exception {
    }

    @Test
    public void getInvoices() throws Exception {
    }

    @Test
    public void payMostRecentInvoice() throws Exception {
    }

    @Test
    public void getMostRecentChargeFromMostRecentInvoice() throws Exception {
    }

    @Test
    public void cancelSubscription() throws Exception {
    }

    @Test
    public void doesCreditCardExist() throws Exception {
    }

    @Test
    public void createCreditCard() throws Exception {
    }

    @Test
    public void deleteCreditCard() throws Exception {
    }

    @Test
    public void getCreditCards() throws Exception {
    }

    @Test
    public void changeDefaultCreditCard() throws Exception {
    }

    @Test
    public void chargeCardForPersonList() throws Exception {
    }

    @Test
    public void getCouponById() throws Exception {
    }

    @Test
    public void createCharge() throws Exception {

    }

    @Test
    public void createCustomer() throws Exception {

    }

    @Test
    public void createSubscription() throws Exception {

    }

}