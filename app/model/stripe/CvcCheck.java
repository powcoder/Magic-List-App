https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.stripe;

/**
 *
 */
public enum CvcCheck {

    PASS, FAIL, UNAVAILABLE, UNCHECKED, UNKNOWN;

    public static final String CVC_PASS = "pass";
    private static final String CVC_FAIL = "fail";
    private static final String CVC_UNAVAILABLE = "unavailable";
    private static final String CVC_UNCHECKED = "unchecked";

    public static CvcCheck createFromStripeApi(String cvcCheck) {
        if (CVC_PASS.equalsIgnoreCase(cvcCheck)) {
            return PASS;
        } else if (CVC_FAIL.equalsIgnoreCase(cvcCheck)) {
            return FAIL;
        } else if (CVC_UNAVAILABLE.equalsIgnoreCase(cvcCheck)) {
            return UNAVAILABLE;
        } else if (CVC_UNCHECKED.equalsIgnoreCase(cvcCheck)) {
            return UNCHECKED;
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        if (this == PASS) {
            return "Passed";
        } else if (this == FAIL) {
            return "Failed";
        } else if (this == UNAVAILABLE) {
            return "Not available";
        } else if (this == UNCHECKED) {
            return "Not checked yet";
        } else {
            return "Unknown";
        }
    }
}
