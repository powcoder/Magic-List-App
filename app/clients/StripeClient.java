https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.*;
import controllers.BaseController;
import database.AccountDBAccessor;
import database.AccountSettingsDBAccessor;
import database.MiscellaneousDBAccessor;
import database.PlanDBAccessor;
import model.stripe.CvcCheck;
import model.VerificationResult;
import play.Logger;
import play.db.Database;

import java.text.DecimalFormat;
import java.util.*;

/**
 *
 */
public class StripeClient {

    private static final Logger.ALogger logger = Logger.of(StripeClient.class);

    public final String STRIPE_API_KEY_PUBLIC;

    public Database database;

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_TRIALING = "trialing";
    public static final String STATUS_PAST_DUE = "past_due";
    public static final String STATUS_CANCELED = "canceled";
    public static final String STATUS_UNPAID = "unpaid";
    private static final String[] STATUSES = {STATUS_ACTIVE, STATUS_TRIALING, STATUS_PAST_DUE, STATUS_CANCELED, STATUS_UNPAID};

    public static boolean isStatusValid(String rawStatus) {
        return rawStatus == null || Arrays.stream(STATUSES).anyMatch(status -> status.equals(rawStatus));
    }

    public static boolean isSubscriptionActive(String status) {
        return STATUS_ACTIVE.equals(status) || STATUS_TRIALING.equals(status);
    }

    public StripeClient() {
        STRIPE_API_KEY_PUBLIC = BaseController.getString("STRIPE_API_KEY_PUBLIC");
        Stripe.apiKey = BaseController.getString("STRIPE_API_KEY_SECRET");
    }

