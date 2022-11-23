https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import database.AccountSettingsDBAccessor;
import global.authentication.BaseAccountAuthenticator;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.account.AccountSettings;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ProfileUtility;
import utilities.ResultUtility;
import views.html.failure.FailurePage;
import views.html.settings.*;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class SettingsController extends BaseController {

    public static final String KEY_KEY = "key";
    public static final String KEY_VALUE = "value";

    private final Logger.ALogger logger = Logger.of(this.getClass());

    private final AccountSettingsDBAccessor accountSettingsDBAccessor;

    @Inject
    public SettingsController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        accountSettingsDBAccessor = new AccountSettingsDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getContactStatuses() {
        List<ProspectState> stateList = ProfileUtility.getAllPersonContactStatesWithChildren();

        if (stateList != null) {
            return sendJsonOk(MagicListObject.serializeToJson(stateList));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }


    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getSettings() {
        Account account = Account.getAccountFromSession();

        List<Pair<AccountSettings, Boolean>> expandSettingsList = accountSettingsDBAccessor.getPersonDetailsExpandSettings(account.getUserId());
        if (expandSettingsList == null) {
            return getFailurePageForInternalServer();
        }

        Boolean defaultAutoPushNotifications = accountSettingsDBAccessor.getDefaultDoneAfterPush(account.getUserId());
        if (defaultAutoPushNotifications == null) {
            return getFailurePageForInternalServer();
        }

        String notificationDate = accountSettingsDBAccessor.getDefaultNotificationDate(account.getUserId());
        if (notificationDate == null) {
            return getFailurePageForInternalServer();
        }

        String appointmentDate = accountSettingsDBAccessor.getDefaultAppointmentDate(account.getUserId());
        if (appointmentDate == null) {
            return getFailurePageForInternalServer();
        }

        return ok(SettingsPage.render(account, expandSettingsList, defaultAutoPushNotifications, notificationDate, appointmentDate));
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(BaseAccountAuthenticator.class)
    public CompletionStage<Result> dismissVersionChanges() {
        Account account = Account.getAccountFromSession();
        return accountSettingsDBAccessor.updateDismissVersionChanges(account.getUserId())
                .exceptionally(e -> {
                    throw new RuntimeException(e);
                })
                .thenApply(isSuccessful -> {
                    if (isSuccessful) {
                        return ok(ResultUtility.getNodeForBooleanResponse(true));
                    } else {
                        String reason = "A server error occurred while dismissed this version revision";
                        return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
                    }
                });
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(BaseAccountAuthenticator.class)
    public CompletionStage<Result> changeSettings() {
        JsonNode node = request().body().asJson();

        if (node.get(KEY_KEY) == null || node.get(KEY_KEY).getNodeType() != JsonNodeType.ARRAY) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_KEY)));
        }

        ArrayNode keys = (ArrayNode) node.get(KEY_KEY);

        if (node.get(KEY_VALUE) == null || node.get(KEY_VALUE).getNodeType() != JsonNodeType.ARRAY) {
            logger.debug("Invalid value, found: {}", node.get(KEY_VALUE));
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_VALUE)));
        }

        ArrayNode rawValues = (ArrayNode) node.get(KEY_VALUE);

        if (keys.size() != rawValues.size()) {
            logger.debug("Keys Size: {}; Values Size: {}", keys.size(), rawValues.size());
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_VALUE)));
        }

        List<String> valuesList = StreamSupport.stream(rawValues.spliterator(), false)
                .map(value -> value.getNodeType() == JsonNodeType.STRING ? value.asText().trim() : null)
                .collect(Collectors.toList());

        if (valuesList.stream().anyMatch(Objects::isNull)) {
            logger.debug("Found a null value!");
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_VALUE)));
        }

        List<AccountSettings> settingsList = StreamSupport.stream(keys.spliterator(), false)
                .map(key -> key.getNodeType() == JsonNodeType.STRING ? AccountSettings.parse(key.asText()) : null)
                .collect(Collectors.toList());

        if (settingsList.stream().anyMatch(Objects::isNull)) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_KEY)));
        }

        if (settingsList.isEmpty() || valuesList.size() != settingsList.size()) {
            return wrapInFuture(badRequest(ResultUtility.getNodeForInvalidField(KEY_KEY)));
        }

        List<Pair<AccountSettings, String>> accountSettingsValuePairList = IntStream.range(0, settingsList.size())
                .boxed()
                .map(i -> new Pair<>(settingsList.get(i), valuesList.get(i)))
                .collect(Collectors.toList());

        for (Pair<AccountSettings, String> accountSettingValuePair : accountSettingsValuePairList) {
            AccountSettings settings = accountSettingValuePair.first();
            if (settings == AccountSettings.NOTIFICATION_DATE || settings == AccountSettings.APPOINTMENT_DATE) {
                if (!isDateSettingValid(accountSettingValuePair.second())) {
                    String reason = "Invalid date setting format";
                    return wrapInFuture(badRequest(ResultUtility.getNodeForBooleanResponse(reason)));
                }
            }
        }

        String userId = Account.getAccountFromSession().getUserId();
        return accountSettingsDBAccessor.updateSetting(userId, accountSettingsValuePairList)
                .exceptionally(e -> {
                    throw new RuntimeException(e);
                })
                .thenApplyAsync(isSuccessful -> {
                    if (isSuccessful) {
                        ProfileUtility.removeConfigurationsFromCache();
                        return ok(ResultUtility.getNodeForBooleanResponse(true));
                    } else {
                        String error = "Your setting was invalid and not updated";
                        return badRequest(ResultUtility.getNodeForBooleanResponse(error));
                    }
                }, getHttpExecutionContext().current())
                .exceptionally(e -> {
                    logger.error("Error: ", e);
                    String error = "Could not update settings. Please try again or submit a bug report";
                    return internalServerError(ResultUtility.getNodeForBooleanResponse(error));
                });

    }

    private static Result getFailurePageForInternalServer() {
        String error = "There was an error retrieving your settings. Please try again or submit a bug report";
        return internalServerError(FailurePage.render(error));
    }

    private boolean isDateSettingValid(String value) {
        String[] splitValues = value.split(";");
        if (splitValues.length != 2) {
            logger.debug("Invalid values length: {}", splitValues.length);
            return false;
        }

        String[] splitInterval = splitValues[0].split("\\s+");
        if (splitInterval.length != 2) {
            logger.debug("Invalid interval length: {}", splitInterval.length);
            return false;
        }

        try {
            int intervalQuantity = Integer.valueOf(splitInterval[0]);
            if (intervalQuantity < 0) {
                logger.debug("Invalid time, found: {}", intervalQuantity);
                return false;
            }
        } catch (Exception e) {
            logger.debug("Invalid time, found: {}", splitInterval[0]);
            return false;
        }

        if (!"days".equals(splitInterval[1]) && !"weeks".equals(splitInterval[1]) && !"months".equals(splitInterval[1])) {
            logger.debug("Invalid unit, found: {}", splitInterval[1]);
            return false;
        }

        String time = splitValues[1];
        SimpleDateFormat format = new SimpleDateFormat("hh:mma");
        try {
            format.parse(time);
        } catch (ParseException e) {
            logger.debug("Parse Exception: ", e);
            return false;
        }

        return true;
    }

}
