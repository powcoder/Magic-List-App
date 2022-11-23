https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Corey on 6/12/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerDialSheet {

    private final long date;
    private final int dialCount;
    private final int appointmentCount;
    private final int contactsCount;

    public ManagerDialSheet(long date, int dialCount, int contactsCount, int appointmentCount) {
        this.date = date;
        this.dialCount = dialCount;
        this.contactsCount = contactsCount;
        this.appointmentCount = appointmentCount;
    }

    public long getDate() {
        return date;
    }

    public int getDialCount() {
        return dialCount;
    }

    public int getAppointmentCount() {
        return appointmentCount;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public String getDateForHref() {
        return new SimpleDateFormat("MM-dd-yyyy")
                .format(new Date(date));
    }

}
