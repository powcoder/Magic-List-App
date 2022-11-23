https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.account;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Calendar;

/**
 * Created by Corey on 6/17/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class MessageNotification extends AccountNotification {

    public static final String KEY_MESSAGE = "message";
    public static final String KEY_SENDER_NAME = "sender_name";

    private final String message;
    private final String senderId;
    private final String senderName;

    public static MessageNotification createForNewMessage(String notificationId, String userId, long expirationDate,
                                                          String message, String senderId, String senderName) {
        long notificationDate = Calendar.getInstance().getTimeInMillis();
        return new MessageNotification(notificationId, userId, notificationDate, expirationDate, false,
                false, message, senderId, senderName);
    }

    public MessageNotification(String notificationId, String userId, long notificationDate, long expirationDate,
                               boolean isFulfilled, boolean isSeen, String message, String senderId, String senderName) {
        super(notificationId, NOTIFICATION_TYPE_MESSAGE, userId, notificationDate, expirationDate, isFulfilled, isSeen);
        this.message = message;
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public static class Converter extends AccountNotification.Converter<MessageNotification> {

        @Override
        public ObjectNode renderAsJsonObject(MessageNotification object) {
            return super.renderAsJsonObject(object)
                    .put(KEY_MESSAGE, escape(object.message))
                    .put(KEY_SENDER_NAME, escape(object.senderName));
        }
    }

}
