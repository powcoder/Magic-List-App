https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import com.google.gson.annotations.SerializedName;
import model.serialization.MagicListObject;

import java.io.Serializable;
import java.text.NumberFormat;

public class DialSheet extends MagicListObject implements Serializable {

    private static final long serialVersionUID = 174833213434L;

    public static final String KEY_ID = "dial_sheet_id";
    public static final String KEY_DIAL_COUNT = "dial_count";

    @SerializedName(KEY_ID)
    String id;
    final int dialCount;
    final long date;
    final int contactsCount;
    final int appointmentsCount;

    public DialSheet(String id, int dialCount, long date, int contactsCount, int appointmentsCount) {
        this.id = id;
        this.dialCount = dialCount;
        this.date = date;
        this.contactsCount = contactsCount;
        this.appointmentsCount = appointmentsCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDialCount() {
        return dialCount;
    }

    public String getDialCountForUi() {
        return NumberFormat.getInstance().format(dialCount);
    }

    public long getDate() {
        return date;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public int getAppointmentsCount() {
        return appointmentsCount;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof DialSheet && id.equals(((DialSheet) object).id);
    }

}
