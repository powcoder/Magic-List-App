https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import database.*;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.PagedList;
import model.account.Account;
import model.dialsheet.DialSheet;
import model.lists.SavedList;
import model.prospect.*;
import model.lists.ProspectSearch;
import model.user.User;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.DialSheetUtility;
import utilities.RandomStringGenerator;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.*;
import views.html.migrations.*;
import views.html.person.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static utilities.StringUtility.EMAIL_REGEX;

/**
 *
 */
public class PersonController extends BaseController {

    public static final String KEY_PERSON_ERROR = "person_error";

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final ProspectDBAccessor prospectDBAccessor;

    @Inject
    public PersonController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        prospectDBAccessor = new ProspectDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPeopleForMigration() {
        Account account = Account.getAccountFromSession();

        Map<String, String[]> queryString = request().queryString();
        ProspectSearch.Criteria sortBy = ProspectSearch.Criteria.parse(Validation.string(KEY_SORT_BY, queryString));
        boolean isAscending = Validation.bool(KEY_ASCENDING, queryString, true);
        int currentPage = Validation.page(KEY_PAGE, queryString);

        PagedList<Prospect> personList = prospectDBAccessor.getPeopleForMigrations(account.getUserId(), sortBy, isAscending, currentPage);
        if (personList != null) {
            return ok(ContactStatusMigrationPage.render(personList, sortBy, isAscending));
        } else {
            String reason = "Your migrations could not be retrieved. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPersonById(String personId) {
        Account account = Account.getAccountFromSession();
        Optional<Prospect> person = prospectDBAccessor.getPersonById(account.getUserId(), personId);
        Optional<Boolean> isOwner = prospectDBAccessor.isOwner(account.getUserId(), personId);

        if (person == null || isOwner == null) {
            String reason = "An error occurred while retrieving this person. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        } else if (!person.isPresent() || !isOwner.isPresent()) {
            flash(KEY_PERSON_ERROR, "This person does not exist");
            return redirect(request().header("referer").orElse(routes.UserController.getProfilePage().url()));
        }

        NotificationDBAccessor notificationDBAccessor = new NotificationDBAccessor(getDatabase());

        List<Notification> pastNotifications = notificationDBAccessor.getPersonPastNotifications(account.getUserId(), personId);
        if (pastNotifications == null) {
            String reason = "Could not load person past notifications";
            return internalServerError(FailurePage.render(reason));
        }

        List<Notification> currentNotifications = notificationDBAccessor.getPersonCurrentNotifications(account.getUserId(), personId);
        if (currentNotifications == null) {
            String reason = "Could not load person current notifications";
            return internalServerError(FailurePage.render(reason));
        }

        List<Notification> upcomingNotificationList = notificationDBAccessor.getPersonUpcomingNotifications(account.getUserId(), personId);
        if (upcomingNotificationList == null) {
            String reason = "Could not load person upcoming notifications";
            return internalServerError(FailurePage.render(reason));
        }

        List<ContactStatusAuditItem> contactStatusAuditItemList =
                prospectDBAccessor.getPersonContactAuditTrail(account.getUserId(), personId, account.getCompanyName());
        if (contactStatusAuditItemList == null) {
            String reason = "Could not load person audit trail";
            return internalServerError(FailurePage.render(reason));
        }

        List<Appointment> appointmentList = prospectDBAccessor.getPersonAppointments(account.getUserId(), personId);
        if (appointmentList == null) {
            String reason = "Could not load person\'s appointments";
            logger.error("Could not load person\'s appointments: [account: {}, person: {}]",
                    account.getUserId(), personId);
            return internalServerError(FailurePage.render(reason));
        }

        List<User> sharedUsers = new UserDBAccessor(getDatabase())
                .getSharedUsersForProspect(personId);
        if (sharedUsers == null) {
            String reason = "Could not load the shared users of this contact";
            return internalServerError(FailurePage.render(reason));
        }

        return ok(PersonDetailsPage.render(person.get(), isOwner.get(), upcomingNotificationList,
                currentNotifications, pastNotifications, contactStatusAuditItemList, appointmentList, sharedUsers));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result updatePerson(String personId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Person ID cannot be empty"));
        }

        String name = Validation.string(Prospect.KEY_PERSON_NAME, node);
        if (Validation.isEmpty(name)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_PERSON_NAME));
        }

        String email = Validation.string(Prospect.KEY_EMAIL, node);
        String phoneNumber = Validation.string(Prospect.KEY_PHONE_NUMBER, node);
        String companyName = Validation.string(Prospect.KEY_COMPANY_NAME, node);
        String jobTitle = Validation.string(Prospect.KEY_JOB_TITLE, node);
        Prospect person = Prospect.Factory.createFromRawInput(personId, name, email, phoneNumber, jobTitle, companyName,
                null);

