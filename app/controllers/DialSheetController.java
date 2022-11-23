https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.*;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.PagedList;
import model.account.Account;
import model.prospect.Prospect;
import model.dialsheet.*;
import model.dialsheet.AllPagesDialSheet;
import model.dialsheet.DialSheet;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.*;
import views.html.dialsheets.*;
import views.html.failure.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static model.dialsheet.DialSheet.KEY_DIAL_COUNT;
import static model.dialsheet.DialSheetAppointment.KEY_APPOINTMENT_TYPE;

public class DialSheetController extends BaseController {

    private static final String KEY_SHOULD_INCREMENT = "should_increment";
    private static final String KEY_ORIGINAL_ACTIVITY_SHEET_ID = "original_activity_sheet_id";
    private static final String KEY_NEW_CONTACT_TIME = "new_contact_time";
    private final DialSheetDBAccessor dialSheetDBAccessor;
    private final DialSheetContactDBAccessor dialSheetContactDBAccessor;
    private final DialSheetAppointmentDBAccessor dialSheetAppointmentDBAccessor;

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public DialSheetController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        dialSheetDBAccessor = new DialSheetDBAccessor(controllerComponents.getDatabase());
        dialSheetContactDBAccessor = new DialSheetContactDBAccessor(controllerComponents.getDatabase());
        dialSheetAppointmentDBAccessor = new DialSheetAppointmentDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result changeDialCountForToday() {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        DialSheet dialSheet = dialSheetDBAccessor.getCurrentAllPagesDialSheet(account.getUserId());
        if (dialSheet == null) {
            String error = "There was an error getting your current dial sheet. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }

        int amount = Validation.integer(KEY_DIAL_COUNT, node);
        boolean shouldIncrement = Validation.bool(KEY_SHOULD_INCREMENT, node);

        boolean isSuccessful;
        if (amount == -1) {
            isSuccessful = dialSheetDBAccessor.changeCallCount(account.getUserId(), dialSheet.getId(), shouldIncrement);
        } else {
            isSuccessful = dialSheetDBAccessor.changeCallCount(account.getUserId(), dialSheet.getId(), amount);
        }

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not change call count";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result changeDialCount(String sheetId) {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        int amount = Validation.integer(KEY_DIAL_COUNT, node);
        boolean shouldIncrement = Validation.bool(KEY_SHOULD_INCREMENT, node);

        boolean isSuccessful;
        if (amount == -1) {
            isSuccessful = dialSheetDBAccessor.changeCallCount(account.getUserId(), sheetId, shouldIncrement);
        } else {
            isSuccessful = dialSheetDBAccessor.changeCallCount(account.getUserId(), sheetId, amount);
        }

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not change call count";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result createDialSheetAppointment() {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        String personId = Validation.string(Prospect.KEY_ID, node);
        if (personId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }

        Optional<Prospect> prospect = new ProspectDBAccessor(getDatabase())
                .getPersonById(account.getUserId(), personId);

        if (prospect == null) {
            String reason = "There was an error retrieving the person associated with this appointment";
            logger.error("Error retrieving person for user: {}", account.getUserId());
            logger.error("Error: ", new IllegalStateException(account.getUserId()));
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        } else if (!prospect.isPresent()) {
            String reason = "This person does not exist";
            logger.info("Error person does not exist for user: {}, person_id: {}", account.getUserId(), personId);
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }

        boolean isConferenceCall = Validation.bool(DialSheetAppointment.KEY_IS_CONFERENCE_CALL, node);

        long appointmentDateMillis = Validation.getLong(DialSheetAppointment.KEY_APPOINTMENT_DATE, node);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.YEAR, 2000);
        currentCalendar.set(Calendar.HOUR, 0);
        currentCalendar.set(Calendar.MINUTE, 0);
        currentCalendar.set(Calendar.SECOND, 0);

        if (appointmentDateMillis < currentCalendar.getTimeInMillis()) {
            return badRequest(ResultUtility.getNodeForInvalidField(DialSheetAppointment.KEY_APPOINTMENT_DATE));
        }

        String notes = Validation.string(MagicListObject.KEY_NOTES, node);

        ProspectState appointmentType;
        {
            String rawAppointmentType = Validation.string(KEY_APPOINTMENT_TYPE, node);
            if (Validation.isEmpty(rawAppointmentType)) {
                return badRequest(ResultUtility.getNodeForMissingField(KEY_APPOINTMENT_TYPE));
            }

            appointmentType = new ProspectStateDBAccessor(getDatabase())
                    .getProspectStateFromKey(rawAppointmentType, account.getCompanyName());
            if (appointmentType == null || (!appointmentType.isInventory() && !appointmentType.isIntroduction())) {
                logger.info("Bad appointment type, found: {} for user: {}", rawAppointmentType, account.getUserId());
                return badRequest(ResultUtility.getNodeForInvalidField(KEY_APPOINTMENT_TYPE));
            }
        }

        String appointmentId = RandomStringGenerator.getInstance().getNextRandomAppointmentId();

        boolean isSuccessful = dialSheetAppointmentDBAccessor.createDialSheetAppointment(account.getUserId(),
                appointmentId, personId, isConferenceCall, appointmentDateMillis, notes, appointmentType);
        if (!isSuccessful) {
            String reason = "There was an error creating your appointment";
            logger.error("Error creating appointment for user: {}", account.getUserId());
            logger.error("Error: ", new IllegalStateException(account.getUserId()));
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        AllPagesDialSheet dialSheet = dialSheetDBAccessor.getCurrentAllPagesDialSheet(account.getUserId());
        if (dialSheet == null) {
            logger.error("There was an error retrieving the updated dial sheet for user: {}", account.getUserId());
            String reason = "There was an error retrieiving your updated dial sheet";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        DialSheetAppointment appointment = new DialSheetAppointment(appointmentId, isConferenceCall,
                appointmentDateMillis, prospect.get(), notes, appointmentType);
        return sendJsonOk(MagicListObject.serializeToJson(new DialSheetWithAppointmentWrapper(dialSheet, appointment)));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result deleteDialSheetAppointment(String appointmentId) {
        Account account = Account.getAccountFromSession();

        if (appointmentId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(DialSheetAppointment.KEY_ID));
        }

        Optional<Boolean> isSuccessful = dialSheetAppointmentDBAccessor.deleteDialSheetAppointment(account.getUserId(), appointmentId);

        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "Could not delete appointment, because you are not the owner";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "Could not delete appointment";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result editDialSheetAppointment(String appointmentId) {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        if (Validation.isEmpty(appointmentId)) {
            return badRequest(ResultUtility.getNodeForMissingField(DialSheetAppointment.KEY_ID));
        }

        String personId = Validation.string(Prospect.KEY_ID, node);
        if (personId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }

        ProspectState appointmentType;
        {
            String rawAppointmentType = Validation.string(KEY_APPOINTMENT_TYPE, node);
            appointmentType = new ProspectStateDBAccessor(getDatabase())
                    .getProspectStateFromKey(rawAppointmentType, account.getCompanyName());
            if (appointmentType == null || (!appointmentType.isInventory() && !appointmentType.isIntroduction())) {
                return badRequest(ResultUtility.getNodeForInvalidField(KEY_APPOINTMENT_TYPE));
            }
        }

        long appointmentDateMillis = Validation.getLong(DialSheetAppointment.KEY_APPOINTMENT_DATE, node);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.YEAR, 2000);
        currentCalendar.set(Calendar.HOUR, 0);
        currentCalendar.set(Calendar.MINUTE, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        if (appointmentDateMillis < currentCalendar.getTimeInMillis()) {
            return badRequest(ResultUtility.getNodeForInvalidField(DialSheetAppointment.KEY_APPOINTMENT_DATE));
        }

        boolean isConferenceCall = Validation.bool(DialSheetAppointment.KEY_IS_CONFERENCE_CALL, node);

        String notes = Validation.string(DialSheetAppointment.KEY_NOTES, node);
        Prospect person = Prospect.Factory.createFromId(personId);

        DialSheetAppointment appointment = new DialSheetAppointment(appointmentId, isConferenceCall,
                appointmentDateMillis, person, notes, appointmentType);
        Optional<Boolean> isSuccessful = dialSheetAppointmentDBAccessor.editDialSheetAppointment(account.getUserId(), appointment);

        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "Could not edit appointment, because you are not the owner of it";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "Could not edit appointment";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result editDialSheetContactTime() {
        JsonNode node = request().body().asJson();
        String originalActivitySheetId = Validation.string(KEY_ORIGINAL_ACTIVITY_SHEET_ID, node);
        long rawNewContactTime = Validation.getLong(KEY_NEW_CONTACT_TIME, node);
        String personId = Validation.string(Prospect.KEY_ID, node);

        if (Validation.isEmpty(originalActivitySheetId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_ORIGINAL_ACTIVITY_SHEET_ID));
        }
        if (rawNewContactTime == -1) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_NEW_CONTACT_TIME));
        }
        if (Validation.isEmpty(personId)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(rawNewContactTime);
        if (calendar.get(Calendar.YEAR) < 2000) {
            return badRequest(ResultUtility.getNodeForInvalidField(KEY_NEW_CONTACT_TIME));
        } else if (rawNewContactTime > Calendar.getInstance().getTimeInMillis()) {
            return badRequest(ResultUtility.getNodeForInvalidField(KEY_NEW_CONTACT_TIME));
        }

        Account account = Account.getAccountFromSession();

        Optional<Prospect> prospect = new ProspectDBAccessor(getDatabase())
                .getPersonById(account.getUserId(), personId);
        if (!prospect.isPresent()) {
            String error = "There was an error retrieving your prospect information. Please try again or submit a bug report";
            return badRequest(ResultUtility.getNodeForBooleanResponse(error));
        }

        DialSheet dialSheet = dialSheetContactDBAccessor.editDialSheetContactTime(account.getUserId(), personId,
                originalActivitySheetId, rawNewContactTime);
        if (dialSheet != null) {
            return sendJsonOk(MagicListObject.serializeToJson(dialSheet));
        } else {
            logger.error("Error editing contact for account: {}", account.getUserId());
            String error = "There was an error editing your contact time. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUnfulfilledDialSheetDialCounts() {
        Account account = Account.getAccountFromSession();
        int page = Validation.page(KEY_PAGE, request().queryString());
        PagedList<DialSheet> dialSheets = dialSheetDBAccessor.getPastDialSheetsWithoutUpdatedDialCounts(account.getUserId(), page);

        if (dialSheets != null) {
            return sendJsonOk(MagicListObject.serializeToJson(dialSheets));
        } else {
            logger.error("Could not get unfulfilled dial sheets for account: {}", account.getUserId());
            String error = "A server error occurred while retrieving your past unfulfilled dial sheets";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getCurrentDialSheet() {
        Account account = Account.getAccountFromSession();

        AllPagesDialSheet dialSheet = dialSheetDBAccessor.getCurrentAllPagesDialSheet(account.getUserId());

        if (dialSheet != null) {
            return sendJsonOk(MagicListObject.serializeToJson(dialSheet));
        } else {
            String reason = "Could not load your current activity sheet";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPastDialSheets() {
        Map<String, String[]> map = request().queryString();
        Account account = Account.getAccountFromSession();

        Date dateToView;
        {
            String rawDate = Validation.string(MagicListObject.KEY_DATE, map);
            if (rawDate == null) {
                dateToView = new Date();
            } else {
                dateToView = DateUtility.parseDate(rawDate);
                if (dateToView == null) {
                    return badRequest(FailurePage.render("You were sent to this page by an invalid date."));
                }
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateToView.getTime());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        dateToView.setTime(calendar.getTimeInMillis());

        List<DialSheetDates> yearsToDialSheetMonthsMap = dialSheetDBAccessor.getPastDialSheetMonthsAndYears(account.getUserId());
        if (yearsToDialSheetMonthsMap == null) {
            String reason = "There was an error loading your activity sheets. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        List<DialSheet> dialSheets = dialSheetDBAccessor.getPastDialSheets(account.getUserId(), dateToView);

        if (dialSheets != null) {
            Map<Integer, List<DialSheet>> dialSheetMap = dialSheets.stream().collect(Collectors.groupingBy(sheet -> {
                calendar.setTimeInMillis(sheet.getDate());
                return calendar.get(Calendar.DAY_OF_MONTH);
            }));

            return ok(DialSheetHistoryPage.render(dialSheetMap, dateToView, yearsToDialSheetMonthsMap));
        } else {
            String reason = "There was an error loading your activity sheets. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getDialSheetDetails(String sheetId) {
        Account account = Account.getAccountFromSession();

        DialSheetPaginationSummary previousDialSheet = dialSheetDBAccessor.getPreviousDialSheet(account.getUserId(), sheetId);
        DialSheetPaginationSummary nextDialSheet = dialSheetDBAccessor.getNextDialSheet(account.getUserId(), sheetId);

        DialSheet dialSheet = dialSheetDBAccessor.getDialSheetById(account.getUserId(), sheetId);

        if (dialSheet == null) {
            String error = "There was an error retrieving your dial sheet. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        DialSheetDetails dialSheetDetails = dialSheetDBAccessor.getDailyDialSheetDetails(account.getUserId(), new Date(dialSheet.getDate()));

        if (dialSheetDetails == null) {
            String reason = "Could not load the activity sheet\'s details";
            return internalServerError(FailurePage.render(reason));
        }

        dialSheetDetails.setId(dialSheet.getId());

        return ok(DialSheetDetailsPage.render(dialSheetDetails, previousDialSheet, nextDialSheet, false,
                null, DialSheetType.DAY));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getWeeklyDialSheetDetails() {
        Account account = Account.getAccountFromSession();
        return getDialSheetDetailsForType(account.getUserId(), DialSheetType.WEEK, null, null);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getMonthlyDialSheetDetails() {
        Account account = Account.getAccountFromSession();
        return getDialSheetDetailsForType(account.getUserId(), DialSheetType.MONTH, null, null);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getQuarterlyDialSheetDetails() {
        Account account = Account.getAccountFromSession();
        return getDialSheetDetailsForType(account.getUserId(), DialSheetType.QUARTER, null, null);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getYearlyDialSheetDetails() {
        Account account = Account.getAccountFromSession();
        return getDialSheetDetailsForType(account.getUserId(), DialSheetType.YEAR, null, null);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAllTimeDialSheetDetails() {
        Account account = Account.getAccountFromSession();
        return getDialSheetDetailsForType(account.getUserId(), DialSheetType.CENTURY, null, null);
    }

    // Mark - Private Methods

    private Result getDialSheetDetailsForType(String userId, DialSheetType type,
                                              DialSheetPaginationSummary previousDialSheet,
                                              DialSheetPaginationSummary nextDialSheet) {
        Date dateToView;
        {
            String rawDate = Validation.string(MagicListObject.KEY_DATE, request().queryString());
            if (Validation.isEmpty(rawDate)) {
                return badRequest(FailurePage.render("You were sent to this page by an invalid date."));
            }

            dateToView = DateUtility.parseDate(rawDate);
            if (dateToView == null) {
                return badRequest(FailurePage.render("You were sent to this page by an invalid date."));
            }
        }

        DialSheetDetails dialSheetDetails;
        switch (type) {
            case DAY:
                dialSheetDetails = dialSheetDBAccessor.getDailyDialSheetDetails(userId, dateToView);
                break;
            case WEEK:
                dialSheetDetails = dialSheetDBAccessor.getWeeklyDialSheetDetails(userId, dateToView);
                break;
            case MONTH:
                dialSheetDetails = dialSheetDBAccessor.getMonthlyDialSheetDetails(userId, dateToView);
                break;
            case QUARTER:
                dialSheetDetails = dialSheetDBAccessor.getQuarterlyDialSheetDetails(userId, dateToView);
                break;
            case YEAR:
                dialSheetDetails = dialSheetDBAccessor.getYearlyDialSheetDetails(userId, dateToView);
                break;
            case CENTURY:
                dialSheetDetails = dialSheetDBAccessor.getAllTimeDialSheetDetails(userId, dateToView);
                break;
            default:
                logger.error("Invalid dial sheet type, found: {}", type);
                return internalServerError(FailurePage.render("A server error occurred. Please submit a bug report so we can resolve the issue"));
        }

        if (dialSheetDetails != null) {
            return ok(DialSheetDetailsPage.render(dialSheetDetails, previousDialSheet, nextDialSheet, false, null, type));
        } else {
            logger.error("Error fetching activity sheet for user: {}", userId);
            String error = "There was an error retrieving your activity sheet\'s details. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }
    }

}
