https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.outlook;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import model.prospect.Prospect;
import play.libs.Json;
import utilities.DateUtility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static model.JsonConverter.getStringListFromJsonArray;

/**
 * Created by Corey on 4/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class CalendarAppointment {

    /* These keys correspond to the OUTLOOK API */

    public static final String KEY_OUTLOOK_USER_ID = "outlook_user_id";
    public static final String KEY_ATTENDEES = "attendees";
    public static final String KEY_BODY = "body";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_DURATION_MINUTES = "duration_in_minutes";
    public static final String KEY_IMPORTANCE = "importance";
    public static final String KEY_IS_REMINDER_ON = "isReminderOn";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_REMINDER_MINUTES_BEFORE_START = "reminderMinutesBeforeStart";
    public static final String KEY_IS_RESPONSE_REQUIRED = "responseRequested";
    public static final String KEY_SENSITIVITY = "sensitivity";
    public static final String KEY_SHOW_AS_STATUS = "showAs";
    public static final String KEY_SUBJECT = "subject";

    public static final String KEY_END_TIME = "end";
    public static final String KEY_IS_ORGANIZER = "isOrganizer";
    public static final String KEY_ORGANIZER = "organizer";
    public static final String KEY_START_TIME = "start";

    public static final String KEY_OUTLOOK_EVENT_ID = "id";
    public static final String KEY_WEB_LINK = "webLink";

    private static final String KEY_ORIGIN_START_TIME_ZONE = "originalStartTimeZone";

    private final String body;
    private final List<EventAttendee> attendeeList;
    private final List<String> categories;
    private final int durationInMinutes;
    private final EventImportance importance;
    private final boolean isReminderOn;
    private final String eventLocation;
    private final int reminderMinutesBeforeStart;
    private final boolean isResponseRequested;
    private final EventSensitivity sensitivity;
    private final EventPersonStatus showAsStatus;
    private final String subject;
    private final long startTimeInMillis;
    private final String organizerName;
    private final String organizerEmailAddress;
    private final Prospect appointmentAttendee;
    private String hyperLink = null;
    private String outlookEventId = null;

    static List<EventAttendee> convertRawEmailsToAttendeeList(List<String> emails, boolean isResponseRequested) {
        List<EventAttendee> attendees = new ArrayList<>();
        for (String email : emails) {
            attendees.add(new EventAttendee(email, isResponseRequested));
        }
        return attendees;
    }

    CalendarAppointment(String body, List<EventAttendee> attendeeList, List<String> categories, int durationInMinutes,
                        EventImportance importance, boolean isReminderOn, String eventLocation,
                        int reminderMinutesBeforeStart, boolean isResponseRequested, EventSensitivity sensitivity,
                        EventPersonStatus showAsStatus, String subject, String organizerName,
                        String organizerEmailAddress, long startTimeInMillis, Prospect appointmentAttendee) {
        this.body = body;
        this.attendeeList = attendeeList;
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
        this.startTimeInMillis = startTimeInMillis;
        this.organizerName = organizerName;
        this.organizerEmailAddress = organizerEmailAddress;
        this.appointmentAttendee = appointmentAttendee;
    }

    public String getBody() {
        return body;
    }

    public List<EventAttendee> getAttendeeList() {
        return attendeeList;
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

    public long getStartTimeInMillis() {
        return startTimeInMillis;
    }

    public Prospect getAppointmentAttendee() {
        return appointmentAttendee;
    }

    public String getHyperLink() {
        return hyperLink;
    }

    public String getOutlookEventId() {
        return outlookEventId;
    }

    public String toString(String outlookAccountId) {
        return new Converter(outlookAccountId).renderAsJsonObject(this).toString();
    }

    public static class Converter implements JsonConverter<CalendarAppointment> {

        private final String outlookAccountId;

        public Converter(String outlookAccountId) {
            this.outlookAccountId = outlookAccountId;
        }

        /**
         * Method to be called when creating a JSON object from an Outlook REST call
         */
        @Override
        public ObjectNode renderAsJsonObject(CalendarAppointment object) {
            ObjectNode baseNode = renderAsJsonObjectFromTemplate(object);

            // over write (possibly bad) values
            baseNode.set(KEY_ATTENDEES, new EventAttendee.Converter().renderAsJsonArray(object.attendeeList));

            ObjectNode bodyNode = Json.newObject()
                    .put("content", object.body)
                    .put("contentType", "HTML");

            baseNode.set(KEY_BODY, bodyNode);

            baseNode.put(KEY_SUBJECT, object.subject);

            return baseNode;
        }

        /**
         * Method to be called when creating a JSON object from a template and the object is to be sent to Outlook
         * <b>for the first time!!!</b>
         */
        private ObjectNode renderAsJsonObjectFromTemplate(CalendarAppointment object) {
            long startTimeInMillis = object.startTimeInMillis;
            long endTimeInMillis = startTimeInMillis + (1000 * 60 * object.getDurationInMinutes());

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

            String startTimeIso = dateFormat.format(new Date(startTimeInMillis));
            String endTimeIso = dateFormat.format(new Date(endTimeInMillis));

            ObjectNode baseNode = Json.newObject();

            ArrayNode categoriesArray = JsonConverter.getJsonArrayFromList(object.categories);
            baseNode.set(KEY_CATEGORIES, categoriesArray);

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

            baseNode.put(KEY_ORIGIN_START_TIME_ZONE, "America/New_York");

            String body = object.getBody();

            ObjectNode bodyNode = Json.newObject()
                    .put("content", body)
                    .put("contentType", "HTML");

            baseNode.set(KEY_BODY, bodyNode);

            ObjectNode endTimeNode = Json.newObject()
                    .put("dateTime", endTimeIso)
                    .put("timeZone", "America/New_York");

            ObjectNode startTimeNode = Json.newObject()
                    .put("dateTime", startTimeIso)
                    .put("timeZone", "America/New_York");

            baseNode.put("type", "SingleInstance");
            baseNode.set(KEY_END_TIME, endTimeNode);
            baseNode.set(KEY_START_TIME, startTimeNode);

            baseNode.put(KEY_IS_ORGANIZER, true);

            ObjectNode emailNode = Json.newObject()
                    .put("address", object.organizerEmailAddress)
                    .put("name", object.organizerName);

            ObjectNode organizerNode = Json.newObject();
            organizerNode.set("emailAddress", emailNode);
            baseNode.set(KEY_ORGANIZER, organizerNode);

            baseNode.put(KEY_SUBJECT, object.getSubject());

            return baseNode;
        }

        @Override
        public CalendarAppointment deserializeFromJson(ObjectNode objectNode) {
            List<EventAttendee> attendeeList = new EventAttendee.Converter()
                    .deserializeFromJsonArray((ArrayNode) objectNode.get(KEY_ATTENDEES));

            List<String> categories = getStringListFromJsonArray((ArrayNode) objectNode.get(KEY_CATEGORIES));

            long startTime = DateUtility.parseIsoDate(objectNode.get(KEY_START_TIME).asText());

            long endTime = DateUtility.parseIsoDate(objectNode.get(KEY_END_TIME).asText());

            EventImportance importance = EventImportance.parse(objectNode.get(KEY_IMPORTANCE).asText());

            boolean isReminderOn = objectNode.get(KEY_IS_REMINDER_ON).asBoolean();

            String location = objectNode.get(KEY_LOCATION).get("displayName").asText();

            int reminderMinutesBeforeStart = -999;
            if (isReminderOn) {
                reminderMinutesBeforeStart = objectNode.get(KEY_REMINDER_MINUTES_BEFORE_START).asInt();
            }

            boolean isResponseRequested = objectNode.get(KEY_IS_RESPONSE_REQUIRED).asBoolean();

            EventSensitivity sensitivity = EventSensitivity.parse(objectNode.get(KEY_SENSITIVITY).asText());

            EventPersonStatus showAsStatus = EventPersonStatus.parse(objectNode.get(KEY_SHOW_AS_STATUS).asText());

            String subject = objectNode.get(KEY_SUBJECT).asText();

            String organizerName = objectNode.get(KEY_ORGANIZER).get("name").asText();
            String organizerEmailAddress = objectNode.get(KEY_ORGANIZER).get("address").asText();

            int durationMinutes = (int) ((endTime - startTime) / 1000 / 60);

            CalendarAppointment appointment = new CalendarAppointment(outlookAccountId, attendeeList, categories, durationMinutes, importance,
                    isReminderOn, location, reminderMinutesBeforeStart, isResponseRequested, sensitivity, showAsStatus,
                    subject, organizerName, organizerEmailAddress, startTime, null);

            appointment.hyperLink = objectNode.get(KEY_WEB_LINK).asText();
            appointment.outlookEventId = objectNode.get(KEY_OUTLOOK_EVENT_ID).asText();

            return appointment;
        }
    }

}
