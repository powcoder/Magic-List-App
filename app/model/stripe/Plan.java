https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.stripe;

import java.text.NumberFormat;

public class Plan {

    public static final String PLAN_UNLIMITED = "unlimited";
    public static final String PLAN_CANCELED = "canceled";
    public static final String PLAN_MONTHLY = "subscription_monthly";
    public static final String PLAN_BUY_PEOPLE = "list_automator_people";

    private String stripePlanId;
    private final double priceAmount;
    private double frequency;
    private final boolean isSubscription;

    /**
     * @param stripePlanId   The ID of the plan on Stripe.
     * @param priceAmount    As the raw amount returned by the database (unmodified)
     * @param isSubscription True if a subscription, false otherwise
     * @param frequency      The frequency at which the person will be charged. Only relevant for subscriptions
     */
    public Plan(String stripePlanId, double priceAmount, double frequency, boolean isSubscription) {
        this.stripePlanId = stripePlanId;
        this.priceAmount = priceAmount;
        this.frequency = frequency;
        this.isSubscription = isSubscription;
    }

    public String getStripePlanId() {
        return stripePlanId;
    }

    /**
     * @return The cost of the payment plan, after dividing the "Stripe" amount by 100.0
     */
    public double getPriceAmount() {
        return priceAmount / 100.0;
    }

    /**
     * @return The cost of the payment plan, <b>WITHOUT</b> dividing it by 100.0
     */
    public int getPriceAmountForDatabaseAndStripe() {
        return (int) priceAmount;
    }

    public double getFrequency() {
        return frequency;
    }

    public boolean isSubscription() {
        return isSubscription;
    }

    @Override
    public String toString() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

        if (priceAmount == -1) {
            return "Cancel your subscription";
        } else if (isSubscription) {
            double chargeFrequency = 12.0 / frequency;
            if (chargeFrequency == 1) {
                return numberFormat.format(priceAmount / 100.0) + " per month";
            } else if (chargeFrequency == 12) {
                return numberFormat.format(priceAmount / 100.0) + " per year";
            } else {
                return numberFormat.format(priceAmount / 100.0) + " every " + ((int) chargeFrequency) + " months";
            }
        } else {
            double amount = priceAmount / 100.0;
            return numberFormat.format(amount) + " (one-time purchase)";
        }
    }
}
