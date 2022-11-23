https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.prospect;

import akka.japi.Pair;
import model.calendar.CalendarEvent;
import model.dialsheet.DialSheetAppointment;
import model.user.User;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Corey on 3/17/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class Appointment extends DialSheetAppointment {

    public enum Sorter {

        APPOINTMENT_DATE, PERSON_NAME, COMPANY_NAME, AREA_CODE;

        public String getRawText() {
            return this.toString().toLowerCase();
        }

        public String toUiString() {
            String[] array = this.toString()
                    .split("_+");

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                builder.append(array[i].substring(0, 1))
                        .append(array[i].substring(1).toLowerCase());
                if (i < array.length - 1) {
                    builder.append(" ");
                }
            }

            return builder.toString();
        }

        public static Sorter parse(String text) {
            return Arrays.stream(Sorter.values())
                    .filter(sorter -> sorter.getRawText().equals(text))
                    .findFirst()
                    .orElse(APPOINTMENT_DATE);
        }

    }

    private final List<CalendarEvent> calendarEvents;
    private final long lastContacted;
    private final User creator;

    public Appointment(DialSheetAppointment d, List<CalendarEvent> calendarEvents, long lastContacted, User creator) {
        super(d.getAppointmentId(), d.isConferenceCall(), d.getAppointmentDate(), d.getPerson(), d.getNotes(),
                d.getAppointmentType(), d.getAppointmentOutcome());
        this.calendarEvents = calendarEvents;
        this.lastContacted = lastContacted;
        this.creator = creator;
    }

    public List<CalendarEvent> getCalendarEvents() {
        return calendarEvents;
    }

    public long getLastContacted() {
        return lastContacted;
    }

    public User getCreator() {
        return creator;
    }

}
