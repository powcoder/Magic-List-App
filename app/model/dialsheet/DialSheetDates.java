https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import java.util.Date;
import java.util.List;

/**
 * Created by Corey Caplan on 9/2/17.
 */
public class DialSheetDates {

    private final int year;
    private final List<Date> months;

    public DialSheetDates(int year, List<Date> months) {
        this.year = year;
        this.months = months;
    }

    public int getYear() {
        return year;
    }

    public List<Date> getMonths() {
        return months;
    }

}
