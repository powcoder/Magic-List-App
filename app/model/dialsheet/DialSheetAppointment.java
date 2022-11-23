https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import akka.japi.Pair;
import com.google.gson.annotations.SerializedName;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DialSheetAppointment extends MagicListObject implements Serializable {

    private static final Logger.ALogger logger = Logger.of(DialSheetAppointment.class);

    public static final String KEY_ID = "appointment_id";
    public static final String KEY_APPOINTMENT_DATE = "appointment_date";
    public static final String KEY_IS_CONFERENCE_CALL = "is_conference_call";
    public static final String KEY_APPOINTMENT_TYPE = "appointment_type";

    private static final long serialVersionUID = 174833219543904L;

    private static final long ONE_HOUR_MILLIS = 1000 * 60 * 60;

    private final String appointmentId;

    @SerializedName(KEY_IS_CONFERENCE_CALL)
    private boolean isConferenceCall;
    private final long appointmentDate;
    private Prospect person;
    private final String notes;
    private final ProspectState appointmentType;
    private final ProspectState appointmentOutcome;

    public static DialSheetAppointment getDummy() {
        long startTime = Calendar.getInstance().getTimeInMillis() + ONE_HOUR_MILLIS;
        Prospect prospect = Prospect.Factory.createFromDatabase("abc123", "John Smith",
                "john.smith@xyz.com", "(123) 123-1234", "Manager",
                "XYZ Company", ProspectState.NOT_CONTACTED, null, -1L);
        return new DialSheetAppointment("abc123", true, startTime, prospect, null,
                null);
    }

    public DialSheetAppointment(String appointmentId, boolean isConferenceCall, long appointmentDate, Prospect person,
                                String notes, ProspectState appointmentType) {
        this(appointmentId, isConferenceCall, appointmentDate, person, notes, appointmentType, ProspectState.NOT_CONTACTED);
    }

    public DialSheetAppointment(String appointmentId, boolean isConferenceCall, long appointmentDate, Prospect person,
                                String notes, ProspectState appointmentType, ProspectState appointmentOutcome) {
        this.appointmentId = appointmentId;
        this.isConferenceCall = isConferenceCall;
        this.appointmentDate = appointmentDate;
        this.person = person;
        this.notes = notes;
        this.appointmentType = appointmentType;
        this.appointmentOutcome = appointmentOutcome;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public boolean isConferenceCall() {
        return isConferenceCall;
    }

    public String getLocation() {
        return isConferenceCall ? "Conference Call" : "In Person";
    }

    public long getAppointmentDate() {
        return appointmentDate;
    }

    public Prospect getPerson() {
        return person;
    }

    public String getNotes() {
        return notes;
    }

    public ProspectState getAppointmentType() {
        return appointmentType;
    }

    public ProspectState getAppointmentOutcome() {
        return appointmentOutcome;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DialSheetAppointment && ((DialSheetAppointment) obj).appointmentId.equals(appointmentId);
    }

    /**
     * @return A list of appointment pairs that maps the appointment type (in order) to its list of appointments
     */
    public static <T extends DialSheetAppointment> List<Pair<ProspectState, List<T>>> getAppointmentsByType(List<T> appointmentList) {
        Map<ProspectState, List<T>> map = appointmentList.stream()
                .collect(Collectors.groupingBy(DialSheetAppointment::getAppointmentType,
                        (Supplier<Map<ProspectState, List<T>>>) HashMap::new,
                        Collectors.toList()));

        map.keySet().forEach(state -> logger.debug("State: {}", state.getStateType()));

        return map.entrySet()
                .stream()
                .map(prospectStateListEntry -> new Pair<>(prospectStateListEntry.getKey(), prospectStateListEntry.getValue()))
                .sorted(Comparator.comparingInt(pair -> pair.first().getIndex()))
                .collect(Collectors.toList());
    }

}
