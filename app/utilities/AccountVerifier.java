https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import clients.StripeClient;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import database.AccountDBAccessor;
import database.PlanDBAccessor;
import model.account.Account;
import model.VerificationResult;
import model.account.AccountMetaData;
import com.typesafe.config.Config;
import play.Logger;
import play.db.Database;
import play.mvc.Http;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Map;

/**
 *
 */
public class AccountVerifier {

    public static final String KEY_IS_SESSION_EXPIRED = "is_session_expired";
    private static final String KEY_PREVIOUS_TIME = "previous_time";

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final AccountDBAccessor accountDBAccessor;
    private final PlanDBAccessor planDBAccessor;
    private final Config config;
    private final StripeClient stripeClient;
    private Account account;

    @Inject
    public AccountVerifier(Database database, Config config) {
        accountDBAccessor = new AccountDBAccessor(database);
        planDBAccessor = new PlanDBAccessor(database);
        this.config = config;
        stripeClient = new StripeClient();
    }

    public Account getAccount() {
        return account;
    }

    public VerificationResult isUserSuperAdmin(String token) {
        if (Validation.isEmpty(token)) {
            return VerificationResult.INVALID_TOKEN;
        }
        Account account = accountDBAccessor.getAccountFromToken(token);
        if (account == null) {
            return VerificationResult.INVALID_TOKEN;
        } else if (account.isSuperAdmin()) {
            return VerificationResult.SUCCESS;
        } else {
            return VerificationResult.NOT_AUTHORIZED;
        }
    }

    /**
     * Checks if the user for the given request is valid. This method should be called by the by methods that are
     * invoked by the Java client, since this method checks for extra things like a verified email and paid
     * subscription.
     *
     * @param requestHeaders The request's headers sent as part of the request, which holds the user's credentials.
     * @return True if the user is valid or false if he/she is not.
     */
    public VerificationResult isUserValidForPaidFunctionalityFromImporter(Map<String, ?> requestHeaders, Http.Session session) {
        String token = Validation.string(BaseController.AUTHORIZATION, requestHeaders);
        return isUserValidForPaidFunctionality(token, true, session);
    }

    /**
     * Checks if the user for the given request is valid. This method should be called by the by methods that are
     * invoked by the Java client, since this method checks for extra things like a verified email and paid
     * subscription.
     *
     * @param node The JSON sent as part of the request, which holds the user's credentials.
     * @return True if the user is valid or false if he/she is not.
     */
    public VerificationResult isUserValidForPaidFunctionality(JsonNode node, Http.Session session) {
        String token = Validation.string(Account.TOKEN, node);
        return isUserValidForPaidFunctionality(token, false, session);
    }

    public VerificationResult isUserValidForPaidFunctionality(String token, boolean isImporter, Http.Session session) {
        VerificationResult result = isUserValidForStandardFunctionality(token, isImporter, session);
        if (result != VerificationResult.SUCCESS) {
            return result;
        }

        if (account.isAdmin()) {
            // The user is an admin, default to returning true
            return VerificationResult.SUCCESS;
        }

        if (!account.isEmailVerified()) {
            logger.debug("Email not verified for account: {}", account.getUserId());
            return VerificationResult.EMAIL_NOT_VERIFIED;
        }

        String subscriptionId = account.getStripeSubscriptionId();
        if (subscriptionId == null) {
            logger.debug("No subscription found for account: {}", account.getUserId());
            return VerificationResult.NO_SUBSCRIPTION;
        }

        if (!accountDBAccessor.getAccountMetaData(account)) {
            logger.debug("Could not get account meta data for account: {}", account.getUserId());
            return VerificationResult.SERVER_ERROR;
        }

        AccountMetaData metaData = account.getMetaData();
        String status = metaData.getSubscriptionStatus();
        if (status == null) {
            status = stripeClient.getSubscriptionStatus(subscriptionId);
            if (status == null) {
                logger.debug("No subscription status found for account: {}", account.getUserId());
                return VerificationResult.NO_SUBSCRIPTION;
            } else if (!planDBAccessor.updateUserPlanId(account.getCustomerId(), account.getStripePlanId(), subscriptionId, status)) {
                String error = String.format("Could not update plan for [customer: %s, subscription: %s]",
                        account.getCustomerId(), subscriptionId);
                logger.error(error, new IllegalStateException());
                return VerificationResult.SERVER_ERROR;
            }
        }

        if (!StripeClient.isSubscriptionActive(status)) {
            logger.debug("No active subscription for account: {}; status: {}", account.getUserId(),
                    metaData.getSubscriptionStatus());
            return VerificationResult.NO_SUBSCRIPTION;
        }

        return VerificationResult.SUCCESS;
    }

    /**
     * @return Success if the user is logged in, the session didn't time out and is a beta user (if this server is a
     * beta server). Does <b>not</b> check if the user has a verified email
     */
    public VerificationResult isUserValidForStandardFunctionality(Http.Session session) {
        String token = session.get(Account.TOKEN);
        return isUserValidForStandardFunctionality(token, false, session);
    }

    // Private Methods

    private VerificationResult isUserValidForStandardFunctionality(String token, boolean isImporter, Http.Session session) {
        if (Validation.isEmpty(token)) {
            return VerificationResult.INVALID_TOKEN;
        }
        account = accountDBAccessor.getAccountFromToken(token);
        if (account == null) {
            return VerificationResult.INVALID_TOKEN;
        }

        if(!isImporter) {
            // The user is verified, let's make sure he/she didn't timeout
            long timeoutSeconds = BaseController.getLong("sessionTimeout", 3600L, config);
            long previousTime = Validation.getLong(KEY_PREVIOUS_TIME, session);
            long currentTime = Calendar.getInstance().getTimeInMillis();

            if (previousTime != -1L && currentTime - previousTime > timeoutSeconds * 1000L) {
                session.put(KEY_IS_SESSION_EXPIRED, String.valueOf(true));
                return VerificationResult.INVALID_TOKEN;
            } else {
                session.put(KEY_PREVIOUS_TIME, String.valueOf(currentTime));
                session.remove(KEY_IS_SESSION_EXPIRED);
            }
        }

        if (BaseController.isBeta() && !account.isBetaUser()) {
            return VerificationResult.NOT_BETA_USER;
        }

        return VerificationResult.SUCCESS;
    }

}
