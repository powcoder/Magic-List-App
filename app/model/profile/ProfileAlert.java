https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.profile;

/**
 * Created by Corey Caplan on 9/29/17.
 */
public class ProfileAlert {

    private final int recentUnfulfilledAppointmentsCount;
    private final int unarchivedAndCompleteNotificationsCount;
    private final int pastDialSheetsWithoutDialsCount;


    public ProfileAlert(int recentUnfulfilledAppointmentsCount, int unarchivedAndCompleteNotificationsCount, int pastDialSheetsWithoutDialsCount) {
        this.recentUnfulfilledAppointmentsCount = recentUnfulfilledAppointmentsCount;
        this.unarchivedAndCompleteNotificationsCount = unarchivedAndCompleteNotificationsCount;
        this.pastDialSheetsWithoutDialsCount = pastDialSheetsWithoutDialsCount;
    }

    public int getRecentUnfulfilledAppointmentsCount() {
        return recentUnfulfilledAppointmentsCount;
    }

    public int getUnarchivedAndCompleteNotificationsCount() {
        return unarchivedAndCompleteNotificationsCount;
    }

    public int getPastDialSheetsWithoutDialsCount() {
        return pastDialSheetsWithoutDialsCount;
    }

    public int getTotalAlertsCount() {
        return recentUnfulfilledAppointmentsCount + unarchivedAndCompleteNotificationsCount + pastDialSheetsWithoutDialsCount;
    }

}
