https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import controllers.UserController;
import model.VerificationResult;
import model.account.Account;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.db.Database;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.AccountVerifier;

import javax.inject.Inject;

/**
 * Authenticator used for re-sending the verification email. Only checks if the user is logged in (not if the email
 * has been verified or not).
 */
public class InvalidEmailAuthenticator extends Security.Authenticator {

    private final AccountVerifier accountVerifier;

    @Inject
    public InvalidEmailAuthenticator(AccountVerifier accountVerifier) {
        this.accountVerifier = accountVerifier;
    }

    @Override
    public String getUsername(Http.Context context) {
        String token = context.session().get(Account.TOKEN);
        if (token == null) {
            return null;
        }

        VerificationResult result = accountVerifier.isUserValidForStandardFunctionality(context.session());
        if (result != VerificationResult.INVALID_TOKEN) {
            accountVerifier.getAccount().saveAccountToSession(context.session());
            if (accountVerifier.getAccount().isEmailVerified()) {
                return null;
            } else {
                context.session().remove(UserController.KEY_ERROR);
                return token;
            }
        } else {
            context.session().remove(UserController.KEY_ERROR);
            return null;
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        Account account = accountVerifier.getAccount();
        if (account.getToken() == null) {
            context.flash().put(UserController.KEY_ERROR, "You are not logged in");
            context.session().clear();
            return redirect(controllers.routes.UserController.getLoginPage());
        } else if (account.isEmailVerified()) {
            return redirect(controllers.routes.UserController.getProfilePage());
        } else {
            Logger.error("Token: {}; Email_Verified: {}", account.getToken(), account.isEmailVerified());
            throw new RuntimeException("This should not happen!");
        }
    }

}
