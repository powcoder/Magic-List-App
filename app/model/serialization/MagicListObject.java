https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.PagedList;
import model.prospect.Prospect;
import play.libs.Json;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Corey Caplan on 9/4/17.
 */
public class MagicListObject {

    public static final String KEY_NOTES = "notes";
    public static final String KEY_DATE = "date";

    private static final Gson GSON_REGULAR = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .registerTypeAdapter(Prospect.class, new MagicListDeserializer.ProspectInstanceCreator())
            .registerTypeAdapter(PagedList.class, new MagicListDeserializer.PagedListSerializer())
            .create();

    private static final Gson GSON_PRETTY_PRINT = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(Prospect.class, new MagicListDeserializer.ProspectInstanceCreator())
            .registerTypeAdapter(PagedList.class, new MagicListDeserializer.PagedListSerializer())
            .create();

    public static <T extends MagicListObject> String serializeToJson(T object) {
        return GSON_REGULAR.toJson(object);
    }

    public static <T extends MagicListObject> String serializeToJson(List<T> objectList) {
        return GSON_REGULAR.toJson(objectList);
    }

    public static <T> String prettyPrint(T object) {
        return GSON_PRETTY_PRINT.toJson(object);
    }

    public static <T> String prettyPrint(List<T> objectList) {
        return GSON_PRETTY_PRINT.toJson(objectList);
    }

    public static <T> T deserializeFromJson(JsonNode object, Class<T> clazz) throws JsonSyntaxException {
        return GSON_REGULAR.fromJson(Json.stringify(object), clazz);
    }

    public static <T extends MagicListObject> List<T> deserializeListFromJson(JsonNode object, Class<T[]> clazz) throws JsonSyntaxException {
        return Arrays.asList(GSON_REGULAR.fromJson(Json.stringify(object), clazz));
    }

}
