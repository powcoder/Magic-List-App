https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import database.NotificationDBAccessor;
import database.ProspectDBAccessor;
import database.ProspectStateDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.*;
import model.account.Account;
import model.prospect.Notification;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.*;
import views.html.failure.FailurePage;
import views.html.notifications.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NotificationsController extends BaseController {

    public static final String KEY_NUMBER_OF_NOTIFICATIONS = "NUMBER_OF_NOTIFICATIONS";

    private static final String KEY_CONTACT_TIME = "contact_time";

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final NotificationDBAccessor notificationDBAccessor;

    @Inject
    public NotificationsController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        notificationDBAccessor = new NotificationDBAccessor(controllerComponents.getDatabase());
    }


    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result createNotification() {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        String notificationId = RandomStringGenerator.getInstance().getNextRandomNotificationId();
        String userId = account.getUserId();

        String personId = Validation.string(Prospect.KEY_ID, node);
        if (Validation.isEmpty(personId)) {
            logger.debug("Create notification: Invalid person ID");
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_ID));
        }

        long notificationDate = Validation.getLong(Notification.KEY_DATE, node);
        if (notificationDate == -1) {
            return badRequest(ResultUtility.getNodeForMissingField(Notification.KEY_DATE));
        }

        String message = Validation.string(Notification.KEY_MESSAGE, node);
        Optional<Prospect> prospect = new ProspectDBAccessor(getDatabase())
                .getPersonById(userId, personId);
        if (prospect == null) {
            String reason = "There was an error retrieving this prospect\'s information. Please try again";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        } else if (!prospect.isPresent()) {
            String reason = "This prospect does not exist";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }

        Notification notification = new Notification(notificationId, userId, prospect.get(), message, notificationDate,
                -1, false, ProspectState.NOT_CONTACTED);

        notification = notificationDBAccessor.insertNotification(notification);

        if (notification != null) {
            if (DateUtility.isSameDay(notificationDate, Calendar.getInstance().getTimeInMillis())) {
                int numberOfNotifications = Integer.parseInt(
                        Optional.ofNullable(session().get(KEY_NUMBER_OF_NOTIFICATIONS))
                                .orElse("0")
                );
                numberOfNotifications += 1;
                session().put(KEY_NUMBER_OF_NOTIFICATIONS, String.valueOf(numberOfNotifications));
            }

            return sendJsonOk(MagicListObject.serializeToJson(notification));
        } else {
            String reason = "Could not create notification";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result deleteNotification(String notificationId) {
        Account account = Account.getAccountFromSession();

        if (notificationId == null) {
            String reason = "Invalid notification";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }

        boolean isSuccessful = notificationDBAccessor.deleteNotification(account.getUserId(), notificationId);

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not delete notification";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result updateNotificationStatus(String notificationId) {
        Account account = Account.getAccountFromSession();

        String rawState = Validation.string(Prospect.KEY_STATE, request().body().asJson());
        if (Validation.isEmpty(rawState)) {
            return badRequest(ResultUtility.getNodeForMissingField(Prospect.KEY_STATE));
        }

        ProspectState state = new ProspectStateDBAccessor(getDatabase())
                .getProspectStateFromKey(rawState, account.getCompanyName());
        if (state == null || state.isParent()) {
            return badRequest(ResultUtility.getNodeForInvalidField(Prospect.KEY_STATE));
        }

        long contactTime = Validation.getLong(KEY_CONTACT_TIME, request().body().asJson());
        if (contactTime == -1 || Calendar.getInstance().getTimeInMillis() < contactTime) {
            return badRequest(ResultUtility.getNodeForInvalidField(KEY_CONTACT_TIME));
        }

        boolean isSuccessful = notificationDBAccessor.setPersonStateFromNotificationStatus(account.getUserId(), notificationId, state);
        if (!isSuccessful) {
            String reason = "Could not update the person\'s state";
            logger.error("Could not update the person\'s state: [account: {}, notification: {}, state: {}]",
                    account.getUserId(), notificationId, rawState, new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        return DialSheetUtility.getCurrentDialSheet(account.getUserId());
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result batchArchiveNotification() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();
        if (!node.has(Notification.KEY_NOTIFICATION_IDS)) {
            return badRequest(ResultUtility.getNodeForMissingField(Notification.KEY_NOTIFICATION_IDS));
        } else if (node.get(Notification.KEY_NOTIFICATION_IDS).getNodeType() != JsonNodeType.ARRAY) {
            return badRequest(ResultUtility.getNodeForInvalidField(Notification.KEY_NOTIFICATION_IDS));
        }

        ArrayNode notificationIdsArray = (ArrayNode) node.get(Notification.KEY_NOTIFICATION_IDS);
        if (notificationIdsArray.size() == 0) {
            return badRequest(ResultUtility.getNodeForInvalidField(Notification.KEY_NOTIFICATION_IDS));
        }

        List<String> notificationIdList = StreamSupport.stream(notificationIdsArray.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        if (logger.isDebugEnabled()) {
            logger.debug("Notification Ids: {}", MagicListObject.prettyPrint(notificationIdList));
        }

        boolean isSuccessful = notificationDBAccessor.batchArchiveNotifications(account.getUserId(), notificationIdList);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            logger.error("Error batch archiving notifications for account: {}", account.getUserId());
            String error = "There was an error batch archiving your notifications. Please try again or submit a bug report.";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result archiveNotification(String notificationId) {
        return performNotificationArchive(notificationId, true);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result unarchiveNotification(String notificationId) {
        return performNotificationArchive(notificationId, false);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result editNotification(String notificationId) {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();

        String userId = account.getUserId();

        Calendar currentTime = Calendar.getInstance();
        currentTime.set(Calendar.HOUR_OF_DAY, 0);
        currentTime.set(Calendar.MINUTE, 0);
        currentTime.set(Calendar.SECOND, 0);
        currentTime.set(Calendar.MILLISECOND, 0);

        long notificationDate = Validation.getLong(Notification.KEY_DATE, node);
        if (notificationDate == -1) {
            return badRequest(ResultUtility.getNodeForMissingField(Notification.KEY_DATE));
        }

        String message = Validation.string(Notification.KEY_MESSAGE, node);
        Notification notification = new Notification(notificationId, userId, null, message, notificationDate,
                -1, false, ProspectState.NOT_CONTACTED);

        boolean isSuccessful = notificationDBAccessor.editNotification(userId, notification);

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not edit notification";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getNotificationsPage() {
        List<Pair<Notification.Type, PagedList<Notification>>> notificationPairList = new ArrayList<>();

        String userId = Account.getAccountFromSession().getUserId();
        int currentPage = 1;

        int numberOfNotifications = notificationDBAccessor.getNotificationCount(userId);
        if (numberOfNotifications > 0) {
            session().put(KEY_NUMBER_OF_NOTIFICATIONS, String.valueOf(numberOfNotifications));
        } else {
            session().remove(KEY_NUMBER_OF_NOTIFICATIONS);
        }

        PagedList<Notification> pastNotifications = notificationDBAccessor.getPastNotifications(userId, currentPage,
                Notification.Sorter.NOTIFICATION_DATE, false);

        PagedList<Notification> currentNotifications =
                notificationDBAccessor.getCurrentNotifications(userId, currentPage,
                        Notification.Sorter.NOTIFICATION_DATE, false);

        PagedList<Notification> upcomingNotifications =
                notificationDBAccessor.getUpcomingNotifications(userId, currentPage,
                        Notification.Sorter.NOTIFICATION_DATE, true);

        if (pastNotifications == null || currentNotifications == null || upcomingNotifications == null) {
            String reason = "There was an error retrieving your notifications. Please try again or submit a bug report";
            Logger.error(reason, new IllegalStateException());
            return internalServerError(FailurePage.render(reason));
        }

        notificationPairList.add(new Pair<>(Notification.Type.PAST, pastNotifications));
        notificationPairList.add(new Pair<>(Notification.Type.CURRENT, currentNotifications));
        notificationPairList.add(new Pair<>(Notification.Type.UPCOMING, upcomingNotifications));

        return ok(NotificationsPage.render(notificationPairList));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getNotificationsForTodayPage() {
        Account account = Account.getAccountFromSession();

        Date currentDate = new Date();
        PagedList<Notification> notifications = notificationDBAccessor.getNotificationsForToday(account.getUserId(),
                currentDate, Notification.Sorter.NOTIFICATION_DATE, true, 1);

        if (notifications != null) {
            return ok(NotificationsForTodayPage.render(notifications));
        } else {
            logger.error("There was an error getting today\'s notifications for user: {}", account.getUserId());
            String error = "There was an error retrieving your appointments for today. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getNotificationsForToday() {
        Account account = Account.getAccountFromSession();

        Notification.Sorter sorter = Notification.Sorter.parse(Validation.string(KEY_SORT_BY, request().queryString()));
        int page = Validation.page(KEY_PAGE, request().queryString());
        boolean isAscending = Validation.bool(KEY_ASCENDING, request().queryString(), true);

        Date currentDate = new Date();
        PagedList<Notification> notifications = notificationDBAccessor.getNotificationsForToday(account.getUserId(),
                currentDate, sorter, isAscending, page);

        if (notifications != null) {
            return sendJsonOk(MagicListObject.serializeToJson(notifications));
        } else {
            logger.error("There was an error getting today\'s notifications for user: {}", account.getUserId());
            String error = "There was an error retrieving your appointments for today. Please try again or submit a bug report.";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPastNotifications() {
        return getNotificationsFromType(Notification.Type.PAST);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getCurrentNotifications() {
        return getNotificationsFromType(Notification.Type.CURRENT);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUpcomingNotifications() {
        return getNotificationsFromType(Notification.Type.UPCOMING);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getCompletedAndUnarchivedNotifications() {
        Account account = Account.getAccountFromSession();

        int currentPage = Validation.page(KEY_PAGE, request().queryString());
        Notification.Sorter sorter = Notification.Sorter.parse(Validation.string(KEY_SORT_BY, request().queryString()));
        boolean isAscending = Validation.bool(KEY_ASCENDING, request().queryString(), false);

        PagedList<Notification> notifications =
                notificationDBAccessor.getUnarchivedAndCompletedNotifications(account.getUserId(), currentPage, sorter, isAscending);

        if (notifications != null) {
            return sendJsonOk(MagicListObject.serializeToJson(notifications));
        } else {
            logger.error("There was an error retrieving notifications for account: {}", account.getUserId());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    // MARK - Private Methods

    private Result performNotificationArchive(String notificationId, boolean isArchived) {
        String userId = Account.getAccountFromSession().getUserId();

        Optional<Boolean> isSuccessful;
        if (isArchived) {
            isSuccessful = notificationDBAccessor.archiveNotification(userId, notificationId);
        } else {
            isSuccessful = notificationDBAccessor.unarchiveNotification(userId, notificationId);
        }

        if (isSuccessful.isPresent() && isSuccessful.get()) {

            if (isArchived) {
                int numberOfNotifications = Integer.parseInt(
                        Optional.ofNullable(session().get(KEY_NUMBER_OF_NOTIFICATIONS)).orElse("0")
                ) - 1;

                if (numberOfNotifications > 0) {
                    session().put(KEY_NUMBER_OF_NOTIFICATIONS, String.valueOf(numberOfNotifications));
                } else {
                    session().remove(KEY_NUMBER_OF_NOTIFICATIONS);
                }
            } else {
                int numberOfNotifications = Integer.parseInt(
                        Optional.ofNullable(session().get(KEY_NUMBER_OF_NOTIFICATIONS))
                                .orElse("0")
                );
                numberOfNotifications += 1;
                session().put(KEY_NUMBER_OF_NOTIFICATIONS, String.valueOf(numberOfNotifications));
            }

            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String error = "This notification does not exist";
            return badRequest(ResultUtility.getNodeForBooleanResponse(error));
        } else {
            String error = "Could not perform (un)archive action on your notification. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
        }
    }

    private Result getNotificationsFromType(Notification.Type notificationType) {
        Map<String, String[]> queryString = request().queryString();
        boolean defaultSortValue = notificationType == Notification.Type.UPCOMING;

        String userId = Account.getAccountFromSession().getUserId();
        int currentPage = Validation.page(KEY_PAGE, queryString);

        String sortBy = Validation.string(KEY_SORT_BY, queryString);
        Notification.Sorter sorter = Optional.ofNullable(Notification.Sorter.parse(sortBy))
                .orElse(Notification.Sorter.NOTIFICATION_DATE);

        boolean isAscending = Validation.bool(KEY_ASCENDING, queryString, defaultSortValue);

        PagedList<Notification> notifications;
        if (notificationType == Notification.Type.PAST) {
            notifications = notificationDBAccessor.getPastNotifications(userId, currentPage, sorter, isAscending);
        } else if (notificationType == Notification.Type.CURRENT) {
            notifications = notificationDBAccessor.getCurrentNotifications(userId, currentPage, sorter, isAscending);
        } else if (notificationType == Notification.Type.UPCOMING) {
            notifications = notificationDBAccessor.getUpcomingNotifications(userId, currentPage, sorter, isAscending);
        } else {
            String reason = String.format("Invalid notification type, found: %s", notificationType);
            Logger.error(reason, new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }

        if (notifications != null) {
            return sendJsonOk(MagicListObject.serializeToJson(notifications));
        } else {
            String reason = "There was an error retrieving your notifications. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
