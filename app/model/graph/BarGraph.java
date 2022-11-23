https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class BarGraph extends GraphStatistic {

    private final List<Pair<String, Double>> coordinatePairList;

    /**
     * @param coordinatePairList A list containing pairs of bar chart labels and values. The first item in the pairs
     *                           object represent the graph's labels, and the second item represents the data.
     */
    public BarGraph(String title, List<Pair<String, Double>> coordinatePairList) {
        super(GraphType.BAR, title);
        this.coordinatePairList = coordinatePairList;
    }

    @Override
    public void renderAsJsonObject(ObjectNode baseNode) {
        ArrayNode labelsNode = baseNode.putArray(KEY_LABELS);
        coordinatePairList.stream()
                .map(Pair::first)
                .forEach(label -> labelsNode.add(escape(label)));

        ArrayNode datasetsNode = baseNode.putArray(KEY_DATA_SETS);

        ObjectNode dataObject = datasetsNode.addObject();

        dataObject.put(KEY_BORDER_WIDTH, 1);
        ArrayNode dataObjectDataNode = dataObject.putArray(KEY_DATA);
        coordinatePairList.stream().map(Pair::second).forEach(dataObjectDataNode::add);
    }
}
