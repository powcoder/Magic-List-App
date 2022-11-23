https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import actions.CreateAccountAction;
import actions.ReCaptchaAction;
import clients.EmailClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Coupon;
import database.*;
import global.authentication.BaseAccountAuthenticator;
import global.authentication.InvalidEmailAuthenticator;
import global.authentication.LogoutAuthenticator;
import model.*;
import model.account.Account;
import model.lists.ProspectSearch;
import model.profile.HomePageAgenda;
import model.dialsheet.HomePageDialSheet;
import model.profile.ProfileAlert;
import model.prospect.Appointment;
import model.prospect.Notification;
import model.prospect.Prospect;
import model.server.VersionChanges;
import model.stripe.Plan;
import model.user.User;
import play.Logger;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.Json;
import play.mvc.*;
import clients.StripeClient;
import utilities.*;
import views.html.misc.*;
import views.html.failure.*;
import views.html.user.*;

import javax.inject.Inject;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static model.account.Account.*;
import static play.mvc.Http.Context.current;
import static utilities.AccountVerifier.KEY_IS_SESSION_EXPIRED;

/**
 *
 */
public class UserController extends BaseController {

    private static final String KEY_REAL_EMAIL_URL = "https://realemail.expeditedaddons.com";
    private static final String KEY_REAL_EMAIL_API_KEY = "REALEMAIL_API_KEY";
    private static final String KEY_LOGIN_ATTEMPTS = "login_attempts";
    private static final String KEY_LOCKED = "locked";

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int FIFTEEN_MINUTES_SECONDS = 60 * 15;
    private static final int TEN_MINUTES_MILLIS = 1000 * 60 * 10;
    private static final int TEN_MINUTES_SECONDS = 60 * 10;