        boolean isSuccessful = prospectDBAccessor.updatePerson(account.getUserId(), person);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not update the person";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result deletePerson(String personId) {
        Account account = Account.getAccountFromSession();

        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Person ID cannot be empty"));
        }

        Optional<Boolean> isSuccessful = prospectDBAccessor.deletePersonById(account.getUserId(), personId);
        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "You are not the owner of this person";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "Could not delete the person";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result createPerson() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String personId = RandomStringGenerator.getInstance().getNextRandomPersonId();
        String name = Validation.string(Prospect.KEY_PERSON_NAME, node);
        if (Validation.isEmpty(name)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_PERSON_NAME));
        }

        String email = Validation.string(Prospect.KEY_EMAIL, node);
        if (!Validation.isEmpty(email) && !email.matches(EMAIL_REGEX)) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_EMAIL));
        }

        String phoneNumber = Validation.string(Prospect.KEY_PHONE_NUMBER, node);
        String companyName = Validation.string(Prospect.KEY_COMPANY_NAME, node);
        String jobTitle = Validation.string(Prospect.KEY_JOB_TITLE, node);
        Prospect person = Prospect.Factory.createFromRawInput(personId, name, email, phoneNumber, jobTitle,
                companyName, null);

        String listId = Validation.string(SavedList.KEY_LIST_ID, node);

        boolean isSuccessful = prospectDBAccessor.createPerson(account.getUserId(), person, listId);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "A server error occurred while creating this person";
            logger.error("Could not create account: [account: {}, person: {}]", account.getUserId(), personId);
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result setPersonState(String personId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }
        if (!prospectDBAccessor.getPersonById(account.getUserId(), personId).isPresent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_ID));
        }

        String rawPersonState = Validation.string(Prospect.KEY_STATE, node);
        if (rawPersonState == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_STATE));
        }
        ProspectState state = new ProspectStateDBAccessor(getDatabase())
                .getProspectStateFromKey(rawPersonState, account.getCompanyName());
        if (state == null || state.isParent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_STATE));
        }

        boolean isSuccessful = prospectDBAccessor.setPersonState(account.getUserId(), personId, state);
        if (!isSuccessful) {
            String reason = "Could not update the person\'s state";
            logger.error("Could not update the person\'s state: [account: {}, person: {}, state: {}]",
                    account.getUserId(), personId, rawPersonState, new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        return DialSheetUtility.getCurrentDialSheet(account.getUserId());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result updatePastPersonState(String personId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }
        if (!prospectDBAccessor.getPersonById(account.getUserId(), personId).isPresent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_ID));
        }

        String rawPersonState = Validation.string(Prospect.KEY_STATE, node);
        if (Validation.isEmpty(rawPersonState)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_STATE));
        }
        ProspectState state = new ProspectStateDBAccessor(getDatabase())
                .getProspectStateFromKey(rawPersonState, account.getCompanyName());
        if (state == null || state.isParent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_STATE));
        }

        String sheetId = Validation.string(DialSheet.KEY_ID, node);
        if (Validation.isEmpty(sheetId)) {
            return badRequest(ResultUtility.getNodeForMissingField(DialSheet.KEY_ID));
        }

        boolean isSuccessful = prospectDBAccessor.updatePastPersonState(account.getUserId(), personId, state, sheetId);
        if (!isSuccessful) {
            String reason = "Could not update the person\'s state";
            logger.error("Could not update the person\'s state: [account: {}, person: {}, state: {}, dial sheet: {}]",
                    account.getUserId(), personId, rawPersonState, sheetId);
            logger.error("Error: ", new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        return DialSheetUtility.getCurrentDialSheet(account.getUserId());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result migratePastPersonState(String personId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }
        if (!prospectDBAccessor.getPersonById(account.getUserId(), personId).isPresent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_ID));
        }

        String rawPersonState = Validation.string(Prospect.KEY_STATE, node);
        if (Validation.isEmpty(rawPersonState)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_STATE));
        }
        ProspectState state = new ProspectStateDBAccessor(getDatabase())
                .getProspectStateFromKey(rawPersonState, account.getCompanyName());
        if (state == null || state.isParent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_STATE));
        }

        boolean isSuccessful = prospectDBAccessor.migratePastPersonState(account.getUserId(), personId, state);
        if (!isSuccessful) {
            String reason = "Could not update the person\'s state";
            logger.error("Could not update the person\'s state: [account: {}, person: {}, state: {}]",
                    account.getUserId(), personId, rawPersonState);
            logger.error("Error: ", new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        return DialSheetUtility.getCurrentDialSheet(account.getUserId());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result setPersonNotes(String personId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (personId == null) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Person ID cannot be null"));
        }

        String notes = Validation.string(ProspectSearch.SearchPredicate.KEY_NOTES, node);

        boolean isSuccessful = prospectDBAccessor.setPersonNotes(account.getUserId(), personId, notes);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not update the person\'s notes";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
