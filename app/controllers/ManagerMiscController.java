https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.EmailClient;
import database.AdminDBAccessor;
import database.ManagerMiscDBAccessor;
import database.UserNotificationDBAccessor;
import global.authentication.ManagerAuthenticator;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.account.ManagerRequestNotification;
import model.account.MessageNotification;
import model.manager.Employee;
import model.user.User;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.RandomStringGenerator;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.FailurePage;
import views.html.manager.ManageEmployeesPage;
import views.html.manager.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("WeakerAccess")
public class ManagerMiscController extends BaseController {

    public static final String KEY_EMPLOYEE_MANAGER_REQUESTS = "employee_manager_requests";

    public static final String KEY_SEARCH_KEY = "search_key";
    public static final String KEY_EMPLOYEE_ID = "employee_id";
    public static final String KEY_MANAGER_ID = "manager_id";
    public static final String KEY_REQUEST_ID = "request_id";
    public static final String KEY_EMPLOYEE_IDS = "employee_ids";
    public static final String KEY_START_DATE = "start_date";
    public static final String KEY_END_DATE = "end_date";

    private final ManagerMiscDBAccessor managerMiscDBAccessor;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public ManagerMiscController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        managerMiscDBAccessor = new ManagerMiscDBAccessor(controllerComponents.getDatabase());
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result setIsUserManager() {
        boolean isManager = Validation.bool(Account.IS_MANAGER, request().body().asJson());

        Account account = Account.getAccountFromSession();
        boolean isSuccessful = managerMiscDBAccessor.setIsUserManager(account.getUserId(), isManager);
        if (isSuccessful) {
            account.setManager(isManager);
            account.saveAccountToSession(session());
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not set user manager status";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getManagementPortalHomepage() {
        String managerId = Account.getAccountFromSession().getUserId();
        List<Employee> employeeList = managerMiscDBAccessor.getManagerEmployees(managerId, false);

        if (employeeList != null) {
            return ok(ManagerHomePage.render(!employeeList.isEmpty()));
        } else {
            String reason = "There was an error retrieving your information. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getManagerFaqPage() {
        return ok(ManagerFaqPage.render());
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getEditEmployeesPage() {
        String managerId = Account.getAccountFromSession().getUserId();
        List<Employee> employeeList = managerMiscDBAccessor.getManagerEmployees(managerId, false);

        if (employeeList != null) {
            return ok(ManageEmployeesPage.render(employeeList));
        } else {
            String reason = "There was an error retrieving your employees. Please reload the page or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getEmployeeCoworkers() {
        Account account = Account.getAccountFromSession();

        String employeeIdToIgnore = Validation.string(KEY_EMPLOYEE_ID, request().queryString());
        if (Validation.isEmpty(employeeIdToIgnore)) {
            Logger.debug("Field " + KEY_EMPLOYEE_ID + " was empty for #getEmployeeCoworkers");
            employeeIdToIgnore = account.getUserId();
        }

        List<Employee> coworkerList = managerMiscDBAccessor.getCoWorkers(account.getUserId(), employeeIdToIgnore);
        if (coworkerList != null && !coworkerList.isEmpty()) {
            return ok(new Employee.Converter().renderAsJsonArray(coworkerList));
        } else if (coworkerList != null) {
            String reason = "You have no coworkers";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "There was a server error while retrieving your coworkers";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(ManagerAuthenticator.class)
    public CompletionStage<Result> sendEmployeeRequest(String employeeId) {
        if (Validation.isEmpty(employeeId)) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForBooleanResponse(KEY_EMPLOYEE_ID)));
        }

        Account manager = Account.getAccountFromSession();

        Optional<Boolean> isUserOnTeamAlready = managerMiscDBAccessor.isUserOnATeamAlready(employeeId);
        if (isUserOnTeamAlready.isPresent() && isUserOnTeamAlready.get()) {
            String reason = "This user is already on a team";
            return wrapInFuture(status(409, ResultUtility.getNodeForBooleanResponse(reason)));
        }

        String requestId = managerMiscDBAccessor.getManagerRequestEmployeeToken(manager.getUserId(), employeeId)
                .orElseGet(() -> {
                    RandomStringGenerator stringGenerator = RandomStringGenerator.getInstance();
                    String managerRequestId = stringGenerator.getNextRandomManagerRequestEmployeeId();

                    ManagerRequestNotification notification = ManagerRequestNotification.getDefaultInstance(
                            managerRequestId, employeeId, manager.getUserId()
                    );

                    Optional<Boolean> isSuccessful = new UserNotificationDBAccessor(getDatabase())
                            .createManagerRequestNotification(notification);

                    if (isSuccessful.isPresent() && isSuccessful.get()) {
                        return managerRequestId;
                    } else {
                        return null;
                    }
                });

        if (Validation.isEmpty(requestId)) {
            logger.error("Could not create manager request", new IllegalStateException());
            String reason = "A server error occurred. Please try again or submit a bug report";
            return wrapInFuture(internalServerError(ResultUtility.getNodeForBooleanResponse(reason)));
        }

        User employee = new AdminDBAccessor(getDatabase()).getUser(employeeId);

        EmailClient emailClient = new EmailClient(getWsClient());
        return emailClient.sendEmailForManagerRequestsEmployee(employee.getName(), employee.getEmail(), employeeId,
                manager.getUserId(), manager.getName(), requestId)
                .thenApplyAsync(isSuccessful -> {
                    if (isSuccessful) {
                        return ok(ResultUtility.getNodeForBooleanResponse(true));
                    } else {
                        String reason = "A server error occurred and your request could not be completed";
                        return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
                    }
                }, getHttpExecutionContext().current());
    }

    /**
     * No authenticator because this is a request opened via email
     */
    public Result acceptManagerRequest() {
        String badRequestReason = "You were sent to this page by a bad link. If you think this is an error, please " +
                "submit a bug report";
        Map<String, String[]> queryString = request().queryString();

        String employeeId = Validation.string(KEY_EMPLOYEE_ID, queryString);
        if (Validation.isEmpty(employeeId)) {
            return badRequest(FailurePage.render(badRequestReason));
        }

        String requestId = Validation.string(KEY_REQUEST_ID, queryString);
        if (Validation.isEmpty(requestId)) {
            return badRequest(FailurePage.render(badRequestReason));
        }

        String managerId = Validation.string(KEY_MANAGER_ID, queryString);
        if (Validation.isEmpty(managerId)) {
            return badRequest(FailurePage.render(badRequestReason));
        }

        Optional<Boolean> isSuccessful = managerMiscDBAccessor.addUserAsEmployee(managerId, employeeId, requestId);
        if (isSuccessful.isPresent() && !isSuccessful.get()) {
            return badRequest(FailurePage.render(badRequestReason));
        } else if (!isSuccessful.isPresent()) {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        AdminDBAccessor adminDBAccessor = new AdminDBAccessor(getDatabase());

        User employee = adminDBAccessor.getUser(employeeId);
        if (employee == null) {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        User superAdminAccount = adminDBAccessor.getSuperAdminAccount();
        if (superAdminAccount == null) {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        String notificationId = RandomStringGenerator.getInstance().getNextRandomUserNotificationId();
        String message = String.format("%s has accepted the request to join your team!", employee.getName());
        MessageNotification messageNotification = MessageNotification.createForNewMessage(notificationId, managerId,
                -1, message, superAdminAccount.getUserId(), superAdminAccount.getName());

        isSuccessful = new UserNotificationDBAccessor(getDatabase()).createMessageNotification(messageNotification);

        if (isSuccessful.isPresent() && !isSuccessful.get()) {
            return badRequest(badRequestReason);
        } else if (!isSuccessful.isPresent()) {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(reason);
        }

        if (!Validation.isEmpty(session().get(Account.TOKEN))
                && employeeId.equals(Account.getAccountFromSession().getUserId())) {
            flash(KEY_SUCCESS, "You have been successfully added to your manager\'s team");
            return redirect(routes.UserController.getProfilePage());
        } else {
            flash(KEY_SUCCESS, "You have been successfully added to your manager\'s team");
            return redirect(routes.UserController.operationSuccess());
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(ManagerAuthenticator.class)
    public Result deleteEmployeeFromManager(String employeeId) {
        if (Validation.isEmpty(employeeId)) {
            return badRequest(ResultUtility.getNodeForBooleanResponse(KEY_EMPLOYEE_ID));
        }

        String userId = Account.getAccountFromSession().getUserId();
        Optional<Boolean> isSuccessful = managerMiscDBAccessor.deleteEmployee(userId, employeeId);
        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "Invalid employee ID submitted";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result viewEmployeeStatusPage() {
        // TODO route used for checking the status of employees that have not accepted the app-invite
        // TODO allow the manager to resend an app-invite if need-be
        return TODO;
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getUsersFromSearch() {
        String key = Validation.string(KEY_SEARCH_KEY, request().queryString());
        if (Validation.isEmpty(key)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_SEARCH_KEY));
        }

        key = key.trim();

        List<User> userList = managerMiscDBAccessor.searchForEmployeeToAddToTeam(Account.getAccountFromSession(), key);
        if (userList != null) {
            return ok(new User.Converter().renderAsJsonArray(userList));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse("A server error occurred"));
        }
    }

}
