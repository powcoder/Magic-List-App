https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.account;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.Logger;
import play.libs.Json;
import play.mvc.Http;
import utilities.Validation;

import java.util.Map;

/**
 * A class that is used for whenever a user logs in or creates an account. This class is meant to be used for when a
 * user requests access to his/her information.
 */
public class Account {

    public static final String ACCOUNT_USER_ID = "user_id";

    public static final String ACCOUNT_NAME = "name";

    /**
     * The JSON field mapping the account's token to all GET/POST requests. Used for validation and responses.
     */
    public static final String TOKEN = "token";

    public static final String ACCOUNT_STRIPE_TOKEN = "stripe_token";

    public static final String COUPON_ID = "coupon_id";

    public static final String CARD_ID = "card_id";

    /**
     * The JSON field mapping the account's token to all GET/POST requests. Used for validation and responses.
     */
    public static final String STRIPE_PLAN_ID = "stripe_plan_id";

    public static final String STRIPE_CUSTOMER_ID = "customer_id";

    public static final String SUBSCRIPTION_ID = "subscription_id";

    public static final String IS_ADMIN = "is_admin";

    public static final String IS_MANAGER = "is_manager";

    /**
     * The JSON field that maps the account's email. Used for validation and responses.
     */
    public static final String EMAIL = "email";

    /**
     * The JSON field that maps the account's email. Used for input validation.
     */
    public static final String PASSWORD = "password";

    public static final String IS_EMAIL_VERIFIED = "is_email_verified";

    /**
     * JSON field used for the user's new password when requesting a new one.
     */
    public static final String ACCOUNT_NEW_PASSWORD = "new_password";

    public static final String ACCOUNT_FORGOT_PASSWORD_LINK = "forgot_password_link";

    public static final String ACCOUNT_VERIFY_EMAIL_LINK = "verify_email_link";

    public static final String ACCOUNT_COMPANY_NAME = "company_name";

    public static final String ACCOUNT_NOTIFICATIONS = "account_notifications";

    private static Converter converter;

    public static Account getAccountFromSession() {
        String token = Http.Context.current().session().get(Account.TOKEN);
        Map<String, Object> args = Http.Context.current().args;
        if (!Validation.isEmpty(token) && args.containsKey(token)) {
            return (Account) args.get(token);
        } else {
            return new Account();
        }
    }

    public static void removeAccountFromSession() {
        new Account().saveAccountToSession(Http.Context.current().session());
    }

    public static Converter getAccountConverter() {
        if (converter == null) {
            converter = new Converter();
        }
        return converter;
    }

    public static Account createAccountFromCreationAction(String userId, String name, String email, String token,
                                                          String stripePlanId, String companyName, boolean isManager) {
        return new Account(userId, name, email, token, null, null, stripePlanId,
                companyName, false, false, isManager, false, false);
    }

    public static Account createAccountFromDatabase(String userId, String name, String email, String token,
                                                    String stripeSubscriptionId, String customerId, String stripePlanId,
                                                    String companyName, boolean isEmailVerified, boolean isAdmin,
                                                    boolean isManager, boolean isSuperAdmin, boolean isBetaUser) {
        return new Account(userId, name, email, token, stripeSubscriptionId, customerId, stripePlanId, companyName,
                isEmailVerified, isAdmin, isManager, isSuperAdmin, isBetaUser);
    }

    private final String userId;
    private final String name;
    private final String email;
    private final String token;
    private final String customerId;
    private final String stripePlanId;
    private final String stripeSubscriptionId;
    private final String companyName;
    private final boolean isEmailVerified;
    private final boolean isAdmin;
    private boolean isManager;
    private final boolean isSuperAdmin;
    private final boolean isBetaUser;
    private AccountMetaData metaData;

    /**
     * @param email       The email of the user.
     * @param token       The access token used to verifySubscriptionAndToken this user with the server.
     * @param companyName The name of the company to which the given account belongs
     */
    private Account(String userId, String name, String email, String token, String stripeSubscriptionId,
                    String customerId, String stripePlanId, String companyName, boolean isEmailVerified, boolean isAdmin,
                    boolean isManager, boolean isSuperAdmin, boolean isBetaUser) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.token = token;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.customerId = customerId;
        this.stripePlanId = stripePlanId;
        this.companyName = companyName;
        this.isEmailVerified = isEmailVerified;
        this.isAdmin = isAdmin;
        this.isManager = isManager;
        this.isSuperAdmin = isSuperAdmin;
        this.isBetaUser = isBetaUser;
    }

    private Account() {
        this(null, null, null, null, null, null, null,
                null, false, false, false, false,
                false);
    }

    public void saveAccountToSession(Http.Session session) {
        if (!Validation.isEmpty(token)) {
            session.put(TOKEN, token);
            Http.Context.current().args.put(token, this);
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return name.split("\\s")[0];
    }

    /**
     * @return The email of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return The user's access token.
     */
    public String getToken() {
        return token;
    }

    /**
     * @return The user's/stripe customer's ID or null if the user has no subscription (it's cancelled)
     */
    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStripePlanId() {
        return stripePlanId;
    }

    /**
     * @return The name of the company to which the given account belongs
     */
    public String getCompanyName() {
        return companyName;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isSuperAdmin() {
        return isSuperAdmin;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean isManager) {
        this.isManager = isManager;
    }

    public AccountMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(AccountMetaData metaData) {
        this.metaData = metaData;
    }

    public boolean isBetaUser() {
        return isBetaUser;
    }

    @Override
    public String toString() {
        return new Converter().renderAsJsonObject(this).toString();
    }

    public static class Converter implements JsonConverter<Account> {

        private Converter() {
            // Singleton instance
        }

        @Override
        public ObjectNode renderAsJsonObject(Account object) {
            return Json.newObject()
                    .put(ACCOUNT_USER_ID, escape(object.userId))
                    .put(ACCOUNT_NAME, escape(object.name))
                    .put(EMAIL, escape(object.email))
                    .put(TOKEN, escape(object.token))
                    .put(ACCOUNT_COMPANY_NAME, escape(object.companyName))
                    .put(IS_EMAIL_VERIFIED, object.isEmailVerified)
                    .put(IS_MANAGER, object.isManager)
                    .put(IS_ADMIN, object.isAdmin);
        }

        @Override
        public Account deserializeFromJson(ObjectNode objectNode) {
            String userId = objectNode.get(ACCOUNT_USER_ID).asText();
            String name = objectNode.get(ACCOUNT_NAME).asText();
            String email = objectNode.get(EMAIL).asText();
            String token = objectNode.get(TOKEN).asText();
            Logger.error("Account.Converter: Attempted to create account using invalid constructor");
            return new Account(userId, name, email, token, null, null, null,
                    null, false, false, false, false, false);
        }
    }

}
