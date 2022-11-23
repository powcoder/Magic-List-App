https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import static play.libs.Json.newObject;

/**
 * A utility class that helps make responses from the server more uniform.
 */
public class ResultUtility {

    /**
     * JSON Field for the simple boolean response sent back from the server.
     */
    public static final String JSON_SUCCESS = "success";

    /**
     * @return A node used to denote the user not being logged in, or having invalid credentials.
     */
    public static ObjectNode getUnauthorizedNode() {
        return newObject()
                .put("unauthorized", true);
    }

    /**
     * Json field used to map a description attribute to error objects.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * @param fieldName The name of the field that's missing from a given request.
     * @return A JSON object that can be returned as part of the error, saying that the request is missing the given
     * field name.
     */
    public static ObjectNode getNodeForMissingField(String fieldName) {
        return Json.newObject()
                .put("error", "missing field")
                .put("field_name", fieldName);
    }

    /**
     * @param fieldName The name of the field that's missing from a given request.
     * @return A JSON object that can be returned as part of the error, saying that the request is missing the given
     * field name.
     */
    public static ObjectNode getNodeForInvalidField(String fieldName) {
        return Json.newObject()
                .put("error", "Invalid field must be nonnull and adhere to criteria")
                .put("field_name", fieldName);
    }

    public static ObjectNode getNodeForBooleanResponse(boolean isSuccessful) {
        return Json.newObject()
                .put(JSON_SUCCESS, isSuccessful);
    }

    public static ObjectNode getNodeForBooleanResponse(String failureReason) {
        return Json.newObject()
                .put(JSON_SUCCESS, false)
                .put("reason", failureReason);
    }

    public static ObjectNode getNodeForNoSubscription() {
        return Json.newObject()
                .put(JSON_SUCCESS, false)
                .put("code", "no_subscription");
    }

    public static ObjectNode getNodeForInvalidCreditCard() {
        return Json.newObject()
                .put(JSON_SUCCESS, false)
                .put("code", "invalid_card");
    }

}
