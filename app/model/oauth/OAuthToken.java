https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.oauth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;

import java.util.Calendar;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class OAuthToken {

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_EXPIRES_IN = "expires_in";
    private static final String KEY_EXPIRATION_TIME_MILLIS = "expiration_time_millis";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private static final long ONE_MINUTE_MILLIS = 60 * 1000;

    private final String accessToken;
    private final String refreshToken;
    private final long expirationTimeMillis;

    /**
     * @param accessToken      The user's access token
     * @param refreshToken     The user's refresh token
     * @param expiresInSeconds The time (in seconds) from now that the new access token will expire. IE 3600 (meaning 1
     *                         hour)
     */
    public OAuthToken(String accessToken, String refreshToken, int expiresInSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTimeMillis = Calendar.getInstance().getTimeInMillis() + (expiresInSeconds * 1000);
    }

    /**
     * @param accessToken          The user's access token
     * @param refreshToken         The user's refresh token
     * @param expirationTimeMillis The time (in milliseconds) that the new access token will expire since the Unix Epoch
     */
    public OAuthToken(String accessToken, String refreshToken, long expirationTimeMillis) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTimeMillis = expirationTimeMillis;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * @return The time at which the given access token will expire. Represented as millis since the Unix Epoch.
     */
    public long getExpirationTimeInMillis() {
        return expirationTimeMillis;
    }

    public boolean isExpired() {
        return Calendar.getInstance().getTimeInMillis() + ONE_MINUTE_MILLIS >= expirationTimeMillis;
    }

    public static class Converter implements JsonConverter<OAuthToken> {

        @Override
        public ObjectNode renderAsJsonObject(OAuthToken object) {
            return Json.newObject()
                    .put(KEY_ACCESS_TOKEN, object.accessToken)
                    .put(KEY_EXPIRATION_TIME_MILLIS, object.expirationTimeMillis)
                    .put(KEY_REFRESH_TOKEN, object.refreshToken);
        }

        @Override
        public OAuthToken deserializeFromJson(ObjectNode objectNode) {
            String accessToken = objectNode.get(KEY_ACCESS_TOKEN).asText();
            String refreshToken = objectNode.get(KEY_REFRESH_TOKEN).asText();
            int expiresInSeconds = objectNode.get(KEY_EXPIRES_IN).asInt();
            return new OAuthToken(accessToken, refreshToken, expiresInSeconds);
        }

    }

}
