https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.BaseController;
import controllers.ManagerMiscController;
import controllers.routes;
import model.account.Account;
import model.user.User;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSRequest;
import play.libs.ws.WSClient;
import play.mvc.Http;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * A utility class used for easily sending emails.
 */
public final class EmailClient {

    private static final long TEN_SECONDS_MILLIS = 10 * 1000L;
    private static final String SERVICE_ACCOUNT_EMAIL = "support@magiclistmaker.com";
    private static final String SEND_GRID_SEND_EMAIL_URL = "https://api.sendgrid.com/v3/mail/send";

    private final String SEND_GRID_API_KEY;
    private final WSClient wsClient;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    public EmailClient(WSClient wsClient) {
        this.wsClient = wsClient;

        SEND_GRID_API_KEY = BaseController.getString("SEND_GRID_API_KEY");
        logger.trace("Using SendGrid Key: " + SEND_GRID_API_KEY);
    }

    /**
     * Sends an email to the given user through the Send Grid API for welcoming him/her to the service
     *
     * @param email The user's email address.
     * @param name  The user's name
     * @return True if the send was successful, false if it wasn't.
     */
    public CompletionStage<Boolean> sendEmailForCreateAccount(String name, String email, String verifyEmailToken) {
        String templateId = "936f8f6f-69fd-4719-bf28-c20fc80b5857";
        String url = buildVerifyEmailUrl(email, verifyEmailToken);

        List<Pair<String, String>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new Pair<>("-name-", name));
        keyValuePairs.add(new Pair<>("-verifyEmailLink-", url));

