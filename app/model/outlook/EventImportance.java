https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.outlook;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public enum EventImportance {

    LOW, NORMAL, HIGH, INVALID;

    private static final String KEY_LOW = "Low";
    private static final String KEY_NORMAL = "Normal";
    private static final String KEY_HIGH = "High";

    @Override
    public String toString() {
        if (this == LOW) {
            return "Low";
        } else if (this == NORMAL) {
            return "Normal";
        } else if (this == HIGH) {
            return "High";
        } else {
            System.err.println("Invalid importance, found: " + this);
            return "Invalid importance";
        }
    }

    public String getRawText() {
        if (this == LOW) {
            return KEY_LOW;
        } else if (this == NORMAL) {
            return KEY_NORMAL;
        } else if (this == HIGH) {
            return KEY_HIGH;
        } else {
            System.err.println("Invalid importance, found: " + this);
            return "Invalid importance";
        }
    }

    public static EventImportance parse(String rawEventImportance) {
        if (KEY_LOW.equalsIgnoreCase(rawEventImportance)) {
            return LOW;
        } else if (KEY_NORMAL.equalsIgnoreCase(rawEventImportance)) {
            return NORMAL;
        } else if (KEY_HIGH.equalsIgnoreCase(rawEventImportance)) {
            return HIGH;
        } else {
            System.err.println("Invalid importance, found: " + rawEventImportance);
            return INVALID;
        }
    }

    public static List<EventImportance> getAll() {
        List<EventImportance> list = new ArrayList<>();
        list.add(LOW);
        list.add(NORMAL);
        list.add(HIGH);
        return list;
    }

}
