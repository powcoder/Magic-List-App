https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Corey on 6/27/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ListUtility {

    @SafeVarargs
    public static <T> List<T> asList(T... args) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, args);
        return list;
    }

}
