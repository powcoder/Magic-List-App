https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class DoughnutGraph extends GraphStatistic {

    private final List<Pair<String, Double>> coordinatePairs;

    /**
     * @param coordinatePairs A list of pairs containing the label and the data for the given label
     */
    public DoughnutGraph(String title, List<Pair<String, Double>> coordinatePairs) {
        super(GraphType.DOUGHNUT, title);
        this.coordinatePairs = coordinatePairs;
    }

    @Override
    void renderAsJsonObject(ObjectNode baseNode) {
        renderJsonObject(baseNode, coordinatePairs);
    }

    /**
     * @param coordinatePairs A list of pairs containing the label and the data for the given label
     */
    static void renderJsonObject(ObjectNode baseNode, List<Pair<String, Double>> coordinatePairs) {
        ObjectNode datasetsNode = baseNode.putArray(KEY_DATA_SETS)
                .addObject()
                .put(KEY_BORDER_WIDTH, 1);

        ArrayNode dataNode = datasetsNode.putArray(KEY_DATA);
        coordinatePairs.stream().map(Pair::second).forEach(dataNode::add);

        ArrayNode labelsNode = baseNode.putArray(KEY_LABELS);
        coordinatePairs.stream().map(Pair::first).forEach(labelsNode::add);
    }

}
