https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import play.Environment;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;

import javax.inject.Inject;

/**
 * Created by Corey Caplan on 8/30/17.
 */
public class ControllerComponents {

    private final SyncCacheApi cache;
    private final Database database;
    private final WSClient wsClient;
    private final Config config;
    private final Environment environment;
    private final HttpExecutionContext httpExecutionContext;
    private final ActorSystem actorSystem;

    @Inject
    public ControllerComponents(SyncCacheApi cache, Database database, WSClient wsClient, Config config,
                                Environment environment, HttpExecutionContext httpExecutionContext,
                                ActorSystem actorSystem) {
        this.cache = cache;
        this.database = database;
        this.wsClient = wsClient;
        this.config = config;
        this.environment = environment;
        this.httpExecutionContext = httpExecutionContext;
        this.actorSystem = actorSystem;
    }

    public SyncCacheApi getCache() {
        return cache;
    }

    public Database getDatabase() {
        return database;
    }

    public WSClient getWsClient() {
        return wsClient;
    }

    public Config getConfig() {
        return config;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public HttpExecutionContext getHttpExecutionContext() {
        return httpExecutionContext;
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

}
