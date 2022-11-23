https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import database.AdminDBAccessor;
import database.UserNotificationDBAccessor;
import global.authentication.AdminAuthenticator;
import global.authentication.SuperAdminAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.account.MessageNotification;
import model.user.BugReport;
import model.user.User;
import model.user.UserQuote;
import model.user.Suggestion;
import play.Logger;
import play.Mode;
import play.api.Play;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.RandomStringGenerator;
import utilities.TimeUtility;
import utilities.Validation;
import views.html.admin.*;
import views.html.failure.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class AdminController extends BaseController {

    public static final String KEY_MESSAGE = "message";

    private final AdminDBAccessor adminDBAccessor;

    @Inject
    public AdminController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        adminDBAccessor = new AdminDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(AdminAuthenticator.class)
    public Result getAdminConsolePage() {
        return ok(AdminPage.render());
    }

    @Security.Authenticated(AdminAuthenticator.class)
    public Result getSuggestionsPage() {
        List<Suggestion> suggestionList = adminDBAccessor.getSuggestions();
        if (suggestionList != null) {
            return ok(SuggestionsPage.render(suggestionList));
        } else {
            return internalServerError(FailurePage.render("Could not load suggestions!"));
        }
    }

    @Security.Authenticated(AdminAuthenticator.class)
    public Result getBugReportsPage() {
        List<BugReport> bugReportList = adminDBAccessor.getBugReports();
        if (bugReportList != null) {
            return ok(BugReportsPage.render(bugReportList));
        } else {
            return internalServerError(FailurePage.render("Could not load bug reports!"));
        }
    }

    @Security.Authenticated(AdminAuthenticator.class)
    public Result getQuotesPage() {
        List<UserQuote> userQuoteList = adminDBAccessor.getUserQuotes();
        if (userQuoteList != null) {
            return ok(QuotesPage.render(userQuoteList));
        } else {
            return internalServerError(FailurePage.render("Could not load submitted quotes!"));
        }
    }

    @Security.Authenticated(AdminAuthenticator.class)
    public Result getUserPage(String userId) {
        User user = adminDBAccessor.getUser(userId);
        if (user != null && user.getEmail() != null) {
            return ok(UserPage.render(user));
        } else if (user != null) {
            return notFound(NotFoundPage.render());
        } else {
            return internalServerError(FailurePage.render("Could not load submitted quotes!"));
        }
    }

    @Security.Authenticated(SuperAdminAuthenticator.class)
    public Result getCreateMessagePage() {
        String error = flash(KEY_ERROR);
        if (error != null) {
            Logger.debug("Create Message error: {}", error);
        }
        return ok(CreateMessagePage.render(error));
    }

    @Security.Authenticated(SuperAdminAuthenticator.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result createMessage() {
        String adminId = Account.getAccountFromSession().getUserId();
        String adminName = Account.getAccountFromSession().getName();
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        String referer = Validation.string(REFERER, request().getHeaders().toMap());

        String message = Validation.string(KEY_MESSAGE, form);
        if (Validation.isEmpty(message)) {
            flash(KEY_ERROR, "Missing a message");
            return redirect(referer);
        }

        List<User> allUsers = adminDBAccessor.getAllUsers(adminId);
        if (allUsers == null) {
            String error = "A server error occurred while retrieving the users. Please try again or submit a bug report";
            flash(KEY_ERROR, error);
            return redirect(referer);
        }

        RandomStringGenerator generator = RandomStringGenerator.getInstance();
        long notificationDate = Calendar.getInstance().getTimeInMillis();
        long expirationDate = notificationDate + TimeUtility.ONE_WEEK_MILLIS;

        List<MessageNotification> messageNotifications = allUsers.stream()
                .map(user -> new MessageNotification(generator.getNextRandomUserNotificationId(), user.getUserId(),
                        notificationDate, expirationDate, false, false, message, adminId, adminName))
                .collect(Collectors.toList());

        Optional<Boolean> isSuccessful = new UserNotificationDBAccessor(getDatabase())
                .createMessageNotification(messageNotifications);

        if (isSuccessful.isPresent() && isSuccessful.get()) {
            flash(KEY_SUCCESS, "Your message was successfully created");
            return redirect(routes.AdminController.getAdminConsolePage());
        } else {
            String error = "A server error occurred while retrieving the users. Please try again or submit a bug report";
            flash(KEY_ERROR, error);
            return redirect(referer);
        }
    }

}
