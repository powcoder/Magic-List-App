https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import clients.OAuthClient;
import controllers.BaseController;
import controllers.routes;
import database.OAuthDBAccessor;
import model.oauth.OAuthToken;
import play.db.Database;
import play.mvc.Http.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Corey on 4/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public final class OAuthUtility {

    private final OAuthClient authClient;
    private final OAuthDBAccessor oAuthDBAccessor;

    public OAuthUtility(OAuthClient authClient, Database database) {
        this.authClient = authClient;
        this.oAuthDBAccessor = new OAuthDBAccessor(database);
    }

    public static String getOutlookSignInUrl(String clientId) {
        String host = Context.current().request().host();
        String protocol = BaseController.getEnvironment().isProd() ? "https://" : "http://";
        String redirectUrl =  protocol + host + controllers.routes.OAuthController.loginWithOutlookCallback().url();
        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
        url += "?client_id=" + clientId;
        url += "&redirect_uri=" + redirectUrl;
        url += "&response_mode=query";
        url += "&response_type=code";
        url += "&scope=offline_access openid Calendars.Read Calendars.Read.Shared Calendars.ReadWrite Calendars.ReadWrite.Shared User.Read";
        return url;
    }

    public CompletionStage<OAuthToken> getTokenForAccount(String userId, String oauthAccountId) {
        OAuthToken authToken = oAuthDBAccessor.getAuthTokenForAccount(userId, oauthAccountId);
        if (authToken == null) {
            return CompletableFuture.completedFuture(null);
        } else if (authToken.isExpired()) {
            // we need to refresh and save the newly retrieved access token
            return authClient.refreshAccessToken(oauthAccountId, authToken)
                    .thenApply(newAuthToken -> {
                        if (newAuthToken != null) {
                            boolean isSuccessful =
                                    oAuthDBAccessor.saveAuthTokenForAccount(userId, oauthAccountId, newAuthToken);
                            return isSuccessful ? newAuthToken : null;
                        } else {
                            // There was an error retrieving a new access token
                            return null;
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(authToken);
        }
    }

}
