https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.calendar;

import java.util.Arrays;
import java.util.Calendar;

public enum CalendarProvider {

    OUTLOOK, GOOGLE, INVALID;

    @Override
    public String toString() {
        return super.toString().substring(0, 1) + super.toString().substring(1).toLowerCase();
    }

    public String getRawText() {
        return this.toString().toLowerCase();
    }

    public static CalendarProvider parse(String rawText) {
        return Arrays.stream(CalendarProvider.values())
                .filter(provider -> provider.getRawText().equalsIgnoreCase(rawText))
                .findFirst()
                .orElse(null);

    }

}
