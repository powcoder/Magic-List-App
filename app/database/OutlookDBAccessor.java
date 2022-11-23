https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import model.JsonConverter;
import model.calendar.BaseCalendarTemplate;
import model.calendar.CalendarEvent;
import model.oauth.OAuthProvider;
import model.outlook.*;
import play.Logger;
import play.db.Database;
import play.libs.Json;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static database.TablesContract._OAuthAccount.*;
import static database.TablesContract._OutlookCalendarTemplate.*;
import static database.TablesContract._User.USER_ID;
import static model.JsonConverter.getJsonArrayFromList;
import static model.JsonConverter.getStringListFromJsonArray;
import static model.JsonConverter.writeJsonArrayAsStringArray;

/**
 *
 */
public class OutlookDBAccessor extends CalendarDBAccessor {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    public OutlookDBAccessor(Database database) {
        super(database);
    }

    public boolean createOutlookAppointment(String userId, CalendarEvent event, String appAppointmentId) {
        return getDatabase().withConnection(connection -> {
            String sql = "INSERT INTO calendar_appointment_links(user_id, provider_appointment_id, " +
                    "app_appointment_id, hyper_link, subject, provider) VALUES (?, ?, ?, ?, ?, ?);";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, event.getEventId());
            statement.setString(3, appAppointmentId);
            statement.setString(4, event.getHyperLink());
            statement.setString(5, event.getSubject());
            statement.setString(6, OAuthProvider.OUTLOOK.getRawText());
            return statement.executeUpdate() == 1;
        });
    }

    /**
     * @param template The template with the ID set.
     * @return True if successful, false otherwise
     */
    public boolean createOutlookTemplate(CalendarTemplate template) {
        ObjectMapper objectMapper = new ObjectMapper();
        String categories;
        String defaultAttendees;
        try {
            categories = objectMapper.writeValueAsString(getJsonArrayFromList(template.getCategories()));
            defaultAttendees = objectMapper.writeValueAsString(getJsonArrayFromList(template.getDefaultEmails()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }

        if (template.getBody() != null && (template.getBody().contains("\n") || template.getBody().contains("\r"))) {
            return false;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "INSERT INTO outlook_calendar_template(oauth_account_id, template_id, default_attendees, " +
                    "body_text, categories, duration_in_minutes, importance, is_reminder_on, event_location, " +
                    "reminder_minutes_before_start, is_response_requested, sensitivity, show_as_status, subject," +
                    "template_name)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            statement = connection.prepareStatement(sql);
            statement.setString(1, template.getOauthAccountId());
            statement.setString(2, template.getTemplateId());
            statement.setString(3, defaultAttendees);
            statement.setString(4, template.getBody());
            statement.setString(5, categories);
            statement.setInt(6, template.getDurationInMinutes());
            statement.setString(7, template.getImportance().getRawText());
            statement.setBoolean(8, template.isReminderOn());
            statement.setString(9, template.getEventLocation());
            statement.setInt(10, template.getReminderMinutesBeforeStart());
            statement.setBoolean(11, template.isResponseRequested());
            statement.setString(12, template.getSensitivity().getRawText());
            statement.setString(13, template.getShowAsStatus().getRawText());
            statement.setString(14, template.getSubject());
            statement.setString(15, template.getTemplateName());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    /**
     * @param outlookAccountId The user's outlook account ID
     * @param template         The template with the ID set.
     * @return True if successful, false otherwise
     */
    public boolean editOutlookTemplate(String outlookAccountId, CalendarTemplate template) {
        String categories = writeJsonArrayAsStringArray(getJsonArrayFromList(template.getCategories()));
        String defaultAttendees = writeJsonArrayAsStringArray(getJsonArrayFromList(template.getDefaultEmails()));
        Connection connection = null;
        PreparedStatement statement = null;

        if (template.getBody() != null && (template.getBody().contains("\n") || template.getBody().contains("\r"))) {
            return false;
        }

        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE outlook_calendar_template SET template_name = ?, default_attendees = ?, body_text = ?, " +
                    "categories = ?, duration_in_minutes = ?, importance = ?, is_reminder_on = ?, event_location = ?, " +
                    "reminder_minutes_before_start = ?, is_response_requested = ?, sensitivity = ?, " +
                    "show_as_status = ?, subject = ? " +
                    "WHERE oauth_account_id = ? AND template_id = ?;";
            statement = connection.prepareStatement(sql);

            int counter = 1;
            statement.setString(counter++, template.getTemplateName());
            statement.setString(counter++, defaultAttendees);
            statement.setString(counter++, template.getBody());
            statement.setString(counter++, categories);
            statement.setInt(counter++, template.getDurationInMinutes());
            statement.setString(counter++, template.getImportance().getRawText());
            statement.setBoolean(counter++, template.isReminderOn());
            statement.setString(counter++, template.getEventLocation());
            statement.setInt(counter++, template.getReminderMinutesBeforeStart());
            statement.setBoolean(counter++, template.isResponseRequested());
            statement.setString(counter++, template.getSensitivity().getRawText());
            statement.setString(counter++, template.getShowAsStatus().getRawText());
            statement.setString(counter++, template.getSubject());
            statement.setString(counter++, outlookAccountId);
            statement.setString(counter, template.getTemplateId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public CalendarTemplate getUserCalendarTemplateById(String userId, String templateId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            // language=PostgreSQL
            String sql = getProjectionForCalendarTemplate() + " " +
                    "WHERE " + USER_ID + " = ? AND " + TEMPLATE_ID + " = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, templateId);
            resultSet = statement.executeQuery();
            List<CalendarTemplate> templateList = getCalendarTemplatesFromResultSet(resultSet);
            if (templateList != null && templateList.size() == 1) {
                return templateList.get(0);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    private String getProjectionForCalendarTemplate() {
        // language=PostgreSQL
        return "SELECT outlook_calendar_template.oauth_account_id as oauth_account_id, template_id, " +
                "template_name, date_created, default_attendees, body_text, categories, duration_in_minutes, " +
                "importance, is_reminder_on, event_location, reminder_minutes_before_start, is_response_requested," +
                "sensitivity, show_as_status, subject " +
                "FROM outlook_calendar_template JOIN oauth_account " +
                "ON outlook_calendar_template.oauth_account_id = oauth_account.oauth_account_id ";
    }

    private static List<CalendarTemplate> getCalendarTemplatesFromResultSet(ResultSet resultSet) throws SQLException {
        List<CalendarTemplate> templatesList = new ArrayList<>();
        while (resultSet.next()) {
            String outlookAccountId = resultSet.getString(OAUTH_ACCOUNT_ID);
            String templateId = resultSet.getString(TEMPLATE_ID);
            String templateName = resultSet.getString(TEMPLATE_NAME);
            long dateCreated = resultSet.getDate(DATE_CREATED).getTime();
            String attendeesJson = resultSet.getString(DEFAULT_ATTENDEES);
            List<String> defaultAttendees = getStringListFromJsonArray((ArrayNode) Json.parse(attendeesJson));
            String bodyText = resultSet.getString(BODY_TEXT);
            String categoriesJson = resultSet.getString(CATEGORIES);
            List<String> categories = getStringListFromJsonArray((ArrayNode) Json.parse(categoriesJson));
            int durationMinutes = resultSet.getInt(DURATION_IN_MINUTES);
            EventImportance importance = EventImportance.parse(resultSet.getString(IMPORTANCE));
            boolean isReminderOn = resultSet.getBoolean(IS_REMINDER_ON);
            String location = resultSet.getString(EVENT_LOCATION);
            int reminderMinutesBeforeStart = resultSet.getInt(REMINDER_MINUTES_BEFORE_START);
            boolean isResponseRequired = resultSet.getBoolean(IS_RESPONSE_REQUESTED);
            EventSensitivity sensitivity = EventSensitivity.parse(resultSet.getString(SENSITIVITY));
            EventPersonStatus status = EventPersonStatus.parse(resultSet.getString(SHOW_AS_STATUS));
            String subject = resultSet.getString(SUBJECT);

            templatesList.add(OutlookCalendarFactory.createTemplateFromValues(outlookAccountId, templateId,
                    templateName, dateCreated, bodyText, defaultAttendees, categories, durationMinutes, importance,
                    isReminderOn, location, reminderMinutesBeforeStart, isResponseRequired, sensitivity, status,
                    subject));
        }
        return templatesList;
    }

}
