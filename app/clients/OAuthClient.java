https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.oauth.OAuthAccount;
import model.oauth.OAuthProvider;
import model.oauth.OAuthToken;
import model.prospect.Appointment;
import play.Logger;
import play.libs.ws.WSClient;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public abstract class OAuthClient {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    private final WSClient wsClient;
    private final String clientId;
    private final String clientSecret;

    public interface OnCalendarEventNotFoundListener {

        void onCalendarEventNotFound(String eventId, Map<String, Appointment> calendarEventsToAppointments);
    }

    OAuthClient(WSClient wsClient, String clientId, String clientSecret) {
        this.wsClient = wsClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    CompletionStage<OAuthToken> exchangeCodeForAuthToken(String redirectUrl, String tokenUrl, String code) {
        String grantType = "authorization_code";
        String body = String.format("client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&grant_type=%s",
                clientId, clientSecret, code, redirectUrl, grantType);
        return wsClient.url(tokenUrl)
                .setContentType("application/x-www-form-urlencoded")
                .post(body)
                .thenApply(response -> {
                    if (response.getStatus() == 200) {
                        JsonNode node = response.asJson();
                        return new OAuthToken.Converter().deserializeFromJson((ObjectNode) node);
                    } else {
                        logger.error("Received [status: {}] response from auth provider", response.getStatus());
                        logger.error("Could not retrieve auth token. Response: {}", response.getBody());
                        return null;
                    }
                });
    }

    CompletionStage<OAuthToken> refreshAccessToken(String oauthAccountId, String tokenUrl, OAuthToken authToken) {
        String grantType = "refresh_token";
        String body = String.format("client_id=%s&client_secret=%s&refresh_token=%s&grant_type=%s",
                clientId, clientSecret, authToken.getRefreshToken(), grantType);

        return wsClient.url(tokenUrl)
                .setContentType("application/x-www-form-urlencoded")
                .post(body)
                .thenApply(response -> {
                    if (response.getStatus() >= 200 && response.getStatus() < 300) {
                        JsonNode node = response.asJson();
                        return new OAuthToken.Converter().deserializeFromJson((ObjectNode) node);
                    } else {
                        logger.error("Could not refresh access token for account: {}", oauthAccountId,
                                new IllegalStateException());
                        logger.error("Response[{}] from provider: {}", response.getStatus(),
                                response.getBody());
                        return null;
                    }
                });
    }

    public WSClient getWsClient() {
        return wsClient;
    }

    public abstract OAuthProvider getProvider();

    public abstract CompletionStage<OAuthToken> exchangeCodeForAuthToken(String code);

    public abstract CompletionStage<OAuthToken> refreshAccessToken(String accountId, OAuthToken authToken);

    public abstract CompletionStage<OAuthAccount> getAccountInformation(OAuthToken accessToken);

}
