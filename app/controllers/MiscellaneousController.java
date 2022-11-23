https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import database.MiscellaneousDBAccessor;
import global.authentication.BaseAccountAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.user.BugReport;
import model.user.Suggestion;
import model.user.UserQuote;
import com.typesafe.config.Config;
import play.Environment;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.Validation;
import views.html.misc.*;

import javax.inject.Inject;
import java.util.Map;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class MiscellaneousController extends BaseController {

    @Inject
    public MiscellaneousController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result createSuggestion() {
        Map<String, String[]> map = request().body().asFormUrlEncoded();

        String suggestionText = Validation.string(Suggestion.KEY_TEXT, map);
        if(Validation.isEmpty(suggestionText)) {
            String error = "The suggestion cannot be empty";
            flash(KEY_ERROR, error);
            return internalServerError(CreateSuggestionPage.render());
        }

        String userId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = new MiscellaneousDBAccessor(getDatabase())
                .insertSuggestion(userId, suggestionText);
        if(isSuccessful) {
            flash(KEY_SUCCESS, "Your suggestion has been submitted and will be reviewed");
            return redirect(routes.UserController.getProfilePage());
        } else {
            String error = "There was an error creating your suggestion";
            flash(KEY_ERROR, error);
            return internalServerError(CreateSuggestionPage.render());
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result createBugReport() {
        Map<String, String[]> map = request().body().asFormUrlEncoded();

        String bugReportText = Validation.string(BugReport.KEY_TEXT, map);
        if(Validation.isEmpty(bugReportText)) {
            String error = "The bug report cannot be empty";
            flash(KEY_ERROR, error);
            return internalServerError(CreateBugReportPage.render());
        }

        String userId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = new MiscellaneousDBAccessor(getDatabase())
                .insertBugReport(userId, bugReportText);
        if(isSuccessful) {
            flash(KEY_SUCCESS, "Your bug report has been filed and will be addressed as soon as possible");
            return redirect(routes.UserController.getProfilePage());
        } else {
            String error = "There was an error filing your bug report";
            flash(KEY_ERROR, error);
            return internalServerError(CreateBugReportPage.render());
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result createUserQuote() {
        Map<String, String[]> map = request().body().asFormUrlEncoded();

        String quoteText = Validation.string(UserQuote.KEY_TEXT, map);
        if(Validation.isEmpty(quoteText)) {
            String error = "The quote cannot be empty";
            flash(KEY_ERROR, error);
            return internalServerError(CreateBugReportPage.render());
        }

        String author = Validation.string(UserQuote.KEY_AUTHOR, map);

        String userId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = new MiscellaneousDBAccessor(getDatabase())
                .insertUserQuote(userId, quoteText, author);
        if(isSuccessful) {
            flash(KEY_SUCCESS, "Your quote will be reviewed before being featured");
            return redirect(routes.UserController.getProfilePage());
        } else {
            String error = "There was an error submitting your quote";
            flash(KEY_ERROR, error);
            return internalServerError(CreateBugReportPage.render());
        }
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getCreateSuggestionPage() {
        return ok(CreateSuggestionPage.render());
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getReportBugPage() {
        return ok(CreateBugReportPage.render());
    }

    @Security.Authenticated(BaseAccountAuthenticator.class)
    public Result getCreateUserQuotePage() {
        return ok(CreateUserQuotePage.render());
    }

}
