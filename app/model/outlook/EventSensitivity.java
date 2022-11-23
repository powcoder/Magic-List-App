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
public enum EventSensitivity {

    NORMAL, PERSONAL, PRIVATE, CONFIDENTIAL, INVALID;

    private static final String KEY_NORMAL = "Normal";
    private static final String KEY_PERSONAL = "Personal";
    private static final String KEY_PRIVATE = "Private";
    private static final String KEY_CONFIDENTIAL = "Confidential";

    @Override
    public String toString() {
        if (this == NORMAL) {
            return "Normal";
        } else if (this == PERSONAL) {
            return "Personal";
        } else if (this == PRIVATE) {
            return "Private";
        } else if (this == CONFIDENTIAL) {
            return "Confidential";
        } else {
            System.err.println("Invalid sensitivity, found: " + this);
            return "Invalid";
        }
    }

    public String getRawText() {
        if (this == NORMAL) {
            return KEY_NORMAL;
        } else if (this == PERSONAL) {
            return KEY_PERSONAL;
        } else if (this == PRIVATE) {
            return KEY_PRIVATE;
        } else if (this == CONFIDENTIAL) {
            return KEY_CONFIDENTIAL;
        } else {
            System.err.println("Invalid sensitivity, found: " + this);
            return "Invalid";
        }
    }

    public static EventSensitivity parse(String rawEventSensitivity) {
        if (KEY_NORMAL.equalsIgnoreCase(rawEventSensitivity)) {
            return NORMAL;
        } else if (KEY_PERSONAL.equalsIgnoreCase(rawEventSensitivity)) {
            return PERSONAL;
        } else if (KEY_PRIVATE.equalsIgnoreCase(rawEventSensitivity)) {
            return PRIVATE;
        } else if (KEY_CONFIDENTIAL.equalsIgnoreCase(rawEventSensitivity)) {
            return CONFIDENTIAL;
        } else {
            System.err.println("Invalid sensitivity, found: " + rawEventSensitivity);
            return INVALID;
        }
    }

    public static List<EventSensitivity> getAll() {
        List<EventSensitivity> list = new ArrayList<>();
        list.add(NORMAL);
        list.add(PERSONAL);
        list.add(PRIVATE);
        list.add(CONFIDENTIAL);
        return list;
    }
}
