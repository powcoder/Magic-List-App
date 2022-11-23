https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import cache.CandidateStateCache;
import cache.DialSheetCache;
import cache.ProspectStateCache;
import cache.UserSettingsCache;
import controllers.BaseController;
import database.*;
import model.account.Account;
import model.account.AccountSettings;
import model.manager.CandidateState;
import model.profile.PersonStatusQuickLink;
import model.profile.ProfileAlert;
import model.prospect.ProspectState;
import model.lists.ProspectSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProfileUtility {

    private static final int DURATION_FIFTEEN_MINUTES = 60 * 15;

    public static List<PersonStatusQuickLink> getPersonStatusQuickLinks() {
        List<PersonStatusQuickLink> statusQuickLinks = new ArrayList<>();
        String searchDetailsUrl = controllers.routes.SearchesController.getSearchDetails().url() + "?" + BaseController.KEY_ASCENDING + "=true";

        Account account = Account.getAccountFromSession();

        List<ProspectState> allStates = ProspectStateCache.getAllStates(account.getCompanyName());

        List<ProspectState> inventoryStates = allStates.stream()
                .filter(ProspectState::isInventory)
                .collect(Collectors.toList());

        StringBuilder inventoryBuilder = new StringBuilder();
        inventoryStates.forEach(state -> {
            inventoryBuilder.append("&selected_states=").append(state.getStateType());
        });

        String inventoryUrl = searchDetailsUrl + inventoryBuilder.toString();
        statusQuickLinks.add(new PersonStatusQuickLink("Inventory", "work", inventoryUrl));

        List<ProspectState> missedAppointmentStates = allStates.stream()
                .filter(ProspectState::isMissedAppointment)
                .collect(Collectors.toList());

        StringBuilder missedAppointmentBuilder = new StringBuilder();
        missedAppointmentStates.forEach(state -> {
            missedAppointmentBuilder.append("&selected_states=").append(state.getStateType());
        });

        String missedDiscoveriesUrl = searchDetailsUrl + missedAppointmentBuilder.toString();
        statusQuickLinks.add(new PersonStatusQuickLink("Missed Appointments", "call_missed_outgoing", missedDiscoveriesUrl));

        return statusQuickLinks;
    }

    public static ProfileAlert getProfileAlert() {
        AppointmentDBAccessor appointmentDBAccessor = new AppointmentDBAccessor(BaseController.getDatabase());
        NotificationDBAccessor notificationDBAccessor = new NotificationDBAccessor(BaseController.getDatabase());
        DialSheetDBAccessor dialSheetDBAccessor = new DialSheetDBAccessor(BaseController.getDatabase());

        Account account = Account.getAccountFromSession();
        Date currentDate = new Date();

        int recentAppointmentsCount = appointmentDBAccessor.getRecentUnfulfilledAppointmentsCount(account.getUserId(), currentDate);
        int unfulfilledNotificationsCount = notificationDBAccessor.getTotalUnarchivedAndCompleteNotifications(account.getUserId());
        int unfulfilledActivitySheetsCount = dialSheetDBAccessor.getPastDialSheetsWithoutUpdatedDialsCount(account.getUserId());
        return new ProfileAlert(recentAppointmentsCount, unfulfilledNotificationsCount, unfulfilledActivitySheetsCount);
    }

    public static Boolean getConfiguredExpandCurrentNotifications() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.PERSON_EXPAND_CURRENT_NOTIFICATIONS, accountSettingsDBAccessor::getPersonDetailsExpandCurrentNotifications);
    }

    public static Boolean getConfiguredExpandPastNotifications() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.PERSON_EXPAND_PAST_NOTIFICATIONS, accountSettingsDBAccessor::getPersonDetailsExpandPastNotifications);
    }

    public static Boolean getConfiguredExpandUpcomingNotifications() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.PERSON_EXPAND_UPCOMING_NOTIFICATIONS, accountSettingsDBAccessor::getPersonDetailsExpandUpcomingNotifications);
    }

    public static Boolean getConfiguredAutoPushNotifications() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.NOTIFICATION_AUTO_DONE_AFTER_PUSH, accountSettingsDBAccessor::getDefaultDoneAfterPush);
    }

    public static String getConfiguredNotificationDate() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.NOTIFICATION_DATE, accountSettingsDBAccessor::getDefaultNotificationDate);
    }

    public static String getConfiguredAppointmentDate() {
        AccountSettingsDBAccessor accountSettingsDBAccessor = new AccountSettingsDBAccessor(BaseController.getDatabase());
        String userId = Account.getAccountFromSession().getUserId();
        return UserSettingsCache.getConfiguration(userId, AccountSettings.APPOINTMENT_DATE, accountSettingsDBAccessor::getDefaultAppointmentDate);
    }

    public static void removeConfigurationsFromCache() {
        Account account = Account.getAccountFromSession();
        UserSettingsCache.removeAll(account.getUserId());
    }

    public static List<ProspectState> getAllPersonContactStatesWithChildren() {
        Account account = Account.getAccountFromSession();
        return ProspectStateCache.getAllStatesWithChildren(account.getCompanyName());
    }

    public static List<ProspectState> getDialSheetObjectionStates() {
        Account account = Account.getAccountFromSession();
        return ProspectStateCache.getAllDialSheetStates(account.getCompanyName());
    }

    public static List<CandidateState> getAllCandidateStates() {
        Account account = Account.getAccountFromSession();
        return CandidateStateCache.getAllCandidateStates(account.getCompanyName());
    }

}
