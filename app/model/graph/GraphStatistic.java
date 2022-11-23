https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;
import play.twirl.api.Html;
import play.twirl.api.HtmlFormat;
import scala.collection.mutable.StringBuilder;
import utilities.RandomStringGenerator;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public abstract class GraphStatistic {

    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_GRAPH_DATA = "graph_data";
    static final String KEY_DATA_SETS = "datasets";
    static final String KEY_LABELS = "labels";
    static final String KEY_LABEL = "label";
    static final String KEY_DATA = "data";
    static final String KEY_BORDER_WIDTH = "borderWidth";
    static final String KEY_FILL = "fill";
    static final String KEY_X = "x";
    static final String KEY_Y = "y";

    private final GraphType type;
    private final String title;

    public GraphStatistic(GraphType type, String title) {
        this.type = type;
        this.title = title;
    }

    public final ObjectNode renderAsJsonObject() {
        ObjectNode node = Json.newObject()
                .put(KEY_TYPE, type.toString());

        ObjectNode dataNode = node.putObject(KEY_GRAPH_DATA);
        dataNode.put(KEY_TITLE, title);
        renderAsJsonObject(dataNode);

        return node;
    }

    /* Package-Private */
    abstract void renderAsJsonObject(ObjectNode baseNode);

    /* Package-Private */ final String escape(String s) {
        Html html = HtmlFormat.escape(s);
        StringBuilder builder = new StringBuilder();
        html.buildString(builder);
        return builder.toString();
    }

}
