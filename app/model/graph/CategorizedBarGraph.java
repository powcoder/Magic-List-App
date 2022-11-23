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
 * Created by Corey on 7/2/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class CategorizedBarGraph extends GraphStatistic {

    private final List<Pair<String, Double>> coordinatePairs;

    private ArrayNode labelsArray;
    private String xAxisTitle;

    public CategorizedBarGraph(String title, List<Pair<String, Double>> coordinatePairs) {
        super(GraphType.BAR, title);
        this.coordinatePairs = coordinatePairs;
    }

    @Override
    void renderAsJsonObject(ObjectNode baseNode) {
        final ArrayNode datasetsNode = baseNode.putArray(KEY_DATA_SETS);
        labelsArray = baseNode.putArray(KEY_LABELS);
        if(labelsArray.size() == 0) {
            labelsArray.add(xAxisTitle);
        }

        coordinatePairs.forEach(coordinatePair -> {
            datasetsNode.addObject()
                    .put(KEY_BORDER_WIDTH, 1)
                    .put(KEY_LABEL, coordinatePair.first())
                    .putArray(KEY_DATA)
                    .add(coordinatePair.second());
        });
    }

    public CategorizedBarGraph setXAxisTitle(String xAxisTitle) {
        if (labelsArray != null) {
            labelsArray.add(xAxisTitle);
        } else {
            this.xAxisTitle = xAxisTitle;
        }
        return this;
    }

}
