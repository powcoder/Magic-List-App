https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import controllers.UserController;
import model.VerificationResult;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.AccountVerifier;

import javax.inject.Inject;

/**
 * Created by Corey on 7/13/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class LogoutAuthenticator extends Security.Authenticator {

    private AccountVerifier accountVerifier;

    @Inject
    public LogoutAuthenticator(AccountVerifier accountVerifier) {
        this.accountVerifier = accountVerifier;
    }

    @Override
    public String getUsername(Http.Context context) {
        VerificationResult result = accountVerifier.isUserValidForStandardFunctionality(context.session());
        if (result == VerificationResult.INVALID_TOKEN) {
            return null;
        } else {
            accountVerifier.getAccount().saveAccountToSession(context.session());
            return accountVerifier.getAccount().getToken();
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        context.session().clear();
        context.flash().put(UserController.KEY_ERROR, "You are not logged in");
        return redirect(controllers.routes.UserController.getLoginPage());
    }

}
