https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.oauth;

/**
 * Created by Corey on 4/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class OAuthTokenAccountWrapper {

    private final OAuthToken authToken;
    private final OAuthAccount authAccount;

    public OAuthTokenAccountWrapper(OAuthToken authToken, OAuthAccount authAccount) {
        this.authToken = authToken;
        this.authAccount = authAccount;
    }

    public OAuthToken getAuthToken() {
        return authToken;
    }

    public OAuthAccount getAuthAccount() {
        return authAccount;
    }
}
