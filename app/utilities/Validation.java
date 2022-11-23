https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import controllers.routes;
import jdk.nashorn.internal.ir.ObjectNode;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static controllers.BaseController.KEY_ERROR;
import static model.account.Account.*;
import static model.account.Account.ACCOUNT_COMPANY_NAME;
import static model.account.Account.STRIPE_PLAN_ID;
import static play.mvc.Results.redirect;

/**
 * A utility class that holds methods for validating JSON and query parameters from the requests.
 */
public final class Validation {

    private Validation() {
    }

    /**
     * @param fieldName The name of the field in JSON that should be extracted from the node.
     * @param object    The {@link Map} (of types String, String; String, String[]; String, List[String];
     *                  OR {@link JsonNode} from which the key's value should be extracted.
     * @return The number that maps to the field name or -1 if it cannot be parsed.
     */
    public static <T> int integer(String fieldName, T object) {
        String value = string(fieldName, object);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * @param fieldName The name of the field in JSON that should be extracted from the node.
     * @param object    The {@link Map} (of types String, String; String, String[]; String, List[String];
     *                  OR {@link JsonNode} from which the key's value should be extracted.
     * @return The page that maps to the field name or 1 if it cannot be parsed, or is below 1.
     */
    public static <T> int page(String fieldName, T object) {
        int page = integer(fieldName, object);
        return page < 1 ? 1 : page;
    }

    /**
     * @param fieldName The name of the field in JSON that should be extracted from the node.
     * @param object    The {@link Map} (of types String, String; String, String[]; String, List[String];
     *                  OR {@link JsonNode} from which the key's value should be extracted.
     * @return The String that maps to the field name or -1 if the field is empty or invalid.
     */
    public static <T> long getLong(String fieldName, T object) {
        String value = string(fieldName, object);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * @param fieldName The name of the field in JSON that should be extracted from the node.
     * @param object    The {@link Map} (of types String, String; String, String[]; String, List[String];
     *                  OR {@link JsonNode} from which the key's value should be extracted.
     * @return The boolean that maps to the field name or false if the field is empty malformed.
     */
    public static <T> boolean bool(String fieldName, T object) {
        return bool(fieldName, object, false);
    }


    /**
     * @param fieldName The name of the field in JSON that should be extracted from the node.
     * @param object    The query map whose data should be extracted.
     * @return The boolean that maps to the field name or false if the field is empty malformed.
     */
    public static <T> boolean bool(String fieldName, T object, boolean defaultValue) {
        String value = string(fieldName, object);
        if (value == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<String> array(String keyName, T object) {
        if (object instanceof JsonNode) {
            JsonNode node = (JsonNode) object;
            if (node.has(keyName) && node.get(keyName).getNodeType() == JsonNodeType.ARRAY) {
                return StreamSupport.stream(node.get(keyName).spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else if (object instanceof Map) {
            Object value = ((Map) object).get(keyName);
            if (value instanceof List) {
                return (List<String>) ((List) value).stream()
                        .filter(Predicates.instanceOf(String.class)::apply)
                        .collect(Collectors.toList());
            } else if (value instanceof String[]) {
                return Arrays.stream((String[]) value).collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Gets a given String from a map OR JSON node, if there is one, or returns null.
     *
     * @param keyName The name of the key to extract from the map.
     * @param object  The {@link Map} (of types String, String; String, String[]; String, List[String];
     *                OR {@link JsonNode} from which the key's value should be extracted.
     * @return The given string that maps to the key or null if there is no such mapping.
     */
    public static <T> String string(String keyName, T object) {
        if (object instanceof JsonNode) {
            JsonNode node = (JsonNode) object;
            if (node.has(keyName)) {
                return node.get(keyName).asText();
            } else {
                return null;
            }
        } else if (object instanceof Map) {
            Map map = (Map) object;
            if (!map.containsKey(keyName)) {
                return null;
            }

            Object value = map.get(keyName);
            if (value instanceof List) {
                return (String) ((List<?>) value).stream()
                        .filter(Predicates.instanceOf(String.class)::apply)
                        .findFirst()
                        .orElse(null);
            } else if (value instanceof String[]) {
                return Arrays.stream(((String[]) value))
                        .findFirst()
                        .orElse(null);
            } else if (value instanceof String) {
                return (String) value;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * @param value The value to be checked if empty
     * @return True if empty (null or trimmed length is 0), false otherwise
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

}
