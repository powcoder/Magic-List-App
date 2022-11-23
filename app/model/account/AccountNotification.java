https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.account;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.Logger;
import play.libs.Json;

import java.util.List;

/**
 * Created by Corey on 6/16/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class AccountNotification {

    public static final String KEY_NOTIFICATION_ID = "notification_id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATE = "date";
    public static final String KEY_IS_FULFILLED = "is_fulfilled";
    public static final String KEY_IS_SEEN = "is_seen";

    public static final String NOTIFICATION_TYPE_MESSAGE = "message";
    public static final String NOTIFICATION_TYPE_EMPLOYEE_REQUEST = "employee_request";

    private final String notificationId;
    private final String notificationType;
    private final String userId;
    private final long notificationDate;
    private final long expirationDate;
    private final boolean isFulfilled;
    private final boolean isSeen;

    public AccountNotification(String notificationId, String notificationType, String userId, long notificationDate,
                               long expirationDate, boolean isFulfilled, boolean isSeen) {
        this.notificationId = notificationId;
        this.notificationType = notificationType;
        this.userId = userId;
        this.notificationDate = notificationDate;
        this.expirationDate = expirationDate;
        this.isFulfilled = isFulfilled;
        this.isSeen = isSeen;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getUserId() {
        return userId;
    }

    public long getNotificationDate() {
        return notificationDate;
    }

    public long getExpirationDate() {
        return expirationDate;
    }

    public boolean isFulfilled() {
        return isFulfilled;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public static int sortByDateDescending(AccountNotification o1, AccountNotification o2) {
        return (int) (o1.notificationDate - o2.notificationDate);
    }

    @Override
    public String toString() {
        return this.notificationId + ": " +  this.notificationDate;
    }

    public static class Converter<T extends AccountNotification> implements JsonConverter<T> {

        @Override
        public ObjectNode renderAsJsonObject(T object) {
            return Json.newObject()
                    .put(KEY_NOTIFICATION_ID, object.getNotificationId())
                    .put(KEY_TYPE, object.getNotificationType())
                    .put(KEY_DATE, object.getNotificationDate())
                    .put(KEY_IS_FULFILLED, object.isFulfilled())
                    .put(KEY_IS_SEEN, object.isSeen());
        }

        @Override
        public T deserializeFromJson(ObjectNode objectNode) {
            throw new RuntimeException("Invalid method call!");
        }

        public static ArrayNode getArrayNodeFromMixedList(List<AccountNotification> accountNotificationList) {
            final MessageNotification.Converter messageConverter = new MessageNotification.Converter();
            final ManagerRequestNotification.Converter managerConverter = new ManagerRequestNotification.Converter();

            ArrayNode arrayNode = Json.newArray();

            for (AccountNotification notification : accountNotificationList) {
                if (notification instanceof MessageNotification) {
                    arrayNode.add(messageConverter.renderAsJsonObject((MessageNotification) notification));
                } else if (notification instanceof ManagerRequestNotification) {
                    arrayNode.add(managerConverter.renderAsJsonObject((ManagerRequestNotification) notification));
                } else {
                    Logger.error("Invalid class, found: " + notification.getClass().getSimpleName());
                }
            }

            return arrayNode;
        }

    }

}
