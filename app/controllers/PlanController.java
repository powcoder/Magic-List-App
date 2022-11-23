https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Card;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import database.AccountDBAccessor;
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
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Result;
import clients.StripeClient;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.*;
import views.html.stripe.*;

import javax.inject.Inject;
import java.util.List;

/**
 *
 */
public class PlanController extends BaseController {

    public static final String COUPON_ID = "coupon_id";
    public static final String AMOUNT_OFF = "amount_off";
    public static final String MAX_REDEMPTIONS = "max_redemptions";
    public static final String DURATION_IN_MONTHS = "duration_in_months";
    public static final String TIMES_REDEEMED = "times_redeemed";
    public static final String IS_VALID = "is_valid";

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final StripeClient stripeClient;

    @Inject
    public PlanController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        stripeClient = new StripeClient();
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getSubscription() {
        Account account = Account.getAccountFromSession();

        String stripePlanId = account.getStripePlanId();
        boolean isSuccessful = new AccountDBAccessor(getDatabase()).getAccountMetaData(account);

        if(!isSuccessful) {
            String error = "There was an error loading your account\'s credentials. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        if (stripePlanId == null || StripeClient.STATUS_CANCELED.equals(account.getMetaData().getSubscriptionStatus())) {
            stripePlanId = Plan.PLAN_CANCELED;
        }

        List<Plan> planList = new PlanDBAccessor(getDatabase())
                .getPlansFromDatabase(true);

        if (planList == null) {
            String text = "Could not load subscriptions. You can try going back or closing this tab.";
            return internalServerError(FailurePage.render(text));
        } else {
            return ok(SubscriptionsPage.render(planList, stripePlanId));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getCards() {
        Account account = Account.getAccountFromSession();

        String customerId = account.getCustomerId();
        if (customerId == null) {
            String text = "Could not load your information. You can try going back or closing this tab.";
            return internalServerError(FailurePage.render(text));
        }
        Customer customer = stripeClient.getCustomerById(customerId);
        if (customer == null) {
            return internalServerError(FailurePage.render("Could not load your information"));
        }

        List<Card> cards = stripeClient.getCreditCards(customerId);
        if (cards == null) {
            String text = "Could not load your cards. You can try going back or closing this tab.";
            return internalServerError(FailurePage.render(text));
        } else {
            return ok(CreditCardsPage.render(cards, customer.getDefaultSource()));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getMostRecentInvoice() {
        Account account = Account.getAccountFromSession();
        try {
            Invoice invoice = stripeClient.getMostRecentInvoice(account.getCustomerId());
            logger.trace("Invoice: {}", invoice);
            return ok(InvoicePage.render(invoice));
        } catch (StripeException e) {
            logger.error("Stripe exception: ", e);
            String reason = "Could not load your invoice. Please reload this page or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getInvoices() {
        Account account = Account.getAccountFromSession();
        try {
            List<Invoice> invoiceList = stripeClient.getInvoices(account.getCustomerId());
            if (invoiceList != null) {
                return ok(InvoiceListPage.render(invoiceList));
            } else {
                String reason = "You have no invoices to load. Please go back or to the home page";
                return badRequest(FailurePage.render(reason));
            }
        } catch (StripeException e) {
            logger.error("Stripe exception: ", e);
            String reason = "Could not load your invoice. Please reload this page or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getInvoiceById(String invoiceId) {
        Account account = Account.getAccountFromSession();
        try {
            Invoice invoice = stripeClient.getInvoiceById(account.getCustomerId(), invoiceId);
            if (invoice != null) {
                logger.trace("Invoice: {}", invoice);
                return ok(InvoicePage.render(invoice));
            } else {
                String reason = "This invoice does not exist. Please go back or to the home page";
                return badRequest(FailurePage.render(reason));
            }
        } catch (StripeException e) {
            logger.error("Stripe exception: ", e);
            String reason = "Could not load your invoice. Please reload this page or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result addNewCard() {
        Account account = Account.getAccountFromSession();

        String customerId = account.getCustomerId();
        if (customerId == null) {
            String text = "Could not load your information. You can try going back or closing this tab.";
            return internalServerError(FailurePage.render(text));
        }

        List<Card> cards = stripeClient.getCreditCards(customerId);
        if (cards == null) {
            String text = "Could not load your cards. You can try going back or closing this tab.";
            return internalServerError(FailurePage.render(text));
        } else {
            return ok(AddCardPage.render(stripeClient.STRIPE_API_KEY_PUBLIC));
        }
    }

    public Result getCouponFromId(String couponId) {
        if (Validation.isEmpty(couponId)) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Coupon ID cannot be empty"));
        }

        couponId = couponId.toUpperCase();
        Coupon coupon;
        try {
            coupon = stripeClient.getCouponById(couponId);

            if (coupon != null) {
                JsonNode node = Json.newObject()
                        .put(COUPON_ID, coupon.getId())
                        .put(AMOUNT_OFF, coupon.getAmountOff())
                        .put(MAX_REDEMPTIONS, coupon.getMaxRedemptions())
                        .put(DURATION_IN_MONTHS, coupon.getDurationInMonths())
                        .put(IS_VALID, coupon.getValid())
                        .put(TIMES_REDEEMED, coupon.getTimesRedeemed());
                return ok(node);
            } else {
                String reason = "A server error occurred when redeeming the coupon. Please try again or submit a bug report";
                return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
            }
        } catch (InvalidRequestException e) {
            String reason = "The entered coupon code is invalid";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