    private final AccountDBAccessor accountDBAccessor;
    private final StripeClient stripeClient;
    private final EmailClient emailClient;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public UserController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        accountDBAccessor = new AccountDBAccessor(getDatabase());
        stripeClient = new StripeClient();
        emailClient = new EmailClient(controllerComponents.getWsClient());
    }

    public Result getRedirectToRegister() {
        return redirect(routes.UserController.getCreateAccountPage());
    }

    public Result getCreateAccountPage() {
        List<Plan> planList = new PlanDBAccessor(getDatabase()).getPlansFromDatabase(false);
        if (planList != null) {
            StripeClient stripeClient = new StripeClient();
            return ok(CreateAccountPage.render(planList, stripeClient.STRIPE_API_KEY_PUBLIC));
        } else {
            return internalServerError(FailurePage.render("An unknown error occurred"));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @RequireCSRFCheck
    public CompletionStage<Result> isEmailValid() {
        JsonNode node = request().body().asJson();
        String email = node.get(Account.EMAIL).asText();

        final String emailRegex = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        if (Validation.isEmpty(email)) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForMissingField(Account.EMAIL)));
        }

        email = email.toLowerCase();
        if (!email.matches(emailRegex)) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(Account.EMAIL)));
        }

        return getWsClient().url(KEY_REAL_EMAIL_URL)
                .addQueryParameter("api_key", BaseController.getString(KEY_REAL_EMAIL_API_KEY))
                .addQueryParameter("email", email)
                .get()
                .thenApply(response -> {
                    if (response.getStatus() == 200) {
                        if (Json.parse(response.getBody()).get("valid").asBoolean()) {
                            return ok(ResultUtility.getNodeForBooleanResponse(true));
                        } else {
                            return ok(ResultUtility.getNodeForBooleanResponse(false));
                        }
                    } else {
                        logger.error("Error received from RealEmail: [status: {}, body: {}]",
                                response.getStatus(), response.getBody());
                        String error = "Error received from email validator";
                        return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
                    }
                });
    }

    public Result getEmailAlreadyInUse() {
        String email = Validation.string(EMAIL, request().queryString());
        if (accountDBAccessor.isEmailAlreadyInDatabase(email)) {
            String reason = "This email is already in use";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        }
    }

    /**
     * The route used to allow the user to create an account (POST)
     */
    @With({CreateAccountAction.class, ReCaptchaAction.class})
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public CompletionStage<Result> createAccount() {
        Account account = (Account) current().args.get(CreateAccountAction.KEY_ACCOUNT);
        String password = (String) current().args.get(CreateAccountAction.KEY_PASSWORD);
        String stripeToken = (String) current().args.get(CreateAccountAction.KEY_STRIPE_TOKEN);
        String couponId = (String) current().args.get(CreateAccountAction.KEY_COUPON);
        String verifyEmailLink = RandomStringGenerator.getInstance().getNextRandomEmailVerifier();

        String customerId;
        try {
            customerId = stripeClient.createCustomer(account.getEmail(), stripeToken);
        } catch (CardException e) {
            Logger.error("Card exception thrown: ", e);
            current().flash().put(KEY_ERROR, e.getMessage());
            return completedFuture(redirect(routes.UserController.getCreateAccountPage()));
        } catch (StripeException e) {
            e.printStackTrace();
            return completedFuture(redirect(routes.UserController.getCreateAccountPage()));
        }

        List<User> administrators = new AdminDBAccessor(getDatabase()).getAdministratorAccounts();
        if (administrators == null) {
            String error = "There was a server error that prevented you from creating an account. Note: your " +
                    "subscription was not yet activated. Please submit a bug report";
            return completedFuture(getRedirectFailure(error));
        }

        account = accountDBAccessor.createAccount(account, password, customerId, couponId, verifyEmailLink);

        if (account != null) {
            CompletionStage<Boolean> adminEmailStage = emailClient.sendEmailForCreateAccountToAdmin(account.getName(),
                    account.getEmail(), account.getCompanyName(), administrators);
            CompletionStage<Boolean> accountEmailStage = emailClient.sendEmailForCreateAccount(account.getName(),
                    account.getEmail(), verifyEmailLink);
            // Send the user the "welcome" email
            return adminEmailStage.thenCombineAsync(accountEmailStage, (result1, result2) -> {
                if (!result1 || !result2) {
                    logger.error("Error sending emails to either account or admin", new IllegalStateException());
                    String reason = "Your account has been created but we could not send a verification email. " +
                            "Please login and retry sending it";
                    return getRedirectFailure(reason);
                } else {
                    String text = "Your account has been created. Please check your email to verify and activate " +
                            "your account!";
                    return getRedirectSuccess(text);
                }
            }, getHttpExecutionContext().current());
        } else {
            boolean isSuccessful = stripeClient.deleteCustomer(customerId);
            if (!isSuccessful) {
                logger.error("There was an error deleting the new customer: ", new IllegalStateException());
            }

            String error = "There was a server error that prevented you from creating an account. Your subscription " +
                    "was not yet activated. Please submit a bug report";
            return completedFuture(getRedirectFailure(error));
        }
    }

    public Result getLoginPage() {
        AccountVerifier accountVerifier = new AccountVerifier(getDatabase(), getConfig());
        VerificationResult verificationResult = accountVerifier.isUserValidForStandardFunctionality(session());
        if (verificationResult == VerificationResult.SUCCESS) {
            return redirect(routes.UserController.getProfilePage());
        }

        boolean isEmailNowVerified = flash(IS_EMAIL_VERIFIED) != null;
        String error = flash(KEY_ERROR);
        return ok(LoginPage.render(error, isEmailNowVerified));
    }

    /**
     * The route used to allow the user to login (POST)
     */
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result submitLogin() {
        Map<String, String[]> map = request().body().asFormUrlEncoded();
        String email = Validation.string(Account.EMAIL, map);
        String password = Validation.string(Account.PASSWORD, map);

        // TODO add user lockouts for incorrect credentials
        if (Validation.isEmpty(email)) {
            return badRequest(ResultUtility.getNodeForMissingField(Account.EMAIL));
        } else {
            email = email.toLowerCase();
        }
        if (password == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Account.PASSWORD));
        }

        try {
            Account account = accountDBAccessor.loginToAccount(email, password, true);
            if (account == null) {
                String error = "Invalid email or password.";
                return unauthorized(LoginPage.render(error, false));
            } else {
                session().clear();
                account.saveAccountToSession(session());
                return redirect(routes.UserController.getProfilePage());
            }
        } catch (SQLException e) {
            Logger.error("Error logging in: ", e);
            String reason = "There was an error logging in. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getProfilePage() {
        Account account = Account.getAccountFromSession();

        boolean isSuccessful = accountDBAccessor.getAccountMetaData(account);
        if (!isSuccessful) {
            String error = "There was an error retrieving your account\'s information. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        VersionChanges version = new AccountSettingsDBAccessor(getDatabase())
                .getDidDismissVersionChanges(account.getUserId());
        if (version == null) {
            String reason = "Could not load versioning information information";
            return internalServerError(FailurePage.render(reason));
        }

        VerificationResult result = new AccountVerifier(getDatabase(), getConfig())
                .isUserValidForPaidFunctionality(account.getToken(), false, session());

        if (result != VerificationResult.SUCCESS) {
            return getProfilePageForNoPaidSubscription(account, version);
        }

        int activeNotificationsCount = new NotificationDBAccessor(getDatabase())
                .getNotificationCount(account.getUserId());

        if (activeNotificationsCount == -1) {
            String reason = "Could not load profile information";
            return internalServerError(FailurePage.render(reason));
        } else {
            session().put(NotificationsController.KEY_NUMBER_OF_NOTIFICATIONS, String.valueOf(activeNotificationsCount));
        }

        PagedList<Prospect> migrationList = new ProspectDBAccessor(getDatabase())
                .getPeopleForMigrations(account.getUserId(), ProspectSearch.Criteria.PERSON_NAME, true, 1);
        if (migrationList == null) {
            String reason = "Could not load contact status migration information. Please submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        HomePageDialSheet homePageDialSheet = new DialSheetDBAccessor(getDatabase())
                .getHomePageDialSheet(account.getUserId());
        if (homePageDialSheet == null) {
            String reason = "Could not load profile information";
            return internalServerError(FailurePage.render(reason));
        }

        AppointmentDBAccessor appointmentDBAccessor = new AppointmentDBAccessor(getDatabase());
        NotificationDBAccessor notificationDBAccessor = new NotificationDBAccessor(getDatabase());

        Date currentDate = new Date();

        PagedList<Appointment> appointments = appointmentDBAccessor.getAppointmentsForToday(account.getUserId(), currentDate,
                1, Appointment.Sorter.APPOINTMENT_DATE, true);

        ProfileAlert alert = ProfileUtility.getProfileAlert();

        PagedList<Notification> notifications = notificationDBAccessor.getNotificationsForToday(account.getUserId(),
                currentDate, Notification.Sorter.NOTIFICATION_DATE, true, 1);

        HomePageAgenda homePageAgenda = new HomePageAgenda(alert, appointments, notifications);

        String dateForUi = DateFormat.getDateInstance(DateFormat.MEDIUM).format(currentDate);

        return ok(ProfilePage.render(account, true, dateForUi, homePageDialSheet, homePageAgenda, version, migrationList));
    }

    @Security.Authenticated(InvalidEmailAuthenticator.class)
    public Result getSendVerificationEmailPage() {
        Account account = Account.getAccountFromSession();
        if (account.isEmailVerified()) {
            return redirect(routes.UserController.getProfilePage());
        } else {
            return ok(SendVerificationEmailPage.render());
        }
    }

    @Security.Authenticated(InvalidEmailAuthenticator.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public CompletionStage<Result> sendVerificationEmail() {
        Account account = Account.getAccountFromSession();

        if (account.isEmailVerified()) {
            String reason = "Your email is already verified";
            return completedFuture(getRedirectSuccess(reason));
        }

        String verifyEmailLink = RandomStringGenerator.getInstance().getNextRandomEmailVerifier();
        String email = account.getEmail();

        boolean didCreateLink = accountDBAccessor.createVerifyEmailLink(email, verifyEmailLink);
        if (didCreateLink) {
            String name = account.getName();
            EmailClient emailClient = new EmailClient(getWsClient());

            return emailClient.sendEmailForVerifyEmail(name, email, verifyEmailLink)
                    .thenApply(isSuccessful -> {
                        if (isSuccessful) {
                            String message = "A verification email has been successfully sent to your email";
                            return getRedirectSuccess(message);
                        } else {
                            String reason = "There was an error sending the email. Please submit a bug report so this issue can be resolved.";
                            return getRedirectFailure(reason);
                        }
                    });
        } else {
            String reason = "There was an error creating the link to send the email. Please submit a bug report so " +
                    "this issue can be resolved.";
            return completedFuture(getRedirectFailure(reason));
        }
    }

    /**
     * Route accessed via email link
     */
    public Result getVerifyEmailLinkPage() {
        Map<String, String[]> queryString = request().queryString();
        String email = Validation.string(Account.EMAIL, queryString);
        if (Validation.isEmpty(email)) {
            String text = "The link that sent you to this page was invalid";
            return badRequest(FailurePage.render(text));
        } else {
            email = email.trim().toLowerCase();
        }

        String verifyEmailToken = Validation.string(Account.ACCOUNT_VERIFY_EMAIL_LINK, queryString);
        if (Validation.isEmpty(verifyEmailToken)) {
            String text = "The link that sent you to this page was invalid";
            return badRequest(FailurePage.render(text));
        }

        Account account = accountDBAccessor.getAccountFromEmail(email);
        if (account == null) {
            Logger.debug("Email: {}; Link: {}", email, verifyEmailToken);
            String text = "You were sent to this page by an invalid email";
            return badRequest(FailurePage.render(text));
        } else if (account.isEmailVerified()) {
            String text = "Your email is already verified";
            return ok(SuccessPage.render(text));
        }

        boolean isSuccessful = accountDBAccessor.getAccountMetaData(account);
        if (!isSuccessful) {
            String reason = "There was an error retrieving your account information. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        if (!verifyEmailToken.equals(account.getMetaData().getVerifyEmailToken())) {
            Logger.debug("Sent Token: {}; Saved Token: {}", verifyEmailToken, account.getMetaData().getVerifyEmailToken());
            String text = "You were sent to this page by an invalid link";
            return badRequest(FailurePage.render(text));
        }

        StripeClient stripeClient = new StripeClient();

        Coupon coupon = null;
        if (account.getMetaData().getStripeCouponId() != null) {
            try {
                coupon = stripeClient.getCouponById(account.getMetaData().getStripeCouponId());
            } catch (InvalidRequestException e) {
                Logger.error("Stripe Exception: ", e);
            }
        }

        String subscriptionId;
        try {
            subscriptionId = stripeClient.createSubscription(account.getCustomerId(), account.getStripePlanId(),
                    coupon);
        } catch (StripeException e) {
            Logger.error("Stripe Exception: ", e);
            return internalServerError("There was an error activating your account. Please try again or submit a bug report");
        }

        boolean didVerifyEmail;
        try {
            didVerifyEmail = accountDBAccessor.verifyEmail(account.getUserId(), verifyEmailToken, subscriptionId);
        } catch (SQLException e) {
            if (!stripeClient.cancelSubscription(subscriptionId)) {
                Logger.error("There was an error cancelling this user\'s subscription: ", new IllegalStateException());
            }
            String reason = "A server error occurred, preventing you from activating your account. Please try again " +
                    "or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        if (!didVerifyEmail) {
            if (!stripeClient.cancelSubscription(subscriptionId)) {
                Logger.error("There was an error cancelling this user\'s subscription: ", new IllegalStateException());
            }
            return badRequest(FailurePage.render("This email referral link was invalid"));
        } else {
            session().put(Account.IS_EMAIL_VERIFIED, String.valueOf(true));
            flash(Account.IS_EMAIL_VERIFIED, "true");
            return redirect(routes.UserController.getLoginPage());
        }

    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result changePassword() {
        return ok(ChangePasswordPage.render());
    }

    public Result forgotPassword() {
        String reason = flash(KEY_ERROR);
        Logger.debug("Reason: {}", reason);
        return ok(ForgotPassword.render(reason));
    }

    /**
     * The route used to allow the user to change his/her account's password (POST)
     */
    @With(ReCaptchaAction.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public CompletionStage<Result> submitForgotPassword() {
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        String referer = Validation.string(REFERER, request().getHeaders().toMap());
        String email = Validation.string(EMAIL, form);

        if (Validation.isEmpty(email)) {
            String reason = "The inputted email was invalid";
            flash(KEY_ERROR, reason);
            return completedFuture(redirect(referer));
        }

        Account account = accountDBAccessor.getAccountFromEmail(email);
        if (account == null) {
            String reason = "This email does not exist";
            flash(KEY_ERROR, reason);
            return completedFuture(redirect(referer));
        }

        final String forgotPasswordLink = RandomStringGenerator.getInstance().getNextRandomPasswordResetLink();
        boolean didAddLink = accountDBAccessor.saveForgotPasswordLink(forgotPasswordLink, email);
        if (didAddLink) {
            return emailClient.sendEmailForResetPassword(account.getName(), email, forgotPasswordLink, account.getUserId())
                    .thenApplyAsync(isSuccessful -> {
                        if (isSuccessful) {
                            return getRedirectSuccess("A password reset link has been sent to your email.");
                        } else {
                            Logger.error("Could not send password reset email!", new IllegalStateException());
                            String reason = "The server was unable to send a password reset email. Please try again or submit a bug report";
                            flash(KEY_ERROR, reason);
                            return redirect(referer);
                        }
                    }, getHttpExecutionContext().current());
        } else {
            String reason = "The server was unable to generate a password reset link";
            flash(KEY_ERROR, reason);
            return completedFuture(redirect(referer));
        }
    }


    public Result resetPassword() {
        Map<String, String[]> map = request().queryString();
        String resetLink = Validation.string(Account.ACCOUNT_FORGOT_PASSWORD_LINK, map);
        String userId = Validation.string(Account.ACCOUNT_USER_ID, map);

        if (resetLink == null || userId == null) {
            String text = "You were sent here by an invalid password reset link";
            return badRequest(FailurePage.render(text));
        } else {
            return ok(ResetPasswordPage.render(userId, resetLink));
        }
    }

    @Security.Authenticated(LogoutAuthenticator.class)
    public Result logoutFromWebApp() {
        Account account = Account.getAccountFromSession();
        boolean isSuccessful = accountDBAccessor.logout(account.getUserId(), account.getToken());
        if (isSuccessful) {
            Account.removeAccountFromSession();
            return redirect(routes.UserController.getLoginPage());
        } else {
            session().put(KEY_ERROR, "A server error occurred and you could not be logged out");
            return redirect(routes.UserController.getProfilePage());
        }
    }

    public Result unsubscribeUser() {
        String email = Validation.string(Account.EMAIL, request().queryString());
        if (Validation.isEmpty(email)) {
            return badRequest(FailurePage.render("You were sent to this page by a bad link"));
        }

        email = email.toLowerCase();
        Account account = accountDBAccessor.getAccountFromEmail(email);
        if (account != null) {
            return ok(SuccessPage.render("You have been successfully unsubscribed"));
        } else {
            return badRequest(FailurePage.render("This email is invalid"));
        }
    }

    public Result operationSuccess() {
        String text = Validation.string("text", request().queryString());
        if (Validation.isEmpty(text)) {
            text = flash(KEY_SUCCESS);
            if (text != null) {
                return redirect(String.format("%s?%s=%s", routes.UserController.operationSuccess().url(), "text", text));
            } else {
                text = "";
            }
        }
        return ok(SuccessPage.render(text));
    }

    public Result operationFailure() {
        String text = Validation.string("text", request().queryString());
        if (Validation.isEmpty(text)) {
            text = flash(KEY_SUCCESS);
            if (text != null) {
                return redirect(String.format("%s?%s=%s", routes.UserController.operationFailure().url(), "text", text));
            } else {
                text = "";
            }
        }
        return ok(FailurePage.render(text));
    }

    // MARK - Private Methods

    private Result getProfilePageForNoPaidSubscription(Account account, VersionChanges versionChanges) {
        if (StripeClient.STATUS_CANCELED.equals(account.getMetaData().getSubscriptionStatus())) {
            current().args.put(KEY_ERROR, account.getMetaData().getSubscriptionStatus());
            return ok(ProfilePage.render(account, false, null, null, null, versionChanges, null));
        }

        try {
            Charge charge = stripeClient.getMostRecentChargeFromMostRecentInvoice(account.getCustomerId());
            if (charge != null) {
                current().args.put(KEY_ERROR, charge);
            }
        } catch (StripeException e) {
            logger.error("Stripe exception: ", e);
        }


        return ok(ProfilePage.render(account, false, null, null, null, versionChanges, null));
    }

}
