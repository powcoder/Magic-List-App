https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Corey on 3/16/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class AllPagesDialSheet extends DialSheet implements Serializable {

    private static final long serialVersionUID = 4327834215434L;

    @SerializedName("contact_types")
    private final List<DialSheetContactType> contactTypeList;
    @SerializedName("appointments")
    private List<DialSheetAppointment> appointmentList;

    public AllPagesDialSheet(DialSheet dialSheet, List<DialSheetContactType> contactTypeList,
                             List<DialSheetAppointment> appointmentList) {
        super(dialSheet.id, dialSheet.dialCount, dialSheet.date, dialSheet.contactsCount, dialSheet.appointmentsCount);
        this.contactTypeList = contactTypeList;
        this.appointmentList = appointmentList;
    }

    public List<DialSheetContactType> getContactTypeList() {
        return contactTypeList;
    }

    public List<DialSheetAppointment> getAppointmentList() {
        return appointmentList;
    }

    public int getAppointmentsCount() {
        return appointmentList.size();
    }

}
