https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import controllers.UserController;
import controllers.routes;
import model.account.Account;
import model.VerificationResult;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.AccountVerifier;
import utilities.Validation;

import javax.inject.Inject;

/**
 * Authenticator used to ensure that the user is logged in and has verified his/her email
 */
public class BaseAccountAuthenticator extends Security.Authenticator {

    private AccountVerifier accountVerifier;
    private VerificationResult verificationResult;

    @Inject
    public BaseAccountAuthenticator(AccountVerifier accountVerifier) {
        this.accountVerifier = accountVerifier;
    }

    @Override
    public String getUsername(Http.Context context) {
        String token = context.session().get(Account.TOKEN);
        if (Validation.isEmpty(token)) {
            return null;
        } else {
            verificationResult = accountVerifier.isUserValidForStandardFunctionality(context.session());
            if (verificationResult == VerificationResult.INVALID_TOKEN) {
                return null;
            } else if (verificationResult == VerificationResult.NOT_BETA_USER) {
                return null;
            } else if (!accountVerifier.getAccount().isEmailVerified()) {
                return null;
            } else {
                Account account = accountVerifier.getAccount();
                context.session().remove(UserController.KEY_ERROR);

                account.saveAccountToSession(context.session());
                return account.getToken();
            }
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        if (context.session().containsKey(AccountVerifier.KEY_IS_SESSION_EXPIRED)) {
            context.flash().put(UserController.KEY_ERROR, "Your session has expired");
            return redirect(controllers.routes.UserController.getLoginPage());
        } else if (accountVerifier.getAccount() == null || accountVerifier.getAccount().getToken() == null) {
            context.session().clear();
            context.flash().put(UserController.KEY_ERROR, "You are not logged in");
            return redirect(controllers.routes.UserController.getLoginPage());
        } else if (!accountVerifier.getAccount().isEmailVerified()) {
            return redirect(controllers.routes.UserController.getSendVerificationEmailPage());
        } else if (verificationResult == VerificationResult.NOT_BETA_USER) {
            context.flash().put(UserController.KEY_ERROR, "You must be a beta user to access this service");
            return redirect(controllers.routes.UserController.getLoginPage());
        } else {
            Logger.error("Invalid state!", new IllegalStateException());
            context.flash().put(UserController.KEY_ERROR, "You are not logged in");
            return redirect(controllers.routes.UserController.getLoginPage());
        }
    }

    /* Package-Private */ AccountVerifier getAccountVerifier() {
        return accountVerifier;
    }

}