    public boolean cancelAllSubscriptions() {
        SubscriptionCollection subscriptionCollection;
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("plan", "magic_list_monthly_temporary");
//            map.put("plan", "plan_Ee05CCI4rrKmZI");
            subscriptionCollection = Subscription.list(map);
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            logger.error("Error: ", e);
            return false;
        }
        for (Subscription subscription : subscriptionCollection.autoPagingIterable()) {
            try {
                subscription.cancel(new HashMap<>());
                String customerId = subscription.getCustomer();
                new PlanDBAccessor(database).updateUserPlanId(customerId, StripeClient.STATUS_CANCELED, null, StripeClient.STATUS_CANCELED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * @param newPlanName The name of the new plan, to be used to migrate to the new subscription.
     * @return True if successful, false otherwise
     */
    public boolean moveAllSubscriptions(String oldPlanName, String newPlanName) {
        Map<String, Object> oldPlanParams = new HashMap<>();
        oldPlanParams.put("plan", oldPlanName);

        SubscriptionCollection subscriptionCollection;
        try {
            subscriptionCollection = Subscription.list(oldPlanParams);
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            logger.error("Error: ", e);
            return false;
        }

        for (Subscription subscription : subscriptionCollection.autoPagingIterable()) {

            String itemID = subscription.getSubscriptionItems().getData().get(0).getId();

            Map<String, Object> item = new HashMap<>();
            item.put("id", itemID);
            item.put("plan", newPlanName);

            Map<String, Object> items = new HashMap<>();
            items.put("0", item);

            Map<String, Object> newPlanParams = new HashMap<>();
            newPlanParams.put("items", items);
            newPlanParams.put("prorate", false);

            try {
                System.out.println("TIME TO PRORATE " + Customer.retrieve(subscription.getCustomer()).getEmail());
            } catch (Exception e) {
                e.printStackTrace();
            }

//            try {
//                subscription.update(newPlanParams);
//            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException e) {
//                logger.error("Error: ", e);
//                return false;
//            } catch (CardException e) {
//                logger.error("Found card error: ", e.getMessage());
//                try {
//                    subscription.cancel(new HashMap<>());
//                } catch (Exception innerError) {
//                    innerError.printStackTrace();
//                }
//            }
        }

        return true;
    }

    /**
     * @param email       The customer's email
     * @param stripeToken The customer's stripe token
     * @return The customer's new unique stripe ID or null if an error occurred.
     */
    public String createCustomer(String email, String stripeToken) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("source", stripeToken);
        return Customer.create(params).getId();
    }

    public boolean deleteCustomer(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            return customer.delete().getDeleted();
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param customerId The customer's ID according to Stripe.
     * @param planId     The plan ID to which the user would like to subscribe, on Stripe.
     * @param coupon     The coupon to apply to the subscription or null for none
     * @return The ID of the customer's subscription to which he/she just subscribed or null if the request failed.
     */
    public String createSubscription(String customerId, String planId, Coupon coupon) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("plan", planId);
        if (coupon != null) {
            params.put("coupon", coupon.getId());
        }
        Subscription subscription = Subscription.create(params);
        if (subscription != null) {
            return subscription.getId();
        } else {
            return null;
        }
    }

    public Customer getCustomerById(String customerId) {
        try {
            return Customer.retrieve(customerId);
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param customerId The ID of the customer whose information should be checked.
     * @return A {@link VerificationResult} object that is based on the result of the check with Stripe whether or not
     * the user's subscription is valid.
     */
    public VerificationResult verifyHasPaymentMethod(String customerId) {
        List<Card> cardList = getCreditCards(customerId);
        if (cardList == null || cardList.isEmpty()) {
            return VerificationResult.NO_PAYMENT_METHODS;
        }

        boolean containsSuccess = cardList.stream()
                .anyMatch(card -> CvcCheck.createFromStripeApi(card.getCvcCheck()) != CvcCheck.FAIL);

        return containsSuccess ? VerificationResult.SUCCESS : VerificationResult.NO_PAYMENT_METHODS;
    }

    /**
     * @param subscriptionId The ID of the subscription that should be checked.
     * @return One of {@link #STATUS_ACTIVE}, {@link #STATUS_CANCELED}, {@link #STATUS_PAST_DUE},
     * {@link #STATUS_TRIALING}, {@link #STATUS_UNPAID} or null if an error occurs
     */
    public String getSubscriptionStatus(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId).getStatus();
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            Logger.error("Stripe Exception: ", e);
            return null;
        }
    }

    /**
     * @param customerId The ID of the customer whose most recent invoice should be retrieved
     * @return The user's invoice if there is a most recent one, or null if there isn't one
     */
    public Invoice getMostRecentInvoice(String customerId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("limit", 1);
        InvoiceCollection invoiceCollection = Invoice.list(params);
        if (invoiceCollection.getData() != null && invoiceCollection.getData().size() >= 1) {
            return invoiceCollection.getData().get(0);
        } else {
            return null;
        }
    }

    /**
     * @param customerId The ID of the customer whose most recent invoice should be retrieved
     * @param invoiceId  The ID of the invoice to retrieve
     * @return The user's invoice or null if the invoice's customer ID doesn't match the customer's ID
     */
    public Invoice getInvoiceById(String customerId, String invoiceId) throws StripeException {
        Invoice invoice = Invoice.retrieve(invoiceId);
        if (invoice != null && customerId.equals(invoice.getCustomer())) {
            return invoice;
        } else {
            return null;
        }
    }

    /**
     * @param customerId The ID of the customer whose most recent invoice should be retrieved and paid
     * @return True if the payment was successful, false if there was no invoice to pay or it was not successful.
     */
    public List<Invoice> getInvoices(String customerId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        return Invoice.list(params).getData();
    }

    /**
     * @param customerId The ID of the customer whose most recent invoice should be retrieved and paid
     * @return True if the payment was successful, false if there was no invoice to pay or it was not successful.
     */
    public boolean payMostRecentInvoice(String customerId) throws StripeException {
        Invoice invoice = getMostRecentInvoice(customerId);
        if (invoice != null) {
            return invoice.pay().getPaid();
        } else {
            return false;
        }
    }

    /**
     * @param customerId The ID of the customer whose most recent charge should be retrieved
     * @return A charge object or null if no recent invoice object exists
     */
    public Charge getMostRecentChargeFromMostRecentInvoice(String customerId) throws StripeException {
        Invoice invoice = getMostRecentInvoice(customerId);
        if (invoice != null) {
            return invoice.getChargeObject();
        } else {
            return null;
        }
    }

    /**
     * @param subscriptionId The ID of the subscription that should be checked.
     * @return True if the subscription is "active" and okay, false otherwise.
     */
    public boolean cancelSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Map<String, Object> map = new HashMap<>();
            map.put("at_period_end", false); // Let the subscription keep going until it expires
            subscription.cancel(map);
            return true;
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean doesCreditCardExist(String customerId, String cardId) {
        try {
            return Customer.retrieve(customerId).getSources().retrieve(cardId) != null;
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createCreditCard(String customerId, String stripeToken) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("source", stripeToken);
            com.stripe.model.Card card = (com.stripe.model.Card) Customer.retrieve(customerId).getSources().create(map);
            return card != null;
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCreditCard(String customerId, String cardId) {
        try {
            DeletedCard deletedCard = (DeletedCard) Customer.retrieve(customerId).getSources().retrieve(cardId).delete();
            return deletedCard.getDeleted();
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param customerId The ID of the customer whose cards should be retrieved.
     * @return The user's credit cards
     */
    public List<Card> getCreditCards(String customerId) {
        try {
            List<Card> cardList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put("object", "card");
            List<ExternalAccount> externalAccounts = Customer.retrieve(customerId)
                    .getSources()
                    .list(map)
                    .getData();
            for (ExternalAccount externalAccount : externalAccounts) {
                cardList.add((Card) externalAccount);
            }

            return cardList;
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean changeDefaultCreditCard(String customerId, String cardId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            if (customer != null) {
                Map<String, Object> updateParams = new HashMap<>();
                updateParams.put("default_source", cardId);
                return customer.update(updateParams) != null;
            } else {
                return false;
            }
        } catch (AuthenticationException | InvalidRequestException | CardException | APIConnectionException | APIException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Charges the given customer a set amount for the people he/she bought with List Automator.
     *
     * @param customerId            The ID of the customer to charge.
     * @param chargeAmountPerPerson The amount the customer should be charged per contect - integer where 100 = $1.00; 6 = $0.06;
     * @param numberOfContacts      The number of people that the person bought.
     * @return True if the charge was successful, false otherwise
     */
    public boolean chargeCardForPersonList(String customerId, int chargeAmountPerPerson, int numberOfContacts) {
        Map<String, Object> params = new HashMap<>();
        chargeAmountPerPerson = Math.max(100, chargeAmountPerPerson * numberOfContacts);
        params.put("amount", chargeAmountPerPerson);
        params.put("currency", "usd");
        params.put("customer", customerId);
        String peopleCount = DecimalFormat.getInstance().format(numberOfContacts);
        params.put("description", String.format("Retrieved %s contacts with Magic List Importer", peopleCount));
        try {
            Charge charge = Charge.create(params);
            return charge.getPaid();
        } catch (AuthenticationException | InvalidRequestException | CardException | APIException
                | APIConnectionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Coupon getCouponById(String couponId) throws InvalidRequestException {
        // Default to all uppercase coupon
        couponId = couponId.toUpperCase();
        try {
            return Coupon.retrieve(couponId);
        } catch (AuthenticationException | APIConnectionException | APIException | CardException e) {
            e.printStackTrace();
            return null;
        }
    }

//    MARK - Private Methods

    private static VerificationResult getVerificationResultFromStatus(String status) {
        switch (status) {
            case StripeClient.STATUS_ACTIVE:
            case StripeClient.STATUS_TRIALING:
                return VerificationResult.SUCCESS;
            case StripeClient.STATUS_CANCELED:
                return VerificationResult.NO_SUBSCRIPTION;
            case StripeClient.STATUS_UNPAID:
            case StripeClient.STATUS_PAST_DUE:
                return VerificationResult.INVALID_CREDIT_CARD;
            default:
                return VerificationResult.SERVER_ERROR;
        }
    }

}
