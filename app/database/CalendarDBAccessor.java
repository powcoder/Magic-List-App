https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.calendar.BaseCalendarTemplate;
import model.calendar.CalendarEvent;
import model.calendar.CalendarProvider;
import play.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static database.TablesContract._OAuthAccount.*;
import static database.TablesContract._OutlookCalendarTemplate.*;
import static database.TablesContract._User.USER_ID;

public class CalendarDBAccessor extends ProspectDBAccessor {

    private static final int AMOUNT_PER_PAGE = 25;

    public CalendarDBAccessor(Database database) {
        super(database);
    }

    public List<BaseCalendarTemplate> getUserCalendarTemplate(String userId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT template_id, template_name, date_created, " +
                    "oauth_account.oauth_account_id AS oauth_account_id, provider " +
                    "FROM outlook_calendar_template " +
                    "JOIN oauth_account ON outlook_calendar_template.oauth_account_id = oauth_account.oauth_account_id " +
                    "WHERE " + USER_ID + " = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            List<BaseCalendarTemplate> templateList = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String templateId = resultSet.getString(TEMPLATE_ID);
                String templateName = resultSet.getString(TEMPLATE_NAME);
                String oauthAccountId = resultSet.getString(OAUTH_ACCOUNT_ID);
                long creationDate = resultSet.getDate(DATE_CREATED).getTime();
                CalendarProvider provider = CalendarProvider.parse(resultSet.getString(OAUTH_PROVIDER));
                templateList.add(new BaseCalendarTemplate(oauthAccountId, templateId, templateName, creationDate, provider));
            }
            return templateList;
        });
    }

    public boolean deleteTemplate(String userId, String templateId) {
        return getDatabase().withConnection(connection -> {
            String sql = "DELETE FROM outlook_calendar_template WHERE template_id = ? AND EXISTS (" +
                    "SELECT * FROM _user " +
                    "JOIN oauth_account ON _user.user_id = oauth_account.user_id " +
                    "JOIN outlook_calendar_template " +
                    "ON oauth_account.oauth_account_id = outlook_calendar_template.oauth_account_id " +
                    "WHERE _user.user_id = ?" +
                    ");";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, templateId);
            statement.setString(2, userId);
            return statement.executeUpdate() == 1;
        });
    }

    public boolean deleteLinkedAppointment(String userId, String providerAppointmentId) {
        return getDatabase().withConnection(connection -> {
            String sql = "DELETE FROM calendar_appointment_links WHERE user_id = ? AND provider_appointment_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, providerAppointmentId);
            return statement.executeUpdate() == 1;
        });
    }

    public boolean saveLinkedAppointment(String userId, CalendarEvent event) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE calendar_appointment_links " +
                    "SET hyper_link = ?, subject = ? " +
                    "WHERE user_id = ? AND provider_appointment_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, event.getHyperLink());
            statement.setString(2, event.getSubject());
            statement.setString(3, userId);
            statement.setString(4, event.getEventId());
            return statement.executeUpdate() == 1;
        });
    }

}
