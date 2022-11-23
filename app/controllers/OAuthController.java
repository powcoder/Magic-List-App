https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.OAuthClient;
import clients.OutlookAuthClient;
import database.OAuthDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.oauth.OAuthAccount;
import model.oauth.OAuthTokenAccountWrapper;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.FailurePage;
import views.html.oauth.ViewOAuthAccountsPage;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utilities.FutureUtility.getFromFutureQuietly;

/**
 * Created by Corey on 3/17/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class OAuthController extends BaseController {

    private static final String KEY_CODE = "code";
    private final Logger.ALogger logger = Logger.of(this.getClass());

    private final OutlookAuthClient outlookAuthClient;

    @Inject
    public OAuthController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        outlookAuthClient = OutlookAuthClient.getInstance(getWsClient(), getOutlookClientId(), getOutlookClientSecret());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAccounts() {
        String outlookClientId = getString(KEY_OUTLOOK_CLIENT_ID);
        String successText = flash(KEY_SUCCESS);

        Account account = Account.getAccountFromSession();

        List<OAuthAccount> accountList = new OAuthDBAccessor(getDatabase())
                .getAccounts(account.getUserId());

        if (accountList == null) {
            String reason = "An error occurred while retrieving your accounts. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        return ok(ViewOAuthAccountsPage.render(outlookClientId, successText, accountList));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getAccountsAsJson() {
        Account account = Account.getAccountFromSession();

        List<OAuthAccount> accountList = new OAuthDBAccessor(getDatabase())
                .getAccounts(account.getUserId());

        if (accountList == null) {
            String reason = "An error occurred while retrieving your accounts. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        return ok(new OAuthAccount.Converter().renderAsJsonArray(accountList));
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> loginWithOutlookCallback() {
        return loginWithProviderCallback(outlookAuthClient);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> loginWithGoogleCallback() {
        return wrapInFuture(TODO);
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result deleteOAuthAccount(String accountId) {
        if (Validation.isEmpty(accountId)) {
            String reason = "Missing account ID";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }

        Account account = Account.getAccountFromSession();
        boolean isSuccessful = new OAuthDBAccessor(getDatabase())
                .deleteAccount(account.getUserId(), accountId);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "There was an error deleting your account. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    private CompletionStage<Result> loginWithProviderCallback(OAuthClient client) {
        String code = Validation.string(KEY_CODE, request().queryString());
        if (code == null) {
            String text = "You were sent to this page by a bad link";
            return CompletableFuture.completedFuture(getRedirectFailure(text));
        }

        Account account = Account.getAccountFromSession();

        return client.exchangeCodeForAuthToken(code)
                .thenApplyAsync(token -> {
                    OAuthAccount oAuthAccount = null;
                    if (token != null) {
                        oAuthAccount = getFromFutureQuietly(client.getAccountInformation(token).toCompletableFuture());
                    }
                    return new OAuthTokenAccountWrapper(token, oAuthAccount);
                })
                .thenApplyAsync(wrapper -> {
                    if (wrapper.getAuthToken() == null) {
                        String text = APP_NAME + " was unable to communicate with " + client.getProvider().toString() +
                                ". Please try again or submit a bug report";
                        throw new IllegalArgumentException(text);
                    } else if (wrapper.getAuthAccount() == null) {
                        String text = APP_NAME + " was unable to retrieve your account information from " +
                                client.getProvider().toString() + ". Please try again or submit a bug report";
                        throw new IllegalArgumentException(text);
                    } else {
                        boolean isSuccessful = new OAuthDBAccessor(getDatabase())
                                .createAccount(account.getUserId(), wrapper.getAuthAccount(), wrapper.getAuthToken());
                        if (isSuccessful) {
                            String text = "Your new account has been successfully added";
                            flash(KEY_SUCCESS, text);
                            return redirect(routes.OAuthController.getAccounts());
                        } else {
                            throw new IllegalArgumentException("A server error occurred. Please try again or submit " +
                                    "a bug report");
                        }
                    }
                }, getHttpExecutionContext().current())
                .exceptionally(e -> {
                    logger.error("Error: ", e);
                    if (e.getCause() != null) {
                        e = e.getCause();
                    }
                    return getRedirectFailure(e.getMessage());
                });
    }
}
