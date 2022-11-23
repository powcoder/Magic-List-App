https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

/**
 * Created by Corey Caplan on 10/14/17.
 */
public enum DialSheetType {

    DAY, WEEK, MONTH, QUARTER, YEAR, CENTURY;

    public String getValue() {
        return this.toString().toLowerCase();
    }

}
