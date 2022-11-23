https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.OAuthClient;
import clients.OutlookAuthClient;
import com.fasterxml.jackson.databind.JsonNode;
import database.*;
import global.authentication.SubscriptionAuthenticator;
import logic.AppointmentToCalendarEventsLinker;
import model.ControllerComponents;
import model.account.Account;
import model.PagedList;
import model.calendar.CalendarEvent;
import model.dialsheet.DialSheetAppointment;
import model.oauth.OAuthToken;
import model.outlook.CalendarAppointment;
import model.outlook.CalendarTemplate;
import model.outlook.OutlookCalendarFactory;
import model.prospect.Appointment;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.*;
import views.html.appointments.*;
import views.html.failure.FailurePage;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utilities.FutureUtility.getFromFutureQuietly;

public class AppointmentsController extends BaseController implements OAuthClient.OnCalendarEventNotFoundListener {

    private static final String KEY_TEMPLATE_ID = "template_id";
    public static final String KEY_APPOINTMENT_LINK_SUCCESS = "appointment_link_success";

    private final DialSheetAppointmentDBAccessor dialSheetAppointmentDBAccessor;
    private final AppointmentDBAccessor appointmentDBAccessor;
    private final CalendarDBAccessor calendarDBAccessor;
    private final OutlookAuthClient outlookAuthClient;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public AppointmentsController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        dialSheetAppointmentDBAccessor = new DialSheetAppointmentDBAccessor(getDatabase());
        appointmentDBAccessor = new AppointmentDBAccessor(getDatabase());
        calendarDBAccessor = new CalendarDBAccessor(getDatabase());
        outlookAuthClient = OutlookAuthClient.getInstance(getWsClient(), getOutlookClientId(), getOutlookClientSecret());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getExportAppointmentToCalendar(String appointmentId) {
        if (Validation.isEmpty(appointmentId)) {
            return badRequest(FailurePage.render("You were sent to this page by a bad link"));
        }

        String templateId = Validation.string(KEY_TEMPLATE_ID, request().queryString());
        if (Validation.isEmpty(templateId)) {
            return badRequest(FailurePage.render("You were sent to this page by a bad link"));
        }

        Account account = Account.getAccountFromSession();

        DialSheetAppointment appointment = dialSheetAppointmentDBAccessor.getAppointmentById(account.getUserId(), appointmentId);

        CalendarTemplate template = new OutlookDBAccessor(getDatabase())
                .getUserCalendarTemplateById(account.getUserId(), templateId);

        String continueUrl = Validation.string(KEY_CONTINUE_URL, request().queryString());

        if (template != null && appointment != null) {
            template.replaceVariablesWithAppointmentInfo(appointment);
            return ok(ExportAppointmentToCalendarPage.render(template.getOauthAccountId(), appointment, template, continueUrl));
        } else {
            String reason = "There was an error retrieving your information. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> exportAppointmentToCalendar(String appointmentId) {
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        DebugUtility.printFormContent(form);

        CalendarTemplate template;
        try {
            template = OutlookCalendarFactory.createTemplateFromForm(form);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(getRedirectFailure(e.getMessage()));
        }


        Account account = Account.getAccountFromSession();
        DialSheetAppointment appointment = dialSheetAppointmentDBAccessor.getAppointmentById(account.getUserId(), appointmentId);
        if (appointment == null) {
            String reason = "There was an error retrieving your appointment. Please try again or submit a bug report";
            return CompletableFuture.completedFuture(getRedirectFailure(reason));
        }

        CalendarAppointment calendarAppointment = OutlookCalendarFactory.createFromTemplate(template, account, appointment);

        OAuthDBAccessor oAuthDBAccessor = new OAuthDBAccessor(getDatabase());

        OAuthToken oauthToken = oAuthDBAccessor.getAuthTokenForAccount(account.getUserId(), template.getOauthAccountId());
        if (oauthToken.isExpired()) {
            oauthToken = getFromFutureQuietly(outlookAuthClient.refreshAccessToken(template.getOauthAccountId(), oauthToken));
            if (oauthToken == null) {
                String text = "There was an error communicating with Outlook. Please try again or submit a bug report";
                return CompletableFuture.completedFuture(getRedirectFailure(text));
            } else {
                oAuthDBAccessor.saveAuthTokenForAccount(account.getUserId(), template.getOauthAccountId(), oauthToken);
            }
        }

        String redirectUrl = Validation.string(KEY_CONTINUE_URL, form);

        return outlookAuthClient.createOutlookEvent(oauthToken, template.getOauthAccountId(), calendarAppointment)
                .thenApplyAsync(calendarEvent -> {
                    if (calendarEvent == null) {
                        String reason = "Your appointment was created with Outlook but could not be linked to " +
                                APP_NAME + ". Please try again or submit a bug report";
                        return getRedirectFailure(reason);
                    }

                    boolean isSuccessful = new OutlookDBAccessor(getDatabase())
                            .createOutlookAppointment(account.getUserId(), calendarEvent, appointmentId);

                    if (!isSuccessful) {
                        String reason = "Your appointment could not be created with Outlook. Please try again or " +
                                "submit a bug report";
                        return getRedirectFailure(reason);
                    }

                    flash(KEY_APPOINTMENT_LINK_SUCCESS, "Your appointment has been linked successfully with Outlook");
                    if (Validation.isEmpty(redirectUrl)) {
                        return redirect(routes.AppointmentsController.getUpcomingUserAppointments().url());
                    } else {
                        try {
                            return redirect(URLDecoder.decode(redirectUrl, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Cannot decode redirect URL: ", e);
                            return redirect(routes.AppointmentsController.getUpcomingUserAppointments().url());
                        }
                    }
                }, getHttpExecutionContext().current())
                .exceptionally(e -> {
                    logger.error("There was an error exporting the appointment for the user: {}", account.getUserId());
                    logger.error("Exception: ", e);
                    return getRedirectFailure("There was an unknown error exporting your appointment. Please submit a bug report of the issue");
                });
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUpcomingUserAppointments() {
        return getUserAppointments(true);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPastUserAppointments() {
        return getUserAppointments(false);
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result setAppointmentOutcome(String appointmentId) {
        Account account = Account.getAccountFromSession();

        JsonNode node = request().body().asJson();

        ProspectState state;
        {
            String rawState = Validation.string(Prospect.KEY_STATE, node);
            if (Validation.isEmpty(rawState)) {
                return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_STATE));
            }
            state = new ProspectStateDBAccessor(getDatabase())
                    .getProspectStateFromKey(rawState, account.getCompanyName());
            if (state == null || state.isParent()) {
                return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_STATE));
            }
        }

        if (Validation.isEmpty(appointmentId)) {
            return badRequest(ResultUtility.getNodeForInvalidField("Appointment_ID"));
        }

        if (!appointmentDBAccessor.setPersonStateFromAppointmentOutcome(account.getUserId(), appointmentId, state)) {
            String error = "Invalid appointment ID or state";
            return badRequest(ResultUtility.getNodeForBooleanResponse(error));
        } else {
            return DialSheetUtility.getCurrentDialSheet(account.getUserId());
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result deleteLinkedProvider(String providerId) {
        Account account = Account.getAccountFromSession();

        if (Validation.isEmpty(providerId)) {
            return badRequest(ResultUtility.getNodeForInvalidField("Appointment_ID"));
        }

        if (calendarDBAccessor.deleteLinkedAppointment(account.getUserId(), providerId)) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return badRequest(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAppointmentsForToday() {
        Account account = Account.getAccountFromSession();
        int currentPage = Validation.page(KEY_PAGE, request().queryString());

        boolean isAscending = Validation.bool(KEY_ASCENDING, request().queryString(), false);
        Appointment.Sorter sorter = Appointment.Sorter.parse(Validation.string(KEY_SORT_BY, request().queryString()));
        Date date = new Date();

        PagedList<Appointment> appointments = appointmentDBAccessor.getAppointmentsForToday(account.getUserId(), date,
                currentPage, sorter, isAscending);
        if (appointments != null) {
            return ok(ViewAppointmentsForTodayPage.render(appointments, sorter, isAscending));
        } else {
            String error = "There was an error retrieving today\'s appointments. Please try again or submit a bug report";
            logger.error("Could not retrieve appointments for: [user: {}, page: {}, date: {}]",
                    account.getUserId(), currentPage, date);
            logger.error("Error: ", new IllegalStateException());
            return internalServerError(FailurePage.render(error));
        }
    }

    @Override
    public void onCalendarEventNotFound(String eventId, Map<String, Appointment> calendarEventsToAppointments) {
        String userId = Account.getAccountFromSession().getUserId();
        if (!calendarDBAccessor.deleteLinkedAppointment(userId, eventId)) {
            logger.error("There was an error deleting {} calendar event: {}", userId, eventId);
            logger.error("Error: ", new IllegalStateException());
        } else {
            List<CalendarEvent> calendarEvents = calendarEventsToAppointments.get(eventId).getCalendarEvents();
            for (int i = 0; i < calendarEvents.size(); i++) {
                if (calendarEvents.get(i).getEventId().equals(eventId)) {
                    calendarEvents.remove(i);
                }
            }
            logger.debug("Successfully deleted appointment [user: {}, event: {}]", userId, eventId);
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getRecentUnfulfilledAppointments() {
        Account account = Account.getAccountFromSession();
        int currentPage = Validation.page(KEY_PAGE, request().queryString());

        boolean isAscending = Validation.bool(KEY_ASCENDING, request().queryString(), false);
        Appointment.Sorter sorter = Appointment.Sorter.parse(Validation.string(KEY_SORT_BY, request().queryString()));
        Date date = new Date();

        PagedList<Appointment> appointmentList = appointmentDBAccessor.getRecentUnfulfilledAppointments(account.getUserId(), date,
                currentPage, sorter, isAscending);
        if (appointmentList == null) {
            String error = "There was an error retrieving your recent appointments. Please try again or submit a bug report";
            logger.error("Could not retrieve appointments for: [user: {}, page: {}, date: {}]",
                    account.getUserId(), currentPage, date);
            logger.error("Error: ", new IllegalStateException());
            return internalServerError(FailurePage.render(error));
        }

        if (!retrieveLinkedCalendarEventsForAppointments(appointmentList, account)) {
            logger.error("Could not link calendar events for user: {}", account.getUserId());
            return internalServerError(MagicListObject.serializeToJson(appointmentList));
        } else {
            return sendJsonOk(MagicListObject.serializeToJson(appointmentList));
        }
    }

    // Private Methods

    private Result getUserAppointments(boolean isUpcoming) {
        Account account = Account.getAccountFromSession();
        Map<String, String[]> queryString = request().queryString();
        int currentPage = Validation.page(KEY_PAGE, queryString);

        boolean isAscending = Validation.bool(KEY_ASCENDING, queryString, true);
        Appointment.Sorter sorter = Appointment.Sorter.parse(Validation.string(KEY_SORT_BY, queryString));

        Date currentDate = new Date();

        PagedList<Appointment> appointmentList;
        if (isUpcoming) {
            appointmentList = appointmentDBAccessor.getUpcomingAppointments(account.getUserId(), currentDate, currentPage, sorter, isAscending);
        } else {
            appointmentList = appointmentDBAccessor.getPastAppointments(account.getUserId(), currentDate, currentPage, sorter, isAscending);
        }

        if (appointmentList == null) {
            String reason = "There was an error retrieving your appointments please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        if (!retrieveLinkedCalendarEventsForAppointments(appointmentList, account)) {
            logger.error("Could not link calendar events for user: {}", account.getUserId());
            return internalServerError(ViewPastOrUpcomingAppointmentsPage.render(appointmentList, isAscending, sorter, isUpcoming));
        } else {
            return ok(ViewPastOrUpcomingAppointmentsPage.render(appointmentList, isAscending, sorter, isUpcoming));
        }
    }

    private boolean retrieveLinkedCalendarEventsForAppointments(PagedList<Appointment> appointmentList, Account account) {
        AppointmentToCalendarEventsLinker linker = new AppointmentToCalendarEventsLinker(getDatabase(), outlookAuthClient);

        if (!linker.linkCalendarEventsToAppointments(appointmentList)) {
            // An error occurred
            return false;
        }

        List<CompletableFuture<List<CalendarEvent>>> newlyRetrievedCalendarEvents = new ArrayList<>();
        linker.getOauthAccountIdsToAuthTokens().keySet().forEach(oauthAccountId -> {
            OAuthToken authToken = linker.getOauthAccountIdsToAuthTokens().get(oauthAccountId);
            List<String> eventIds = linker.getOauthAccountIdsToEventIds().get(oauthAccountId);

            if (!eventIds.isEmpty()) {
                // If there are events to retrieve...
                CompletableFuture<List<CalendarEvent>> future =
                        outlookAuthClient.getBatchedCalendarEvents(oauthAccountId, authToken, eventIds,
                                linker.getCalendarEventsToAppointments(), AppointmentsController.this);
                newlyRetrievedCalendarEvents.add(future);
            }
        });

        // The futures return TRUE if successful, false otherwise
        return FutureUtility.combineStages(newlyRetrievedCalendarEvents)
                .thenApply(calendarEvents -> {
                    if (calendarEvents == null) {
                        String error = "There was an error retrieving some of your linked calendar events. Please submit a " +
                                "bug report so the issue may be resolved";
                        Http.Context.current().args.put(KEY_ERROR, error);
                        return false;
                    }

                    // Update the calendar events & their respective appointment
                    calendarEvents.forEach(event -> {
                        // Update the appointment's calendar event
                        Appointment appointment = linker.getCalendarEventsToAppointments().get(event.getEventId());
                        appointment.getCalendarEvents().forEach(originalCalendarEvent -> {
                            if (originalCalendarEvent.getEventId().equals(event.getEventId())) {
                                originalCalendarEvent.setHyperLink(event.getHyperLink());
                                originalCalendarEvent.setSubject(event.getSubject());
                            }
                        });
                        // Save the calendar in the database
                        if (!calendarDBAccessor.saveLinkedAppointment(account.getUserId(), event)) {
                            logger.error("Could not save appointment for [User: {}, Event: {}]",
                                    account.getUserId(), event.getEventId());
                        }
                    });

                    return true;
                })
                .toCompletableFuture()
                .join();
    }

}
