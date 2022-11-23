https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import model.account.Account;
import com.typesafe.config.Config;
import play.Environment;
import play.db.Database;
import play.mvc.Http;
import play.mvc.Result;
import utilities.AccountVerifier;

import javax.inject.Inject;

public class AdminAuthenticator extends BaseAccountAuthenticator {

    @Inject
    public AdminAuthenticator(AccountVerifier accountVerifier) {
        super(accountVerifier);
    }

    @Override
    public String getUsername(Http.Context context) {
        if (super.getUsername(context) == null) {
            return null;
        } else {
            Account account = getAccountVerifier().getAccount();
            if (account.isAdmin()) {
                return account.getToken();
            } else {
                return null;
            }
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(controllers.routes.UserController.getProfilePage());
    }
}