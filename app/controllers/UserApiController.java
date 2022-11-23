https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.EmailClient;
import com.fasterxml.jackson.databind.JsonNode;
import database.AccountDBAccessor;
import global.authentication.BaseAccountAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.ImporterLimit;
import play.mvc.BodyParser;
import play.mvc.BodyParser.Json;
import play.mvc.Result;
import play.mvc.Security;
import utilities.*;

import javax.inject.Inject;

import java.sql.SQLException;

import static model.account.Account.*;

/**
 * The controller used to handle requests relating to the users.
 */
public class UserApiController extends BaseController {

    @Inject
    public UserApiController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    /**
     * The route used to allow the user to change his/her account's password (POST)
     */
    @Security.Authenticated(BaseAccountAuthenticator.class)
    @BodyParser.Of(Json.class)
    public Result changePassword() {
        JsonNode node = request().body().asJson();

        String token = Account.getAccountFromSession().getToken();
        String oldPassword = Validation.string(PASSWORD, node);
        String newPassword = Validation.string(ACCOUNT_NEW_PASSWORD, node);

        if (oldPassword == null) {
            return badRequest(ResultUtility.getNodeForMissingField(PASSWORD));
        }
        if (newPassword == null) {
            return badRequest(ResultUtility.getNodeForMissingField(ACCOUNT_NEW_PASSWORD));
        }

        int result = new AccountDBAccessor(getDatabase())
                .changePassword(token, oldPassword, newPassword);
        if (result == 1) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (result == 0) {
            return unauthorized(ResultUtility.getUnauthorizedNode());
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    /**
     * The route used to allow the user to change his/her account's password (POST)
     */
    @BodyParser.Of(Json.class)
    public Result resetPassword() {
        JsonNode node = request().body().asJson();

        String userId = Validation.string(ACCOUNT_USER_ID, node);
        if (userId == null) {
            return badRequest(ResultUtility.getNodeForMissingField(ACCOUNT_USER_ID));
        }

        String forgotPasswordLink = Validation.string(ACCOUNT_FORGOT_PASSWORD_LINK, node);
        if (forgotPasswordLink == null) {
            return badRequest(ResultUtility.getNodeForMissingField(ACCOUNT_FORGOT_PASSWORD_LINK));
        }

        String password = Validation.string(ACCOUNT_NEW_PASSWORD, node);
        if (password == null) {
            return badRequest(ResultUtility.getNodeForMissingField(ACCOUNT_NEW_PASSWORD));
        }

        boolean didAddLink = new AccountDBAccessor(getDatabase())
                .resetPassword(userId, forgotPasswordLink, password);
        if (didAddLink) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return badRequest(ResultUtility.getNodeForBooleanResponse(false));
        }
    }
}
