https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.outlook;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import model.calendar.BaseCalendarTemplate;
import model.calendar.CalendarProvider;
import model.dialsheet.DialSheetAppointment;
import model.prospect.Prospect;
import play.libs.Json;
import utilities.DateUtility;

import java.util.*;

import static model.outlook.CalendarAppointment.*;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class CalendarTemplate extends BaseCalendarTemplate {

    public static final String VARIABLE_PROSPECT_NAME = "PROSPECT_NAME";
    public static final String VARIABLE_PROSPECT_EMAIL = "PROSPECT_EMAIL";
    public static final String VARIABLE_PROSPECT_PHONE = "PROSPECT_PHONE";
    public static final String VARIABLE_PROSPECT_JOB_TITLE = "PROSPECT_JOB_TITLE";
    public static final String VARIABLE_PROSPECT_COMPANY = "PROSPECT_COMPANY";
    public static final String VARIABLE_APPOINTMENT_DATE = "APPOINTMENT_DATE";

    private String body;
    private final List<String> defaultEmails;
    private final List<String> categories;
    private final int durationInMinutes;
    private final EventImportance importance;
    private final boolean isReminderOn;
    private String eventLocation;
    private final int reminderMinutesBeforeStart;
    private final boolean isResponseRequested;
    private final EventSensitivity sensitivity;
    private final EventPersonStatus showAsStatus;
    private String subject;

    CalendarTemplate(String outlookAccountId, String templateId, String templateName, long dateTimeCreated, String body,
                     List<String> defaultEmails, List<String> categories, int durationInMinutes,
                     EventImportance importance, boolean isReminderOn, String eventLocation,
                     int reminderMinutesBeforeStart, boolean isResponseRequested, EventSensitivity sensitivity,
                     EventPersonStatus showAsStatus, String subject) {
        super(outlookAccountId, templateId, templateName, dateTimeCreated, CalendarProvider.OUTLOOK);
        this.body = body;
        this.defaultEmails = defaultEmails;
        this.categories = categories;
        this.durationInMinutes = durationInMinutes;
        this.importance = importance;
        this.isReminderOn = isReminderOn;
        this.eventLocation = eventLocation;
        this.reminderMinutesBeforeStart = reminderMinutesBeforeStart;
        this.isResponseRequested = isResponseRequested;
        this.sensitivity = sensitivity;
        this.showAsStatus = showAsStatus;
        this.subject = subject;

        if (this.body != null) {
            this.body = this.body.replace("\n", "");
            this.body = this.body.replace("\r", "");
            this.body = this.body.replace("<script>", "");
            this.body = this.body.replace("</script>", "");
        }
    }

    public String getBody() {
        return body;
    }

    public List<String> getDefaultEmails() {
        return defaultEmails;
    }

    public List<String> getCategories() {
        return categories;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public EventImportance getImportance() {
        return importance;
    }

    public boolean isReminderOn() {
        return isReminderOn;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public int getReminderMinutesBeforeStart() {
        return reminderMinutesBeforeStart;
    }

    public boolean isResponseRequested() {
        return isResponseRequested;
    }

    public EventSensitivity getSensitivity() {
        return sensitivity;
    }

    public EventPersonStatus getShowAsStatus() {
        return showAsStatus;
    }

    public String getSubject() {
        return subject;
    }

    public void replaceVariablesWithAppointmentInfo(DialSheetAppointment appointment) {
        body = replaceVariablesWithValues(body, appointment);
        subject = replaceVariablesWithValues(subject, appointment);
        eventLocation = replaceVariablesWithValues(eventLocation, appointment);
    }

    private String replaceVariablesWithValues(String text, DialSheetAppointment appointment) {
        if (text == null) {
            return null;
        }
        Prospect prospect = appointment.getPerson();

        if (prospect.getName() != null) {
            text = text.replace(VARIABLE_PROSPECT_NAME, prospect.getName());
        }
        if (appointment.getPerson().getEmail() != null) {
            text = text.replace(VARIABLE_PROSPECT_EMAIL, prospect.getEmail());
        }
        if (prospect.getPhoneNumber() != null) {
            text = text.replace(VARIABLE_PROSPECT_PHONE, prospect.getPhoneNumber());
        }
        if (prospect.getCompanyName() != null) {
            text = text.replace(VARIABLE_PROSPECT_COMPANY, prospect.getCompanyName());
        }
        if (prospect.getJobTitle() != null) {
            text = text.replace(VARIABLE_PROSPECT_JOB_TITLE, prospect.getJobTitle());
        }

        text = text.replace(VARIABLE_APPOINTMENT_DATE, DateUtility.getDateWithTimeForUi(appointment.getAppointmentDate()));

        return text;
    }

    @Override
    public String toString() {
        return new Converter().renderAsJsonObject(this).toString();
    }

    public static class Converter implements JsonConverter<CalendarTemplate> {

        @Override
        public ObjectNode renderAsJsonObject(CalendarTemplate object) {
            ObjectNode baseNode = Json.newObject();

            ObjectNode bodyNode = Json.newObject()
                    .put("content", object.body)
                    .put("contentType", "HTML");
            baseNode.set(KEY_BODY, bodyNode);

            ArrayNode categoriesArray = JsonConverter.getJsonArrayFromList(object.categories);
            baseNode.set(KEY_CATEGORIES, categoriesArray);

            ArrayNode attendeesNode = JsonConverter.getJsonArrayFromList(object.defaultEmails);
            baseNode.set(KEY_ATTENDEES, attendeesNode);

            String importance = object.importance.getRawText();
            baseNode.put(KEY_IMPORTANCE, importance);

            boolean isReminderOn = object.isReminderOn;
            baseNode.put(KEY_IS_REMINDER_ON, isReminderOn);

            ObjectNode locationNode = Json.newObject()
                    .put("displayName", object.eventLocation)
                    .putNull("address")
                    .putNull("locationEmailAddress");
            baseNode.set(KEY_LOCATION, locationNode);

            baseNode.put(KEY_REMINDER_MINUTES_BEFORE_START, object.reminderMinutesBeforeStart);

            baseNode.put(KEY_SENSITIVITY, object.sensitivity.getRawText());

            baseNode.put(KEY_SHOW_AS_STATUS, object.showAsStatus.getRawText());

            baseNode.put(KEY_SUBJECT, object.subject);

            return baseNode;
        }

        @Override
        public CalendarTemplate deserializeFromJson(ObjectNode objectNode) {
            System.err.println("STUB CODE CALLED");
            return null;
        }

    }

}
