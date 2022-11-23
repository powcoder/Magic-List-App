https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.ControllerComponents;
import model.account.Account;
import play.*;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.Json;
import play.inject.Injector;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

/**
 *
 */
public class BaseTestController extends WithApplication {

    protected static final String USER_ID = "usr_e370892d269c786721548a698b07006f1fdcc096";
    protected static final String SEARCH_ID = "srch_c17194a19e00893c0298ecbf023688d1bf161645";
    protected static final String PERSON_ID = "z_cus_cb7d1be02d5090b4903a8ac4cb055f8c46673bdb";

    @Override
    protected play.Application provideApplication() {
        Map<String, Object> options = new HashMap<>();
        Application application = Helpers.fakeApplication(options);
        ControllerComponents controllerComponents = application.injector().instanceOf(ControllerComponents.class);
        new BaseController(controllerComponents);
        return application;
    }

    protected Account login() {
        ObjectNode objectNode = Json.newObject()
                .put(Account.EMAIL, "coreycaplan3@gmail.com")
                .put(Account.PASSWORD, "dopey1019");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/api/user/login")
                .bodyJson(objectNode);
        Result result = route(app, requestBuilder);

        assertEquals(200, result.status());

        String rawJson = contentAsString(result);
        objectNode = (ObjectNode) Json.parse(rawJson);
        return Account.getAccountConverter().deserializeFromJson(objectNode);
    }

}
