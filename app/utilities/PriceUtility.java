https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import java.text.NumberFormat;

public class PriceUtility {

    public static String getPriceFromStripe(long price) {
        return NumberFormat.getCurrencyInstance()
                .format(price / 100.0);
    }

}
