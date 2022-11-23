https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import database.UserNotificationDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.account.AccountNotification;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Corey on 6/17/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class UserNotificationController extends BaseController {

    private static final String KEY_NOTIFICATION_ID = "notification_id";

    private UserNotificationDBAccessor userNotificationDBAccessor;

    @Inject
    public UserNotificationController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        userNotificationDBAccessor = new UserNotificationDBAccessor(controllerComponents.getDatabase());
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result fulfillNotification(String notificationId) {
        if (Validation.isEmpty(notificationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_NOTIFICATION_ID));
        }

        String userId = Account.getAccountFromSession().getUserId();
        Optional<Boolean> isSuccessful = userNotificationDBAccessor.fulfillNotification(userId, notificationId);

        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "There was an error fulfilling your notification";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result readAllNotifications() {
        String userId = Account.getAccountFromSession().getUserId();
        Optional<Boolean> isSuccessful = userNotificationDBAccessor.setAllNotificationsAsSeen(userId);

        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "There was an error marking your notification as seen";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAllNotifications() {
        String userId = Account.getAccountFromSession().getUserId();
        List<AccountNotification> accountNotificationList = userNotificationDBAccessor.getAllPagesNotifications(userId);

        if (accountNotificationList != null) {
            return ok(AccountNotification.Converter.getArrayNodeFromMixedList(accountNotificationList));
        } else {
            String reason = "There was an error retrieving your notifications. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAllPastNotifications() {
//        TODO
        return TODO;
    }

}
