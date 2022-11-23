https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import model.ControllerComponents;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A base controller that works well for injecting resources into the client.
 */
@SuppressWarnings("WeakerAccess")
public class BaseController extends Controller {

    public static final String APP_NAME = "Magic List";
    public static final String COMPANY_NAME = "Caplan Innovations LLC";

    public static final String KEY_ERROR = "error";
    public static final String KEY_SUCCESS = "success";

    public static final String KEY_PAGE = "page";
    public static final String KEY_SORT_BY = "sort_by";
    public static final String KEY_CONTINUE_URL = "continue_url";

    public static final String KEY_ASCENDING = "ascending";

    public static final String KEY_OUTLOOK_CLIENT_ID = "OUTLOOK_CLIENT_ID";
    public static final String KEY_OUTLOOK_PASSWORD = "OUTLOOK_PASSWORD";

    private static final String KEY_IS_BETA = "isBeta";
    public static boolean isSecure;

    public static Config configuration;
    public static Environment environment;

    private static Database database;
    private static WSClient wsClient;

    private static HttpExecutionContext httpExecutionContext;

    private String outlookClientId;
    private String outlookClientSecret;

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public BaseController(ControllerComponents controllerComponents) {
        database = controllerComponents.getDatabase();
        wsClient = controllerComponents.getWsClient();
        configuration = controllerComponents.getConfig();
        environment = controllerComponents.getEnvironment();
        httpExecutionContext = controllerComponents.getHttpExecutionContext();
        isSecure = controllerComponents.getEnvironment().isProd();

        outlookClientId = getString(KEY_OUTLOOK_CLIENT_ID);
        outlookClientSecret = getString(KEY_OUTLOOK_PASSWORD);
    }

    protected <U> CompletionStage<U> wrapInFuture(U value) {
        return CompletableFuture.completedFuture(value);
    }

    public static Database getDatabase() {
        return database;
    }

    public static WSClient getWsClient() {
        return wsClient;
    }

    public static HttpExecutionContext getHttpExecutionContext() {
        return httpExecutionContext;
    }

    public static Config getConfig() {
        return configuration;
    }

    public static Environment getEnvironment() {
        return environment;
    }

    public static Result sendJsonOk(String s) {
        String contentType = "application/json; charset=UTF-8";
        return ok(s, "UTF-8").as(contentType);
    }

    public static String getString(String key) {
        return getString(key, configuration, environment);
    }

    public static String getString(String key, Config config, Environment environment) {
        configuration = config;
        BaseController.environment = environment;

        if (environment.isProd() || environment.isTest()) {
            return System.getenv(key);
        } else {
//            return config.getString(key);
            return System.getenv(key);
        }
    }

    public static long getLong(String key, long defaultValue) {
        return getLong(key, defaultValue, configuration);
    }

    public static long getLong(String key, long defaultValue, Config config) {
        configuration = config;
        try {
            return config.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean isBeta() {
        return getConfig().getBoolean(KEY_IS_BETA);
    }

    public Result getRedirectFailure(String text) {
        return redirect(routes.UserController.operationFailure().url() + "?text=" + text);
    }

    public Result getRedirectSuccess(String text) {
        return redirect(routes.UserController.operationSuccess().url() + "?text=" + text);
    }

    public String getOutlookClientId() {
        return outlookClientId;
    }

    public String getOutlookClientSecret() {
        return outlookClientSecret;
    }

}
