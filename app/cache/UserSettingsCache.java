https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package cache;

import controllers.BaseController;
import model.account.Account;
import model.account.AccountSettings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by Corey Caplan on 10/19/17.
 */
public class UserSettingsCache {

    /**
     * @param userId           The user's ID
     * @param setting          The setting to retrieve
     * @param databaseFunction A function that takes the user's ID as a parameter and returns the type parameter as a result
     * @param <T>              The type of variable to return.
     * @return The setting of type T.
     */
    public static <T extends Serializable> T getConfiguration(String userId, AccountSettings setting, Function<String, T> databaseFunction) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        String key = getConfigurationHash(setting, userId);
        T t = wrapper.get(key);
        return Optional.ofNullable(t)
                .orElseGet(() -> {
                    T aNewT = databaseFunction.apply(userId);
                    wrapper.set(key, MemCacheWrapper.FIFTEEN_MINUTES, aNewT);
                    return aNewT;
                });
    }

    public static void removeAll(String userId) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        Arrays.stream(AccountSettings.values())
                .forEach(setting -> wrapper.removeAndBlock(getConfigurationHash(setting, userId)));
    }

    // Private Methods

    private static String getConfigurationHash(AccountSettings settings, String userId) {
        return settings.toDatabaseText() + userId;
    }

}
