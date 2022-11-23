https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.twirl.api.Html;
import play.twirl.api.HtmlFormat;
import scala.collection.mutable.StringBuilder;
import utilities.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An abstract class used as the base for all of the models.
 */
public interface JsonConverter<T> {

    default String escape(String text) {
        if(Validation.isEmpty(text)) {
            return text;
        } else {
            Html html = HtmlFormat.escape(text);
            StringBuilder builder = new StringBuilder();
            html.buildString(builder);
            return builder.toString();
        }
    }

    /**
     * @return A {@link ObjectNode} which represents the state of this object as JSON.
     */
    ObjectNode renderAsJsonObject(T object);

    /**
     * @param objectsToRender The list of objects that will be rendered as an array
     * @return A JSON array containing the JSON objects that were passed into this function.
     */
    default ArrayNode renderAsJsonArray(List<T> objectsToRender) {
        ArrayNode arrayNode = Json.newArray();
        for (T anObjectsToRender : objectsToRender) {
            ObjectNode jsonObject = renderAsJsonObject(anObjectsToRender);
            arrayNode.add(jsonObject);
        }
        return arrayNode;
    }

    T deserializeFromJson(ObjectNode objectNode);

    default List<T> deserializeFromJsonArray(ArrayNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), true)
                .map(o -> this.deserializeFromJson((ObjectNode) o))
                .collect(Collectors.toList());
    }

    static void putStringOrNull(ObjectNode objectNode, String fieldName, String value) {
        if (value != null) {
            objectNode.put(fieldName, value);
        } else {
            objectNode.putNull(fieldName);
        }
    }

    static String getString(JsonNode node, String key) {
        return node.get(key) == null || node.get(key).isNull() ? null : node.get(key).asText();
    }

    static ArrayNode getJsonArrayFromList(List<String> stringList) {
        ArrayNode arrayNode = Json.newArray();
        for (String s : stringList) {
            arrayNode.add(s);
        }
        return arrayNode;
    }

    static List<String> getStringListFromJsonArray(ArrayNode arrayNode) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arrayNode.size(); i++) {
            list.add(arrayNode.get(i).asText());
        }
        return list;
    }

    /**
     * @param arrayNode The array to write
     * @return A JSON formatted string of a JSON array
     */
    static String writeJsonArrayAsStringArray(ArrayNode arrayNode) {
        try {
            return new ObjectMapper()
                    .writeValueAsString(arrayNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
