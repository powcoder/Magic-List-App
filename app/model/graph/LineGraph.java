https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import java.util.List;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class LineGraph extends GraphStatistic {

    private final List<Pair<Long, Double>> coordinatePairList;

    /**
     * @param coordinatePairList A list containing x/y coordinate pairs. The first item in the pairs object represent
     *                           UNIX Epoch millis, and the second item represents the data.
     */
    public LineGraph(String title, List<Pair<Long, Double>> coordinatePairList) {
        super(GraphType.LINE, title);
        this.coordinatePairList = coordinatePairList;
    }

    @Override
    public void renderAsJsonObject(ObjectNode baseNode) {
        ArrayNode datasetsArray = Json.newArray();

        coordinatePairList.forEach(pair -> {
            ArrayNode dataArray = Json.newArray();

            ObjectNode coordinateObject = Json.newObject()
                    .put(KEY_X, pair.first())
                    .put(KEY_Y, pair.second());

            dataArray.add(coordinateObject);

            datasetsArray.add(Json.newObject()
                    .put(KEY_BORDER_WIDTH, 3)
                    .put(KEY_FILL, false)
                    .set(KEY_DATA, dataArray));

        });

        baseNode.set(KEY_DATA_SETS, datasetsArray);
    }

}
