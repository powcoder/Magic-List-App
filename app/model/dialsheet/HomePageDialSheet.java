https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import akka.japi.Pair;
import model.prospect.ProspectState;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Corey Caplan on 9/12/17.
 */
public class HomePageDialSheet extends AllPagesDialSheet {

    private List<DialSheetContact> dialSheetActivity;
    private final int newAppointmentsCount;
    private final int otherAppointmentsCount;

    public HomePageDialSheet(DialSheet dialSheet, List<DialSheetContactType> contactTypeList,
                             int newAppointmentsCount, int otherAppointmentsCount,
                             List<DialSheetAppointment> appointmentList, List<DialSheetContact> dialSheetActivity) {
        super(dialSheet, contactTypeList, appointmentList);
        this.newAppointmentsCount = newAppointmentsCount;
        this.otherAppointmentsCount = otherAppointmentsCount;
        this.dialSheetActivity = dialSheetActivity;
    }

    public int getNewAppointmentsCount() {
        return newAppointmentsCount;
    }

    public int getOtherAppointmentsCount() {
        return otherAppointmentsCount;
    }

    public List<DialSheetContact> getDialSheetActivity() {
        return dialSheetActivity;
    }

    public int getFollowUpsCount() {
        return (int) dialSheetActivity.stream()
                .filter(contact -> contact.getState().isIndeterminate())
                .count();
    }

    public int getObjectionsCount() {
        return (int) dialSheetActivity.stream()
                .filter(contact -> contact.getState().isObjection())
                .count();
    }

}
