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
public enum EventPersonStatus {

    FREE, TENTATIVE, BUSY, OOF, WORKING_ELSEWHERE, UNKNOWN;

    private static final String KEY_FREE = "Free";
    private static final String KEY_TENTATIVE = "Tentative";
    private static final String KEY_BUSY = "Busy";
    private static final String KEY_OOF = "Oof";
    private static final String KEY_WORKING_ELSEWHERE = "WorkingElsewhere";

    @Override
    public String toString() {
        if (this == FREE) {
            return "Free";
        } else if (this == TENTATIVE) {
            return "Tentative";
        } else if (this == BUSY) {
            return "Busy";
        } else if (this == OOF) {
            return "Away";
        } else if (this == WORKING_ELSEWHERE) {
            return "Working Elsewhere";
        } else {
            System.err.println("Invalid status, found: " + this);
            return "Invalid";
        }
    }

    public String getRawText() {
        if (this == FREE) {
            return KEY_FREE;
        } else if (this == TENTATIVE) {
            return KEY_TENTATIVE;
        } else if (this == BUSY) {
            return KEY_BUSY;
        } else if (this == OOF) {
            return KEY_OOF;
        } else if (this == WORKING_ELSEWHERE) {
            return KEY_WORKING_ELSEWHERE;
        } else {
            System.err.println("Invalid status, found: " + this);
            return "invalid";
        }
    }

    public static EventPersonStatus parse(String rawEventPersonStatus) {
        if (KEY_FREE.equalsIgnoreCase(rawEventPersonStatus)) {
            return FREE;
        } else if (KEY_TENTATIVE.equalsIgnoreCase(rawEventPersonStatus)) {
            return TENTATIVE;
        } else if (KEY_BUSY.equalsIgnoreCase(rawEventPersonStatus)) {
            return BUSY;
        } else if (KEY_OOF.equalsIgnoreCase(rawEventPersonStatus)) {
            return OOF;
        } else if (KEY_WORKING_ELSEWHERE.equalsIgnoreCase(rawEventPersonStatus)) {
            return WORKING_ELSEWHERE;
        } else {
            System.err.println("Invalid person status, found: " + rawEventPersonStatus);
            return UNKNOWN;
        }
    }

    public static List<EventPersonStatus> getAll() {
        List<EventPersonStatus> list = new ArrayList<>();
        list.add(FREE);
        list.add(TENTATIVE);
        list.add(BUSY);
        list.add(OOF);
        list.add(WORKING_ELSEWHERE);
        return list;
    }
}
