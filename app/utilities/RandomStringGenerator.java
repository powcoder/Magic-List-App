https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * A singleton class used to randomly generate user IDs for the database, by calling {@link #getNextRandomString()}
 */
public class RandomStringGenerator {

    private SecureRandom random;

    private static RandomStringGenerator instance;

    private final char[] ARRAY;

    public static RandomStringGenerator getInstance() {
        if (instance == null) {
            instance = new RandomStringGenerator();
        }
        return instance;
    }

    private RandomStringGenerator() {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            // Request should fail since we cannot securely hash the information
            throw new RuntimeException(e);
        }

        ARRAY = new char[62];
        for (int i = 0; i < ARRAY.length; i++) {
            if (i < 26) {
                ARRAY[i] = (char) ('a' + i);
            } else if (i < 52) {
                ARRAY[i] = (char) ('A' + i - 26);
            } else {
                ARRAY[i] = (char) ('0' + i - 52);
            }
        }
    }

    public String getNextRandomBugReportId() {
        return "bug_" + getNextRandomString();
    }

    public String getNextRandomQuoteId() {
        return "quote_" + getNextRandomString();
    }

    public String getNextRandomSuggestionId() {
        return "sug_" + getNextRandomString();
    }

    public String getNextRandomTemplateId() {
        return "tmplt_" + getNextRandomString();
    }

    public String getNextRandomImporterToken() {
        return "imptr_" + getNextRandomString();
    }

    public String getNextRandomListId() {
        return "lst_" + getNextRandomString();
    }

    public String getNextRandomDialSheetId() {
        return "dial_" + getNextRandomString();
    }

    public String getNextRandomAppointmentId() {
        return "appt_" + getNextRandomString();
    }

    public String getNextRandomManagerRequestEmployeeId() {
        return "mgr_emp_" + getNextRandomString();
    }

    public String getNextRandomUserNotificationId() {
        return "usr_not_" + getNextRandomString();
    }

    public String getNextRandomNotificationId() {
        return "not_" + getNextRandomString();
    }

    public String getNotificationIdFromAppointmentId(String appointmentId) {
        return "not_" + appointmentId.substring("appt_".length());
    }

    public String getNextRandomPersonId() {
        return "per_" + getNextRandomString();
    }

    public String getNextRandomUserId() {
        return "usr_" + getNextRandomString();
    }

    public String getNextRandomEmailVerifier() {
        return "ema_" + getNextRandomString();
    }

    public String getNextRandomPasswordResetLink() {
        return "rst_" + getNextRandomString();
    }

    /**
     * @return A secure random string to be used for the user's login token.
     */
    public String getNextRandomToken() {
        return "tok_" + getNextRandomString();
    }

    /**
     * @return A secure random string to be used for inserting into the database.
     */
    private String getNextRandomString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            // Append the ARRAY's char at a random position
            builder.append(ARRAY[random.nextInt(ARRAY.length)]);
        }
        return builder.toString();
    }

    /**
     * @return A secure random string to be used for inserting into the database.
     */
    private String getNextRandomString() {
        return getNextRandomString(32);
    }

}
