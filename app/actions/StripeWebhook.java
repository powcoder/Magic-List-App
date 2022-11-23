https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package actions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.libs.Json;
import play.mvc.*;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static controllers.BaseController.getString;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class StripeWebhook {

    private static final long DEFAULT_TOLERANCE = 300;

    /**
     * Key used to access the {@link Event} object, as it was obtained from stripe.
     */
    public static final String KEY_EVENT = "event";

    @With(AuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Authenticated {
        String key();
    }

    public static class AuthenticatedAction extends Action<Authenticated> {

        private static final String STRIPE_HEADER = "Stripe-Signature";

        private final Logger.ALogger logger = Logger.of(this.getClass());
        private final Config config;
        private final Environment environment;

        @Inject
        public AuthenticatedAction(Config config, Environment environment) {
            this.config = config;
            this.environment = environment;
        }

        @Override
        public CompletionStage<Result> call(Http.Context context) {
            String stripeWebhookSecret = getString(super.configuration.key(), config, environment);

            String payload = new String(context.request().body().asRaw().asBytes().toArray());

            logger.trace("Payload: {}", payload);

            String stripeHeader = Validation.string(STRIPE_HEADER, context.request().getHeaders().toMap());
            if (Validation.isEmpty(stripeHeader)) {
                String reason = "Missing stripe-signature header";
                logger.debug(reason);
                return completedFuture(badRequest(ResultUtility.getNodeForBooleanResponse(reason)));
            }
            logger.trace("Stripe Header: {}", stripeHeader);

            try {
                if (!Webhook.Signature.verifyHeader(payload, stripeHeader, stripeWebhookSecret, DEFAULT_TOLERANCE)) {
                    throw new SignatureVerificationException("Invalid signature!", stripeHeader);
                }
            } catch (SignatureVerificationException e) {
                logger.error("Stripe Webhook Error: ", e);
                String reason = "Signature verification failed";
                return completedFuture(badRequest(ResultUtility.getNodeForBooleanResponse(reason)));
            } catch (Exception e) {
                logger.error("Exception thrown: ", e);
                logger.error("Payload: {}", payload);
                String error = "There was an error parsing the webhook";
                return CompletableFuture.completedFuture(internalServerError(ResultUtility.getNodeForBooleanResponse(error)));
            }

            /*
                This is a hack since Event#EventRequest is thought of as a String by the Stripe API... so we put a NULL
                for the "request" field to conform to it being a String.
              */
            Event event;
            try {
                ObjectNode node = (ObjectNode) Json.parse(payload);
                node.putNull("request");
                event = StripeObject.PRETTY_PRINT_GSON.fromJson(Json.stringify(node), Event.class);
            } catch (Exception e) {
                logger.error("Exception thrown: ", e);
                logger.error("Payload: {}", payload);
                String error = "There was an error parsing the payload";
                return CompletableFuture.completedFuture(internalServerError(ResultUtility.getNodeForBooleanResponse(error)));
            }

            context.args.put(KEY_EVENT, event);

            return delegate.call(context);
        }

    }

}
