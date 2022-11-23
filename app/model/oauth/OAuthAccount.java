https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.oauth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class OAuthAccount {

    private static final String KEY_ID = "account_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PROVIDER = "provider";

    private final String accountId;
    private final String email;
    private final OAuthProvider provider;

    public OAuthAccount(String accountId, String email, OAuthProvider provider) {
        this.accountId = accountId;
        this.email = email;
        this.provider = provider;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public static class Converter implements JsonConverter<OAuthAccount> {

        @Override
        public ObjectNode renderAsJsonObject(OAuthAccount object) {
            return Json.newObject()
                    .put(KEY_ID, escape(object.accountId))
                    .put(KEY_EMAIL, escape(object.email))
                    .put(KEY_PROVIDER, escape(object.getProvider().getRawText()));
        }

        @Override
        public OAuthAccount deserializeFromJson(ObjectNode objectNode) {
            throw new RuntimeException("Attempted to call stubbed method");
        }
    }

    public static class OutlookConverter extends Converter {

        private static final String KEY_OUTLOOK_ID = "id";
        private static final String KEY_OUTLOOK_EMAIL = "userPrincipalName";

        @Override
        public OAuthAccount deserializeFromJson(ObjectNode objectNode) {
            String accountId = objectNode.get(KEY_OUTLOOK_ID).asText();
            String email = objectNode.get(KEY_OUTLOOK_EMAIL).asText();
            return new OAuthAccount(accountId, email, OAuthProvider.OUTLOOK);
        }

    }

}
