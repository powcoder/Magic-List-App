https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import controllers.UserController;
import model.account.Account;
import model.VerificationResult;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.db.Database;
import play.mvc.Http;
import play.mvc.Result;
import utilities.AccountVerifier;

import javax.inject.Inject;

public class SubscriptionAuthenticator extends BaseAccountAuthenticator {

    private VerificationResult verificationResult;

    @Inject
    public SubscriptionAuthenticator(AccountVerifier accountVerifier) {
        super(accountVerifier);
    }

    @Override
    public String getUsername(Http.Context context) {
        String token = super.getUsername(context);
        if (token == null) {
            return null;
        }

        verificationResult = getAccountVerifier().isUserValidForPaidFunctionality(token, false, context.session());
        if (verificationResult == VerificationResult.SUCCESS) {
            context.session().remove(UserController.KEY_ERROR);
            getAccountVerifier().getAccount().saveAccountToSession(context.session());
            return token;
        } else if (verificationResult == VerificationResult.INVALID_TOKEN) {
            context.session().clear();
            return null;
        } else if (verificationResult == VerificationResult.EMAIL_NOT_VERIFIED) {
            getAccountVerifier().getAccount().saveAccountToSession(context.session());
            return null;
        } else {
            return null;
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        Account account = getAccountVerifier().getAccount();
        if (super.getUsername(context) == null
                || account == null
                || account.getToken() == null
                || verificationResult == VerificationResult.INVALID_TOKEN) {
            return super.onUnauthorized(context);
        } else if (!getAccountVerifier().getAccount().isEmailVerified()) {
            // The user has not verified his/her email yet
            return redirect(controllers.routes.UserController.getSendVerificationEmailPage());
        } else if (verificationResult != VerificationResult.INVALID_TOKEN) {
            // The user is logged in but does not have a valid subscription
            return BaseAccountAuthenticator.redirect(controllers.routes.UserController.getProfilePage());
        } else {
            // The user is not logged in, save that info to his/her session and redirect to the login page
            context.flash().put(UserController.KEY_ERROR, "You have been logged out");
            return redirect(controllers.routes.UserController.getLoginPage());
        }
    }
}