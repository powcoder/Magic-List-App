https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global.authentication;

import model.VerificationResult;
import play.mvc.Http;
import utilities.AccountVerifier;
import utilities.Validation;

import javax.inject.Inject;

public class SuperAdminAuthenticator extends BaseAccountAuthenticator {

    @Inject
    public SuperAdminAuthenticator(AccountVerifier accountVerifier) {
        super(accountVerifier);
    }

    @Override
    public String getUsername(Http.Context context) {
        String token = super.getUsername(context);
        if(Validation.isEmpty(token)) {
            return null;
        }

        VerificationResult result = getAccountVerifier().isUserSuperAdmin(token);
        if(result == VerificationResult.SUCCESS) {
            return getAccountVerifier().getAccount().getToken();
        } else {
            context.session().clear();
            return null;
        }
    }
}
