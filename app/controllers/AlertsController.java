https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.google.inject.Inject;
import database.AppointmentDBAccessor;
import database.DialSheetDBAccessor;
import database.NotificationDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.PagedList;
import model.account.Account;
import model.dialsheet.DialSheet;
import model.prospect.Appointment;
import model.prospect.Notification;
import play.Logger;
import play.mvc.Result;
import play.mvc.Security;
import utilities.Validation;
import views.html.alerts.AlertsPage;
import views.html.failure.FailurePage;

import java.util.Date;

/**
 * Created by Corey Caplan on 9/24/17.
 */
public class AlertsController extends BaseController {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public AlertsController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAlertsPage() {
        Account account = Account.getAccountFromSession();

        PagedList<Appointment> unfulfilledAppointments = new AppointmentDBAccessor(getDatabase())
                .getRecentUnfulfilledAppointments(account.getUserId(), new Date(), 1,
                        Appointment.Sorter.APPOINTMENT_DATE, false);
        if (unfulfilledAppointments == null) {
            logger.error("Could not get unfulfilled appointments for account: {}", account.getUserId());
            String error = "There was an error retrieving your appointment alerts. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        PagedList<Notification> unarchivedNotifications = new NotificationDBAccessor(getDatabase())
                .getUnarchivedAndCompletedNotifications(account.getUserId(), 1,
                        Notification.Sorter.NOTIFICATION_DATE, false);
        if (unarchivedNotifications == null) {
            logger.error("Could not get completed notifications for account: {}", account.getUserId());
            String error = "There was an error retrieving your completed notifications. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }


        PagedList<DialSheet> unfulfilledDialSheets = new DialSheetDBAccessor(getDatabase())
                .getPastDialSheetsWithoutUpdatedDialCounts(account.getUserId(), 1);
        if (unfulfilledDialSheets == null) {
            logger.error("Could not get unfulfilled activity sheets for account: {}", account.getUserId());
            String error = "There was an error retrieving your unfulfilled activity sheet alerts. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }


        return ok(AlertsPage.render(unfulfilledAppointments, unarchivedNotifications, unfulfilledDialSheets));
    }

}
