https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Corey Caplan on 10/12/17.
 */
public class StringUtility {

    // language=regex
    public static final String EMAIL_REGEX = "^(?:[a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
    public static final String EMAIL_GROUPING_REGEX = "(?:[a-zA-Z0-9_\\-\\.]+)@(?:[a-zA-Z0-9_\\-\\.]+)\\.(?:[a-zA-Z]{2,5})";

    public static String makeString(String separator, String[] list) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(list).forEach(each -> builder.append(each).append(separator));
        return builder.toString();
    }

    public static String makeString(String separator, List<String> list) {
        StringBuilder builder = new StringBuilder();
        list.forEach(each -> builder.append(each).append(separator));
        return builder.toString();
    }

}
