https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.account;

import play.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by Corey Caplan on 8/12/17.
 */
public enum AccountSettings {

    PERSON_EXPAND_CURRENT_NOTIFICATIONS, PERSON_EXPAND_PAST_NOTIFICATIONS, PERSON_EXPAND_UPCOMING_NOTIFICATIONS,

    NOTIFICATION_AUTO_DONE_AFTER_PUSH, NOTIFICATION_DATE,

    APPOINTMENT_DATE;

    public String toDatabaseText() {
        return this.toString().toLowerCase();
    }

    public static AccountSettings parse(String rawText) {
        return Arrays.stream(AccountSettings.values())
                .filter(setting -> setting.toString().equalsIgnoreCase(rawText))
                .findFirst()
                .orElse(null);
    }

    public static Integer parseQuantityFromIntervalSetting(String setting) {
        String[] splitSettings = setting.split(";");
        if (splitSettings.length <= 0) {
            return null;
        }

        String[] splitInterval = splitSettings[0].split("\\s+");
        if (splitInterval.length >= 1) {
            try {
                return Integer.parseInt(splitInterval[0]);
            } catch (Exception ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String parseDateTypeFromIntervalSetting(String setting) {
        String[] splitSettings = setting.split(";");
        if (splitSettings.length <= 0) {
            return null;
        }

        String[] splitInterval = splitSettings[0].split("\\s+");
        if (splitInterval.length >= 2) {
            return splitInterval[1];
        } else {
            return null;
        }
    }

    public static String parseTimeFromIntervalSetting(String setting) {
        String[] splitSettings = setting.split(";");
        if (splitSettings.length >= 2) {
            return splitSettings[1];
        } else {
            return null;
        }
    }

    public static Integer parseHourFromIntervalSetting(String setting) {
        Calendar calendar = getCalendarFromIntervalSetting(setting);
        if (calendar != null) {
            return calendar.get(Calendar.HOUR_OF_DAY);
        } else {
            return null;
        }
    }

    public static Integer parseMinuteFromIntervalSetting(String setting) {
        Calendar calendar = getCalendarFromIntervalSetting(setting);
        if (calendar != null) {
            return calendar.get(Calendar.MINUTE);
        } else {
            return null;
        }
    }

    private static Calendar getCalendarFromIntervalSetting(String setting) {
        String[] splitSettings = setting.split(";");
        if (splitSettings.length >= 2) {
            Calendar calendar = Calendar.getInstance();
            try {
                calendar.setTimeInMillis(new SimpleDateFormat("hh:mma").parse(splitSettings[1]).getTime());
                return calendar;
            } catch (ParseException e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
