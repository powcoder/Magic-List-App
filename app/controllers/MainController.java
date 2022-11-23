https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.SubscriptionAssetsClient;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.VerificationResult;
import play.Logger;
import play.api.mvc.AnyContent;
import play.api.mvc.Action;
import play.mvc.Result;
import play.mvc.Security;
import utilities.AccountVerifier;
import views.html.failure.FailurePage;
import views.html.failure.NotFoundPage;
import views.html.misc.*;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *
 */
public class MainController extends BaseController {

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final controllers.Assets assets;
    private final SubscriptionAssetsClient subscriptionAssetsClient;

    @Inject
    public MainController(ControllerComponents controllerComponents, Assets assets) {
        super(controllerComponents);
        this.assets = assets;
        this.subscriptionAssetsClient = new SubscriptionAssetsClient();
    }

    public Result index() {
        AccountVerifier accountVerifier = new AccountVerifier(getDatabase(), getConfig());
        if(accountVerifier.isUserValidForStandardFunctionality(session()) == VerificationResult.SUCCESS) {
            accountVerifier.getAccount().saveAccountToSession(session());
        } else {
            session().clear();
        }

        return ok(Index.render());
    }

    public Action<AnyContent> getPublicAsset(String file) {
        return assets.versioned("/public", new Assets.Asset(file));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> getSubscriptionAsset(String file) {
        String reason = String.format("Could not retrieve file: %s", file);

        return CompletableFuture.supplyAsync(() -> subscriptionAssetsClient.downloadFileFromBucket(file))
                .thenApplyAsync(blobOptional -> blobOptional.map(blob -> ok(blob.getContent()))
                        .orElseGet(() -> notFound(NotFoundPage.render())), getHttpExecutionContext().current())
                .exceptionally(exception -> {
                    logger.error(reason, exception);
                    return internalServerError();
                })
                .thenApplyAsync(result -> {
                    if (result.status() >= 500) {
                        return internalServerError(FailurePage.render(reason));
                    } else {
                        return result;
                    }
                }, getHttpExecutionContext().current());
    }

    public Result getPrivacyStatement() {
        return ok(PrivacyStatementPage.render());
    }

    public Result getPrivacyStatementRedirect() {
        return redirect(routes.MainController.getPrivacyStatement());
    }

    public Result getTermsOfUse() {
        return ok(TermsOfUsePage.render());
    }

    public Result getComingSoon() {
        return ok(ComingSoonPage.render());
    }

}
