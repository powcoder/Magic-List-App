https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.prospect;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.annotations.SerializedName;
import model.JsonConverter;
import model.account.Account;
import model.serialization.MagicListObject;
import play.libs.Json;

import java.util.Arrays;
import java.util.Calendar;

public class Notification extends MagicListObject {

    public enum Sorter {
        NOTIFICATION_DATE, PERSON_NAME, LAST_CONTACTED, COMPANY_NAME, AREA_CODE;

        public static Sorter parse(String value) {
            return Arrays.stream(Sorter.values())
                    .filter(sorter -> sorter.toString().equalsIgnoreCase(value))
                    .findFirst()
                    .orElse(NOTIFICATION_DATE);
        }

        public String toUiString() {
            String[] array = this.toString()
                    .split("_+");

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                builder.append(array[i].substring(0, 1))
                        .append(array[i].substring(1).toLowerCase());
                if (i < array.length - 1) {
                    builder.append(" ");
                }
            }

            return builder.toString();
        }

        public String getRawText() {
            return this.toString().toLowerCase();
        }
    }

    public enum Type {
        PAST, CURRENT, UPCOMING;

        public String getText() {
            return this.toString().substring(0, 1) + this.toString().substring(1, this.toString().length()).toLowerCase();
        }

        public String getRawText() {
            return this.toString().toLowerCase();
        }

        public String getTextForUi() {
            return toString().substring(0, 1) + toString().substring(1, toString().length()).toLowerCase();
        }

        public String getLowercaseText() {
            return toString().toLowerCase();
        }
    }

    private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;

    public static final String KEY_NOTIFICATION_IDS = "notification_ids";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_DATE = "notification_date";

    private final String notificationId;
    private final String userId;
    private final Prospect person;
    private final String message;
    private final long notificationDate;

    @SerializedName("last_contacted")
    private final long lastContactedTime;
    private final boolean isArchived;
    private final ProspectState outcome;

    public Notification(String id, String userId, Prospect person, String message, long notificationDate,
                        long lastContactedTime, boolean isArchived, ProspectState outcome) {
        this.notificationId = id;
        this.userId = userId;
        this.person = person;
        this.message = message;
        this.notificationDate = notificationDate;
        this.lastContactedTime = lastContactedTime;
        this.isArchived = isArchived;
        this.outcome = outcome;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public Prospect getProspect() {
        return person;
    }

    public String getMessage() {
        return message;
    }

    public long getNotificationDate() {
        return this.notificationDate;
    }

    public long getLastContactedTime() {
        return this.lastContactedTime;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public ProspectState getOutcome() {
        return outcome;
    }

}
