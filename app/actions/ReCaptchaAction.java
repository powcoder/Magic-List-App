https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package actions;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utilities.DebugUtility;
import utilities.Validation;
import views.html.failure.FailurePage;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Controller.request;

public class ReCaptchaAction extends Action.Simple {

    private static final String GOOGLE_CAPTCHA_VERIFICATION_ROUTE = "https://www.google.com/recaptcha/api/siteverify";
    private static final String KEY_CAPTCHA = "g-recaptcha-response";

    private final WSClient client;

    @Inject
    public ReCaptchaAction(WSClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<Result> call(Http.Context context) {
        String captchaResponse = Validation.string(KEY_CAPTCHA, request().body().asFormUrlEncoded());

        if (!verifyCaptcha(captchaResponse, context.request().remoteAddress())) {
            String reason = "The submitted ReCaptcha is invalid";
            Logger.debug(reason);
            context.flash().put(BaseController.KEY_ERROR, reason);
            String referer = Validation.string(BaseController.REFERER, context.request().getHeaders().toMap());
            return completedFuture(redirect(referer));
        }

        return delegate.call(context);
    }

    private boolean verifyCaptcha(String captcha, String userIpAddress) {
        try {
            String body = client.url(GOOGLE_CAPTCHA_VERIFICATION_ROUTE)
                    .addQueryParameter("secret", "6LdKCSgUAAAAAPNH1O0OIHkdWLNW4NS4kIih5OHp")
                    .addQueryParameter("response", captcha)
                    .addQueryParameter("remoteip", userIpAddress)
                    .get()
                    .toCompletableFuture()
                    .get()
                    .getBody();

            if (!Validation.isEmpty(body)) {
                try {
                    JsonNode node = Json.parse(body);
                    return node.get("success").asBoolean();
                } catch (Exception e) {
                    Logger.error("Captcha Exception: ", e);
                    return false;
                }
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

}
