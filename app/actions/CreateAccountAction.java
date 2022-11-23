https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package actions;

import clients.StripeClient;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Coupon;
import controllers.routes;
import database.AccountDBAccessor;
import database.CompanyDBAccessor;
import database.PlanDBAccessor;
import model.account.Account;
import model.stripe.Plan;
import com.typesafe.config.Config;
import play.Environment;
import play.db.Database;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utilities.RandomStringGenerator;
import utilities.Validation;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static controllers.BaseController.KEY_ERROR;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static model.account.Account.*;

/**
 * Action to be applied to the <i>Create Account</i> route.
 */
public class CreateAccountAction extends Action.Simple {

    public static final String KEY_ACCOUNT = "ACCOUNT";
    public static final String KEY_PASSWORD = "PASSWORD";
    public static final String KEY_STRIPE_TOKEN = "STRIPE_TOKEN";
    public static final String KEY_COUPON = "COUPON";

    private final StripeClient stripeClient;
    private final CompanyDBAccessor companyDBAccessor;
    private final PlanDBAccessor planDBAccessor;
    private final AccountDBAccessor accountDBAccessor;

    @Inject
    public CreateAccountAction(Database database) {
        stripeClient = new StripeClient();
        companyDBAccessor = new CompanyDBAccessor(database);
        planDBAccessor = new PlanDBAccessor(database);
        accountDBAccessor = new AccountDBAccessor(database);
    }

    @Override
    public CompletionStage<Result> call(Http.Context context) {
        Map<String, String[]> map = context.request().body().asFormUrlEncoded();
        String email = Validation.string(EMAIL, map);
        String name = Validation.string(ACCOUNT_NAME, map);
        String password = Validation.string(PASSWORD, map);
        String stripeToken = Validation.string(ACCOUNT_STRIPE_TOKEN, map);
        String stripePlanId = Validation.string(STRIPE_PLAN_ID, map);
        String couponId = Validation.string(COUPON_ID, map);
        String companyName = Validation.string(ACCOUNT_COMPANY_NAME, map);
        boolean isManager = Validation.bool(Account.IS_MANAGER, map, false);

        if (Validation.isEmpty(email)) {
            flashPut(KEY_ERROR, "Missing " + EMAIL + " field!");
            return redirectToCreateAccount();
        }
        if (Validation.isEmpty(name)) {
            flashPut(KEY_ERROR, "Missing " + ACCOUNT_NAME + " field!");
            return redirectToCreateAccount();
        }
        if (Validation.isEmpty(password)) {
            flashPut(KEY_ERROR, "Missing " + PASSWORD + " field!");
            return redirectToCreateAccount();
        }
        if (Validation.isEmpty(stripeToken)) {
            flashPut(KEY_ERROR, "Missing " + ACCOUNT_STRIPE_TOKEN + " field!");
            return redirectToCreateAccount();
        }
        if (Validation.isEmpty(stripePlanId)) {
            flashPut(KEY_ERROR, "Missing " + STRIPE_PLAN_ID + " field!");
            return redirectToCreateAccount();
        }
        if (Validation.isEmpty(companyName)) {
            flashPut(KEY_ERROR, "Missing " + ACCOUNT_COMPANY_NAME + " field!");
            return redirectToCreateAccount();
        }

        email = email.trim().toLowerCase();
        name = name.trim();

        if (!Validation.isEmpty(couponId)) {
            try {
                Coupon coupon = stripeClient.getCouponById(couponId);
                if (coupon != null && !coupon.getValid()) {
                    context.flash().put(KEY_ERROR, "This coupon has expired");
                    return redirectToCreateAccount();
                } else if (coupon == null) {
                    context.flash().put(KEY_ERROR, "A server occurred while redeeming your coupon. Please try again or submit a bug report");
                    return redirectToCreateAccount();
                } else if (coupon.getValid()) {
                    context.args.put(KEY_COUPON, couponId);
                }
            } catch (InvalidRequestException e) {
                context.flash().put(KEY_ERROR, "The coupon code entered is invalid");
                return redirectToCreateAccount();
            }
        }

        if (accountDBAccessor.isEmailAlreadyInDatabase(email)) {
            context.flash().put(KEY_ERROR, "This email is already in use");
            return redirectToCreateAccount();
        }

        if (!companyDBAccessor.isCompanyExist(companyName)) {
            context.flash().put(KEY_ERROR, "This company does not exist in our system yet. Please contact support to have them be added");
            return redirectToCreateAccount();
        }

        if (!planDBAccessor.isPlanIdValid(stripePlanId) ||
                Plan.PLAN_UNLIMITED.equals(stripePlanId) ||
                Plan.PLAN_CANCELED.equals(stripePlanId)) {
            context.flash().put(KEY_ERROR, "The selected payment plan is invalid");
            return redirectToCreateAccount();
        }

        Plan plan = planDBAccessor.getPlanById(stripePlanId);
        if (plan == null) {
            context.flash().put(KEY_ERROR, "Could not retrieve subscription plan");
            return redirectToCreateAccount();
        }

        String userId = RandomStringGenerator.getInstance().getNextRandomUserId();
        String token = RandomStringGenerator.getInstance().getNextRandomToken();

        Account account = Account.createAccountFromCreationAction(userId, name, email, token, stripePlanId,
                companyName, isManager);
        context.args.put(KEY_ACCOUNT, account);
        context.args.put(KEY_PASSWORD, password);
        context.args.put(KEY_STRIPE_TOKEN, stripeToken);

        return delegate.call(context);
    }

    private static CompletionStage<Result> redirectToCreateAccount() {
        return completedFuture(redirect(routes.UserController.getCreateAccountPage()));
    }

    private static void flashPut(String key, String value) {
        Http.Context.current().flash().put(key, value);
    }

}
