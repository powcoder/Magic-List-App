https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public final class DateUtility {

    private DateUtility() {
        // No instance
    }

    public static boolean isSameDay(long date1, long date2) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(date1);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(date2);

        return calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR) &&
                calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
    }

    public static boolean isSameMonth(long date1, long date2) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(date1);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(date2);

        return calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
                calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
    }

    /**
     * @return A date of the format "Mar 31, 2017"
     */
    public static String getLongDateForUi(long dateAsMillis) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        Date date = new Date(dateAsMillis);
        return dateFormat.format(date);
    }

    /**
     * @return A date of the format "3/31/2017"
     */
    public static String getShortDateForUi(long dateAsMillis) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        Date date = new Date(dateAsMillis);
        return dateFormat.format(date);
    }

    /**
     * @return A month of the format: March
     */
    public static String getDateForSql(long dateAsMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(dateAsMillis);
        return dateFormat.format(date);
    }

    /**
     * @return A month of the format: 2017
     */
    public static String getYearFromDateForUi(long dateAsMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
        Date date = new Date(dateAsMillis);
        return dateFormat.format(date);
    }

    /**
     * @return The day of the month, as 1 to 31, depending on the month
     */
    public static String getDayOfMonthFromDateForUi(long dateAsMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
        Date date = new Date(dateAsMillis);
        return dateFormat.format(date);
    }

    public static String getMonthFromDateForUi(long dateAsMillis) {
        return new SimpleDateFormat("MMMMM").format(new Date(dateAsMillis));
    }

    /**
     * @return The quarter, returned as "1", "2", "3" or "4"
     */
    public static String getQuarterFromDateForUi(long dateAsMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateAsMillis);
        return String.valueOf(((calendar.get(Calendar.MONTH) / 3) + 1));
    }

    public static String getMonthAndYearFromDateForUi(Date date) {
        return getMonthFromDateForUi(date.getTime()) + " " + getYearFromDateForUi(date.getTime());
    }

    public static String getMonthFromDateForUi(Date date) {
        return getMonthFromDateForUi(date.getTime());
    }

    /**
     * @return A date of the format "3/21/2017 at 2:16 PM"
     */
    public static String getDateWithTimeForUi(long dateAsMillis) {
        Date date = new Date(dateAsMillis);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        return dateFormat.format(date) + " at " + timeFormat.format(date);
    }

    /**
     * @param date The date in the format "yyyy-MM-dd"
     * @return Date in unix millis or null if a parsing error occurred
     */
    public static Date parseDate(String date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * @param date The date in the format "yyyy-MM-dd'T'HH:mm"
     * @return Date in unix millis or -1 if a parsing error occurred
     */
    public static long parseIsoDate(String date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        try {
            return dateFormat.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public static int getTotalDaysInMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static String getWeekFromDate(Date date, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return getDateForSql(calendar.getTimeInMillis());
    }

}
