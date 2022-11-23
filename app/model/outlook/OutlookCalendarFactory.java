https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.outlook;

import model.account.Account;
import model.dialsheet.DialSheetAppointment;
import utilities.RandomStringGenerator;
import utilities.Validation;

import java.util.*;

import static model.calendar.BaseCalendarTemplate.KEY_TEMPLATE_NAME;
import static model.outlook.CalendarAppointment.*;

/**
 * Created by Corey on 4/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class OutlookCalendarFactory {

    public static CalendarTemplate createTemplateFromValues(String outlookAccountId, String templateId,
                                                            String templateName, long dateCreatedMillis, String body,
                                                            List<String> defaultEmails, List<String> categories,
                                                            int durationInMinutes, EventImportance importance,
                                                            boolean isReminderOn, String location,
                                                            int reminderMinutesBeforeStart, boolean isResponseRequested,
                                                            EventSensitivity sensitivity, EventPersonStatus showAsStatus,
                                                            String subject) {
        return new CalendarTemplate(outlookAccountId, templateId, templateName, dateCreatedMillis, body, defaultEmails, categories,
                durationInMinutes, importance, isReminderOn, location, reminderMinutesBeforeStart, isResponseRequested,
                sensitivity, showAsStatus, subject);
    }

    public static CalendarTemplate createTemplateFromForm(Map<String, String[]> form) throws Exception {
        String templateId = RandomStringGenerator.getInstance().getNextRandomTemplateId();

        return createTemplateFromForm(templateId, form);
    }

    public static CalendarTemplate createTemplateFromForm(String templateId, Map<String, String[]> form) throws Exception {
        String outlookAccountId = Validation.string(KEY_OUTLOOK_USER_ID, form);
        if(Validation.isEmpty(outlookAccountId)) {
            throw new IllegalArgumentException("Missing outlook account ID");
        }

        boolean isAttendanceRequired = Validation.bool(KEY_IS_RESPONSE_REQUIRED, form);
        List<String> defaultAttendees = new ArrayList<>();
        if (form.containsKey(KEY_ATTENDEES)) {
            String[] emailsArray = form.get(KEY_ATTENDEES);
            defaultAttendees.addAll(Arrays.asList(emailsArray));
        }

        String subject = Validation.string(KEY_SUBJECT, form);

        EventImportance importance = EventImportance.parse(Validation.string(KEY_IMPORTANCE, form));
        if (importance == EventImportance.INVALID) {
            throw new IllegalArgumentException("Invalid event importance");
        }

        EventSensitivity sensitivity = EventSensitivity.parse(Validation.string(KEY_SENSITIVITY, form));
        if (sensitivity == EventSensitivity.INVALID) {
            throw new IllegalArgumentException("Invalid event sensitivity");
        }

        int eventDuration = Validation.integer(KEY_DURATION_MINUTES, form);

        boolean isReminderOn = true;
        int reminderBeforeStart = Validation.integer(KEY_REMINDER_MINUTES_BEFORE_START, form);
        if (reminderBeforeStart == -1) {
            throw new IllegalArgumentException("Invalid reminder before start entered");
        } else if (reminderBeforeStart == -999) {
            isReminderOn = false;
        }

        EventPersonStatus status = EventPersonStatus.parse(Validation.string(KEY_SHOW_AS_STATUS, form));
        if (status == EventPersonStatus.UNKNOWN) {
            throw new IllegalArgumentException("Invalid \"show me as\" status entered");
        }

        String eventLocationName = Validation.string(KEY_LOCATION, form);

        List<String> categories = new ArrayList<>();
        if (form.containsKey(KEY_CATEGORIES)) {
            String[] categoriesArray = form.get(KEY_CATEGORIES);
            Collections.addAll(categories, categoriesArray);
        }

        String emailBody = Validation.string(KEY_BODY, form);

        String templateName = Validation.string(KEY_TEMPLATE_NAME, form);
        if (Validation.isEmpty(templateName)) {
            throw new IllegalArgumentException("The template\'s name cannot be empty");
        }

        long timeCreated = Calendar.getInstance().getTimeInMillis();

        return new CalendarTemplate(outlookAccountId, templateId, templateName, timeCreated, emailBody,
                defaultAttendees, categories, eventDuration, importance, isReminderOn, eventLocationName,
                reminderBeforeStart, isAttendanceRequired, sensitivity, status, subject);
    }

    public static CalendarAppointment createFromTemplate(CalendarTemplate template, Account account,
                                                         DialSheetAppointment appointment) {
        List<EventAttendee> attendees =
                convertRawEmailsToAttendeeList(template.getDefaultEmails(), template.isResponseRequested());
        return new CalendarAppointment(template.getBody(), attendees, template.getCategories(),
                template.getDurationInMinutes(), template.getImportance(), template.isReminderOn(),
                template.getEventLocation(), template.getReminderMinutesBeforeStart(),
                template.isResponseRequested(), template.getSensitivity(), template.getShowAsStatus(),
                template.getSubject(), account.getName(), account.getEmail(),
                appointment.getAppointmentDate(), appointment.getPerson());
    }

}
