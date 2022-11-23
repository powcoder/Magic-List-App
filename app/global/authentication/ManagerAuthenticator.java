https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import controllers.BaseController;
import model.account.Account;
import com.typesafe.config.Config;
import play.Environment;
import play.db.Database;
import play.mvc.Http;
import play.mvc.Result;
import utilities.AccountVerifier;

import javax.inject.Inject;

/**
 * Authenticator used for granular access to the management portal and all of its respective routes
 */
public class ManagerAuthenticator extends SubscriptionAuthenticator {

    @Inject
    public ManagerAuthenticator(AccountVerifier accountVerifier) {
        super(accountVerifier);
    }

    @Override
    public String getUsername(Http.Context context) {
        if (super.getUsername(context) == null) {
            return null;
        } else {
            Account account = getAccountVerifier().getAccount();
            if (account.isManager()) {
                return account.getToken();
            } else {
                return null;
            }
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        Account account = getAccountVerifier().getAccount();
        if (account != null && !account.isManager()) {
            context.flash().put(BaseController.KEY_ERROR, "You must be a manager to access this feature");
            return redirect(controllers.routes.UserController.getProfilePage());
        } else {
            return super.onUnauthorized(context);
        }
    }

}
