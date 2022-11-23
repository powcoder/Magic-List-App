https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import utilities.Validation;

/**
 * Created by Corey on 6/21/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public enum GraphOutputDateFormat {

    /**
     * Data to be shown as days, formatted as "3/27/2017", "10/19/2017", etc.
     */
    DAYS,

    /**
     * Data to be shown as weeks, formatted as "Week of 6/11", "Week of 10/15", etc.
     */
    WEEKS,

    /**
     * Data to be shown as months, formatted as "January", "February", etc.
     */
    MONTHS,

    /**
     * Data to be shown as a quarters, formatted as "1st qr.", "2nd qr.", etc.
     */
    QUARTERS,

    /**
     * Data to be shown as years, formatted as "2017", "2018", etc.
     */
    YEARS;

    public static GraphOutputDateFormat parse(String s) {
        if ("DAYS".equalsIgnoreCase(s)) {
            return DAYS;
        } else if ("WEEKS".equalsIgnoreCase(s)) {
            return WEEKS;
        } else if ("MONTHS".equalsIgnoreCase(s)) {
            return MONTHS;
        } else if ("QUARTERS".equalsIgnoreCase(s)) {
            return QUARTERS;
        } else if ("YEARS".equalsIgnoreCase(s)) {
            return YEARS;
        } else {
            return null;
        }
    }

}
