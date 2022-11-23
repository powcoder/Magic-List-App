https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import controllers.BaseController;

import java.util.Map;

/**
 *
 */
public final class DebugUtility {

    public static void printFormContent(Map<String, String[]> form) {
        if (BaseController.getEnvironment() != null && BaseController.getEnvironment().isProd()) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        form.forEach((key, value) -> {
            stringBuilder.append("\"")
                    .append(key)
                    .append("\"")
                    .append(" : ");

            stringBuilder.append("[");
            for (int i = 0; i < value.length; i++) {
                stringBuilder.append(value[i]);
                if (i < value.length - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("]")
                    .append("\n");
        });
        stringBuilder.append("}");
        System.out.println("Form Content = " + stringBuilder.toString());
    }

}
