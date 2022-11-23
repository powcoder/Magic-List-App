https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import actions.StripeWebhook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import database.PlanDBAccessor;
import com.typesafe.config.Config;
import model.ControllerComponents;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import utilities.ResultUtility;

import javax.inject.Inject;
import java.util.Arrays;

public class StripeWebHookController extends BaseController {

    private static final String TYPE_SUBSCRIPTION_CREATED = "customer.subscription.created";
    private static final String TYPE_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
    private static final String TYPE_SUBSCRIPTION_DELETED = "customer.subscription.deleted";
    private static final String[] TYPE_SUBSCRIPTION_CHANGES = {TYPE_SUBSCRIPTION_CREATED, TYPE_SUBSCRIPTION_UPDATED, TYPE_SUBSCRIPTION_DELETED};

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public StripeWebHookController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    @StripeWebhook.Authenticated(key = "STRIPE_KEY_ON_SUBSCRIPTION_CHANGE")
    @BodyParser.Of(BodyParser.Raw.class)
    public Result onSubscriptionChange() throws JsonProcessingException {
        Event event = (Event) Http.Context.current().args.get(StripeWebhook.KEY_EVENT);

        if (Arrays.stream(TYPE_SUBSCRIPTION_CHANGES).noneMatch(type -> type.equals(event.getType()))) {
            String error = String.format("Invalid event type, found: %s", event.getType());
            logger.error(error, new IllegalArgumentException());
            return badRequest(ResultUtility.getNodeForBooleanResponse(error));
        }

        PlanDBAccessor planDBAccessor = new PlanDBAccessor(getDatabase());
        Subscription s = (Subscription) event.getData().getObject();

        logger.trace("Subscription: {}", s);
        boolean isSuccessful = planDBAccessor.updateUserPlanId(s.getCustomer(), s.getPlan().getId(), s.getId(), s.getStatus());
        if (getEnvironment().isProd() && !isSuccessful) {
            String error = String.format("Could not update plan for customer: %s, subscription: %s", s.getCustomer(), s.getId());
            logger.error(error, new IllegalStateException());
        } else {
            logger.debug("Successfully updated subscription for customer: {}, subscription: {}", s.getCustomer(), s.getId());
        }

        return ok(ResultUtility.getNodeForBooleanResponse(true));
    }

}
