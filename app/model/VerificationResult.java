https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model;

import controllers.BaseController;
import model.account.Account;
import play.mvc.Result;
import utilities.ResultUtility;

/**
 *
 */
public enum VerificationResult {

    SUCCESS, INVALID_TOKEN, NOT_AUTHORIZED, NO_SUBSCRIPTION, NO_PAYMENT_METHODS, INVALID_CREDIT_CARD, SERVER_ERROR,
    EMAIL_NOT_VERIFIED, NOT_BETA_USER;

    public Result getHttpResponse() {
        if (this == INVALID_TOKEN) {
            return BaseController.badRequest(ResultUtility.getNodeForInvalidField(Account.TOKEN));
        } else if (this == NO_SUBSCRIPTION) {
            return BaseController.paymentRequired(ResultUtility.getNodeForNoSubscription());
        } else if (this == INVALID_CREDIT_CARD) {
            return BaseController.paymentRequired(ResultUtility.getNodeForInvalidCreditCard());
        } else if (this == SERVER_ERROR) {
            String reason = "A server error occurred";
            return BaseController.internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        } else if (this == EMAIL_NOT_VERIFIED) {
            String reason = "Your email is not verified";
            return BaseController.unauthorized(ResultUtility.getNodeForBooleanResponse(reason));
        } else if (this == NO_PAYMENT_METHODS) {
            return BaseController.paymentRequired(ResultUtility.getNodeForInvalidCreditCard());
        } else if (this == NOT_AUTHORIZED) {
            String reason = "You are not authorized to perform this action";
            return BaseController.forbidden(ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            return BaseController.ok(ResultUtility.getNodeForBooleanResponse(true));
        }
    }
}
