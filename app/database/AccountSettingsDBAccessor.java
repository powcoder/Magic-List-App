https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import akka.japi.Pair;
import model.account.AccountSettings;
import model.server.VersionChanges;
import play.Logger;
import play.db.Database;
import utilities.ListUtility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static database.TablesContract._ServerVersionRevisions.*;
import static database.TablesContract._UserSettings.*;
import static database.TablesContract._UserSettingsModel.*;
import static database.TablesContract._UserViewsServerVersionRevisions.*;
import static model.account.AccountSettings.*;

/**
 * Created by Corey Caplan on 8/12/17.
 */
public class AccountSettingsDBAccessor extends DBAccessor {

    public AccountSettingsDBAccessor(Database database) {
        super(database);
    }

    public CompletionStage<Boolean> updateDismissVersionChanges(String userId) {
        return executeAsync(connection -> {
            String sql = "INSERT INTO user_views_server_revisions(version_number, user_id, is_dismissed) " +
                    "SELECT MAX(version_number), ?, ? FROM server_version_revisions " +
                    "ON CONFLICT (version_number, user_id) DO UPDATE " +
                    "SET is_dismissed = EXCLUDED.is_dismissed";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setBoolean(2, true);

            return statement.executeUpdate() == 1;
        });
    }

    public VersionChanges getDidDismissVersionChanges(String userId) {
        return execute(connection -> {
            String sql = "SELECT server_version_revisions.version_number, version_changes, is_dismissed, " +
                    "version_release_date " +
                    "FROM server_version_revisions " +
                    "  LEFT JOIN user_views_server_revisions " +
                    "    ON server_version_revisions.version_number = user_views_server_revisions.version_number " +
                    "       AND user_id = ? " +
                    "ORDER BY server_version_revisions.version_number DESC " +
                    "LIMIT 1;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int versionNumber = resultSet.getInt(VERSION_NUMBER);
                String versionChanges = resultSet.getString(VERSION_CHANGES);
                boolean isDismissed = resultSet.getBoolean(IS_DISMISSED);
                long releaseDate = resultSet.getTimestamp(VERSION_RELEASE_DATE).getTime();
                return new VersionChanges(versionNumber, versionChanges, releaseDate, isDismissed);
            } else {
                return null;
            }
        });
    }

    public CompletionStage<Boolean> updateSetting(String userId, List<Pair<AccountSettings, String>> accountSettingsListPairValues) {
        return executeTransactionAsync(connection -> {
            String sql;
            PreparedStatement statement;

            for (Pair<AccountSettings, String> settingsValuePair : accountSettingsListPairValues) {
                sql = "INSERT INTO user_settings (settings_type, user_id, settings_value) VALUES (?, ?, ?) " +
                        "ON CONFLICT(settings_type, user_id) DO UPDATE SET settings_value = EXCLUDED.settings_value " +
                        "   WHERE user_settings.user_id = ? AND user_settings.settings_type = ?;";
                statement = connection.prepareStatement(sql);
                statement.setString(1, settingsValuePair.first().toDatabaseText());
                statement.setString(2, userId);
                statement.setString(3, settingsValuePair.second());
                statement.setString(4, userId);
                statement.setString(5, settingsValuePair.first().toDatabaseText());

                if (statement.executeUpdate() != 1) {
                    Logger.debug("Could not update settings for: {}", sql);
                    return false;
                }
            }

            connection.commit();

            return true;
        });
    }

    public Boolean getPersonDetailsExpandCurrentNotifications(String userId) {
        List<AccountSettings> accountSettings = ListUtility.asList(PERSON_EXPAND_CURRENT_NOTIFICATIONS);
        return getAccountSettings(userId, accountSettings)
                .stream()
                .map(pair -> pair.second().equalsIgnoreCase("true"))
                .findFirst()
                .orElse(null);
    }

    public Boolean getPersonDetailsExpandPastNotifications(String userId) {
        List<AccountSettings> accountSettings = ListUtility.asList(PERSON_EXPAND_PAST_NOTIFICATIONS);
        return getAccountSettings(userId, accountSettings)
                .stream()
                .map(pair -> pair.second().equalsIgnoreCase("true"))
                .findFirst()
                .orElse(null);
    }

    public Boolean getPersonDetailsExpandUpcomingNotifications(String userId) {
        List<AccountSettings> accountSettings = ListUtility.asList(PERSON_EXPAND_UPCOMING_NOTIFICATIONS);
        return getAccountSettings(userId, accountSettings)
                .stream()
                .map(pair -> pair.second().equalsIgnoreCase("true"))
                .findFirst()
                .orElse(null);
    }

    public List<Pair<AccountSettings, Boolean>> getPersonDetailsExpandSettings(String userId) {
        List<AccountSettings> accountSettings = ListUtility.asList(PERSON_EXPAND_CURRENT_NOTIFICATIONS,
                PERSON_EXPAND_PAST_NOTIFICATIONS,
                PERSON_EXPAND_UPCOMING_NOTIFICATIONS);
        return getAccountSettings(userId, accountSettings)
                .stream()
                .map(pair -> new Pair<>(pair.first(), pair.second().equalsIgnoreCase("true")))
                .collect(Collectors.toList());
    }

    public Boolean getDefaultDoneAfterPush(String userId) {
        return getAccountSettings(userId, ListUtility.asList(NOTIFICATION_AUTO_DONE_AFTER_PUSH))
                .stream()
                .map(setting -> setting.second().equalsIgnoreCase("true"))
                .findFirst()
                .orElse(null);
    }

    public String getDefaultNotificationDate(String userId) {
        return getAccountSettings(userId, ListUtility.asList(NOTIFICATION_DATE))
                .stream().map(Pair::second).findFirst().orElse(null);
    }

    public String getDefaultAppointmentDate(String userId) {
        return getAccountSettings(userId, ListUtility.asList(APPOINTMENT_DATE))
                .stream().map(Pair::second).findFirst().orElse(null);
    }

    // MARK - Private Methods

    private List<Pair<AccountSettings, String>> getAccountSettings(String userId, List<AccountSettings> requestedAccountSettings) {
        return execute(connection -> {
            String settingParameters = getQuestionMarkParametersForList(requestedAccountSettings, "?");
            String sql = "SELECT * " +
                    "FROM user_settings " +
                    "RIGHT JOIN user_settings_model ON user_settings.settings_type = user_settings_model.model_settings_type " +
                    "AND user_id = ? " +
                    "WHERE user_settings_model.model_settings_type IN (" + settingParameters + ")";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            int counter = 2;
            for (AccountSettings requestedAccountSetting : requestedAccountSettings) {
                statement.setString(counter++, requestedAccountSetting.toDatabaseText());
            }

            ResultSet resultSet = statement.executeQuery();

            List<Pair<AccountSettings, String>> pairs = new ArrayList<>();
            while (resultSet.next()) {
                AccountSettings settings = parse(resultSet.getString(MODEL_SETTINGS_TYPE));
                String value = getStringFromSettingsQuery(resultSet);
                pairs.add(new Pair<>(settings, value));
            }

            return pairs;
        });
    }

    private static String getStringFromSettingsQuery(ResultSet resultSet) throws SQLException {
        return Optional.ofNullable(resultSet.getString(SETTINGS_VALUE))
                .orElse(resultSet.getString(SETTINGS_DEFAULT_VALUE));
    }

}