        return sendEmail(Collections.singletonList(email), templateId, Collections.singletonList(keyValuePairs));
    }

    /**
     * Sends an email to the given user through the Send Grid API for welcoming him/her to the service
     *
     * @param name           The user's name
     * @param email          The user's email
     * @param companyName    The user's company name
     * @param administrators The administrators of the app
     * @return True if the send was successful, false if it wasn't.
     */
    public CompletionStage<Boolean> sendEmailForCreateAccountToAdmin(String name, String email, String companyName,
                                                                     List<User> administrators) {
        String templateId = "81ddc349-9c7d-4f9c-a8f2-a5c66a03221a";

        List<List<Pair<String, String>>> keyValuePairsForEachAdmin = new ArrayList<>();
        administrators.forEach(administrator -> {
            List<Pair<String, String>> keyValuePairs = new ArrayList<>();
            keyValuePairs.add(new Pair<>("-adminName-", administrator.getName()));
            keyValuePairs.add(new Pair<>("-name-", name));
            keyValuePairs.add(new Pair<>("-email-", email));
            keyValuePairs.add(new Pair<>("-companyName-", companyName));
            keyValuePairsForEachAdmin.add(keyValuePairs);
        });

        List<String> adminEmails = administrators.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        return sendEmail(adminEmails, templateId, keyValuePairsForEachAdmin);
    }


    public CompletionStage<Boolean> sendEmailForVerifyEmail(String name, String email, String verifyEmailLink) {
        String templateId = "90c444a3-2519-49b6-a1b8-de70e4f2320a";
        String url = buildVerifyEmailUrl(email, verifyEmailLink);

        List<Pair<String, String>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new Pair<>("-name-", name));
        keyValuePairs.add(new Pair<>("-verifyEmailLink-", url));

        return sendEmail(Collections.singletonList(email), templateId, Collections.singletonList(keyValuePairs));
    }

    /**
     * Sends an email to the given user through the Send Grid API for resetting his/her password
     *
     * @param email      The user's email address
     * @param name       The user's name
     * @param resetToken The unique token used to reset a user's password
     * @return True if the send was successful, false if it wasn't.
     */
    public CompletionStage<Boolean> sendEmailForResetPassword(String name, String email, String resetToken, String userId) {
        String templateId = "01796a56-acb4-43c6-a036-55f41d990e94";
        String passwordResetUrl = buildResetPasswordUrl(resetToken, userId);

        List<Pair<String, String>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new Pair<>("-name-", name));
        keyValuePairs.add(new Pair<>("-resetLink-", passwordResetUrl));

        return sendEmail(Collections.singletonList(email), templateId, Collections.singletonList(keyValuePairs));
    }

    public CompletionStage<Boolean> sendEmailForManagerRequestsEmployee(String employeeName, String employeeEmail,
                                                                        String employeeId, String managerId,
                                                                        String managerName, String requestId) {
        String templateId = "a2571338-c3dd-4556-b54b-e494ceb9fe0c";
        String link = String.format("%s?%s=%s&%s=%s&%s=%s",
                routes.ManagerMiscController.acceptManagerRequest().absoluteURL(Http.Context.current().request()),
                ManagerMiscController.KEY_EMPLOYEE_ID, employeeId,
                ManagerMiscController.KEY_MANAGER_ID, managerId,
                ManagerMiscController.KEY_REQUEST_ID, requestId);

        List<Pair<String, String>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new Pair<>("-name-", employeeName));
        keyValuePairs.add(new Pair<>("-managerName-", managerName));
        keyValuePairs.add(new Pair<>("-link-", link));


        return sendEmail(Collections.singletonList(employeeEmail), templateId, Collections.singletonList(keyValuePairs));
    }

    private CompletionStage<Boolean> sendEmail(List<String> emailRecipientList, String templateId,
                                               List<List<Pair<String, String>>> substitutionKeyValuesForEachRecipient) {
        ObjectNode baseObject = Json.newObject();
        ArrayNode personalizationArray = baseObject.putArray("personalizations");

        Iterator<String> recipientIterator = emailRecipientList.iterator();
        Iterator<List<Pair<String, String>>> substitutionKeyValuePairIterator = substitutionKeyValuesForEachRecipient.iterator();

        while (recipientIterator.hasNext() && substitutionKeyValuePairIterator.hasNext()) {
            ObjectNode personalizationInnerObject = personalizationArray.addObject();

            personalizationInnerObject.putArray("to")
                    .addObject()
                    .put("email", recipientIterator.next());

            ObjectNode substitutionsObject = personalizationInnerObject.putObject("substitutions");

            for (Pair<String, String> substitutionKeyValuePair : substitutionKeyValuePairIterator.next()) {
                substitutionsObject.put(substitutionKeyValuePair.first(), substitutionKeyValuePair.second());
            }
        }

        baseObject.putObject("from")
                .put("email", SERVICE_ACCOUNT_EMAIL);
        baseObject.put("template_id", templateId);

        WSRequest request = wsClient.url(SEND_GRID_SEND_EMAIL_URL)
                .setMethod("POST")
                .setRequestTimeout(Duration.ofMillis(TEN_SECONDS_MILLIS))
                .addHeader("Authorization", "Bearer " + SEND_GRID_API_KEY)
                .addHeader("Content-Type", "application/json");

        return request.post(baseObject)
                .thenApply(result -> {
                    emailRecipientList.forEach(emailRecipient -> {
                        logger.info("Received {} result from mail client for recipient: {}",
                                result.getStatus(), emailRecipient);
                    });
                    return result.getStatus() >= 200 && result.getStatus() < 300;
                });
    }

    private static String buildVerifyEmailUrl(String email, String emailVerificationToken) {
        String verifyEmailLinkPageUrl = controllers.routes.UserController.getVerifyEmailLinkPage()
                .absoluteURL(Http.Context.current().request(), BaseController.isSecure);
        return String.format("<a href=\"%s?%s=%s&%s=%s\">Verify Email</a>", verifyEmailLinkPageUrl,
                Account.ACCOUNT_VERIFY_EMAIL_LINK, emailVerificationToken, Account.EMAIL, email);
    }

    private static String buildResetPasswordUrl(String forgotPasswordLink, String userId) {
        String resetPasswordUrl = routes.UserController.resetPassword()
                .absoluteURL(Http.Context.current().request(), BaseController.isSecure);
        return String.format("<a href=\"%s?%s=%s&%s=%s\">Change Password</a>", resetPasswordUrl,
                Account.ACCOUNT_FORGOT_PASSWORD_LINK, forgotPasswordLink, Account.ACCOUNT_USER_ID, userId);
    }

}
