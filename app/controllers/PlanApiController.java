https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Card;
import database.PlanDBAccessor;
import global.authentication.BaseAccountAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.stripe.Plan;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import clients.StripeClient;
import utilities.Validation;

import javax.inject.Inject;
import java.util.List;

/**
 *
 */
public class PlanApiController extends BaseController {

    private final StripeClient stripeClient;

    @Inject
    public PlanApiController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        stripeClient = new StripeClient();
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result updateSubscription() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String newPlanId = Validation.string(Account.STRIPE_PLAN_ID, node);
        if (newPlanId == null) {
            return badRequest(ResultUtility.getNodeForInvalidField(Account.STRIPE_PLAN_ID));
        }

        PlanDBAccessor planDBAccessor = new PlanDBAccessor(getDatabase());
        boolean isPlanValid = planDBAccessor.isPlanIdValid(newPlanId);
        if (!isPlanValid || newPlanId.equals(Plan.PLAN_UNLIMITED) || newPlanId.contains(Plan.PLAN_UNLIMITED)) {
            return badRequest(ResultUtility.getNodeForInvalidField(Account.STRIPE_PLAN_ID));
        }

        // Propagate the changes to Stripe
        String subscriptionId = account.getStripeSubscriptionId();
        if (Plan.PLAN_CANCELED.equalsIgnoreCase(newPlanId)) {
            boolean didUpdateSubscription = stripeClient.cancelSubscription(subscriptionId);
            if (didUpdateSubscription) {
                session().put(Account.STRIPE_PLAN_ID, Plan.PLAN_CANCELED);
            } else {
                String reason = "There was an error cancelling your subscription";
                return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
            }
        } else {
            try {
                subscriptionId = stripeClient.createSubscription(account.getCustomerId(), newPlanId, null);
                session().put(Account.SUBSCRIPTION_ID, subscriptionId);
                session().put(Account.STRIPE_PLAN_ID, newPlanId);
            } catch (CardException e) {
                Logger.error("Card exception thrown: " + e.getMessage());
                String reason = "Your credit card is either incorrect, was declined, or expired.";
                return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
            } catch (StripeException e) {
                e.printStackTrace();
                String reason = "Could not change subscription";
                return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
            }
            if (subscriptionId == null) {
                String reason = "Could not change subscription";
                return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
            }
        }

        // Update the database
        String status = Plan.PLAN_CANCELED.equals(newPlanId) ? StripeClient.STATUS_CANCELED : StripeClient.STATUS_ACTIVE;
        boolean isSuccessful = planDBAccessor.updateUserPlanId(account.getCustomerId(), newPlanId, subscriptionId, status);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not update plan!";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result addCreditCard() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String stripeToken = Validation.string(Account.ACCOUNT_STRIPE_TOKEN, node);
        if (stripeToken == null) {
            return badRequest(ResultUtility.getNodeForInvalidField(Account.STRIPE_PLAN_ID));
        }

        String customerId = account.getCustomerId();
        if (customerId == null) {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }

        boolean isSuccessful = stripeClient.createCreditCard(customerId, stripeToken);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result deleteCreditCard() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String creditCardId = Validation.string(Account.CARD_ID, node);
        if (creditCardId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Account.CARD_ID));
        }

        String customerId = account.getCustomerId();
        if (customerId == null) {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }

        if (!stripeClient.doesCreditCardExist(customerId, creditCardId)) {
            return badRequest(ResultUtility.getNodeForInvalidField(Account.CARD_ID));
        }

        List<Card> cardList = stripeClient.getCreditCards(customerId);
        if (cardList == null || cardList.size() == 1) {
            // You cant delete a credit card if there's only one left
            String reason = "You cannot delete a card if there\'s only one left";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        }

        boolean didDeleteCreditCard = stripeClient.deleteCreditCard(customerId, creditCardId);
        if (didDeleteCreditCard) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result changeDefaultCreditCard() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String creditCardId = Validation.string(Account.CARD_ID, node);
        if (creditCardId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(Account.CARD_ID));
        }

        String customerId = account.getCustomerId();
        if (customerId == null) {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }

        if (!stripeClient.doesCreditCardExist(customerId, creditCardId)) {
            return badRequest(ResultUtility.getNodeForInvalidField(Account.CARD_ID));
        }

        boolean wasSuccessful = stripeClient.changeDefaultCreditCard(customerId, creditCardId);
        if (wasSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse("Could not change default card"));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result retryPayment() {
        Account account = Account.getAccountFromSession();
        try {
            boolean isSuccessful = stripeClient.payMostRecentInvoice(account.getCustomerId());
            if(isSuccessful) {
                return ok(ResultUtility.getNodeForBooleanResponse(true));
            } else {
                String reason = "The payment could not be completed";
                return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
            }
        } catch (StripeException e) {
            Logger.error("Stripe Error: ", e);
            return internalServerError(ResultUtility.getNodeForBooleanResponse(e.getLocalizedMessage()));
        }
    }

}
