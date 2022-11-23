https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import model.serialization.MagicListObject;

/**
 *
 */
public class DialSheetWithAppointmentWrapper extends MagicListObject {

    private final AllPagesDialSheet dialSheet;
    private final DialSheetAppointment appointment;

    public DialSheetWithAppointmentWrapper(AllPagesDialSheet dialSheet, DialSheetAppointment appointment) {
        this.dialSheet = dialSheet;
        this.appointment = appointment;
    }

    public AllPagesDialSheet getDialSheet() {
        return dialSheet;
    }

    public DialSheetAppointment getAppointment() {
        return appointment;
    }
}
