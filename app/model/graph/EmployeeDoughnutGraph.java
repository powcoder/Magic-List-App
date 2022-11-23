https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class EmployeeDoughnutGraph extends EmployeeGraphStatistic {

    private final List<Pair<String, Double>> coordinatePairs;

    /**
     * @param coordinatePairs A list of coordinates containing the user's ID mapped to the data (as a double)
     */
    public EmployeeDoughnutGraph(String title, List<Pair<String, Double>> coordinatePairs) {
        super(GraphType.DOUGHNUT, title);
        this.coordinatePairs = coordinatePairs;
    }

    @Override
    void renderBaseEmployeeObject(ObjectNode baseNode) {
        DoughnutGraph.renderJsonObject(baseNode, coordinatePairs);

        ArrayNode itemIdsNode = ((ObjectNode) baseNode.get(KEY_DATA_SETS).get(0)).putArray(KEY_ITEM_IDS);
        coordinatePairs.stream().map(Pair::first).forEach(itemIdsNode::add);

        baseNode.putNull(KEY_LABELS);
    }

    @Override
    List<String> getEmployeeIdList() {
        return coordinatePairs.stream().map(Pair::first).collect(Collectors.toList());
    }
}
