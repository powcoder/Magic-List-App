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
public class EmployeeLineGraph extends EmployeeGraphStatistic {

    private final List<Data> lineGraphDataList;

    /**
     * @param lineGraphDataList A list of lists containing each employee's ID and  x/y coordinate pairs. The first item
     *                          in the pairs object represent UNIX Epoch millis, and the second item represents
     *                          the data.
     */
    public EmployeeLineGraph(String title, List<Data> lineGraphDataList) {
        super(GraphType.LINE, title);
        this.lineGraphDataList = lineGraphDataList;
    }

    @Override
    void renderBaseEmployeeObject(ObjectNode baseNode) {
        ArrayNode datasetsArray = baseNode.putArray(KEY_DATA_SETS);

        lineGraphDataList.forEach(lineGraphData -> {
            ObjectNode datasetsObject = datasetsArray.addObject()
                    .put(KEY_BORDER_WIDTH, 3)
                    .put(KEY_FILL, false)
                    .put(KEY_ITEM_ID, lineGraphData.id);

            ArrayNode dataArray = datasetsObject.putArray(KEY_DATA);

            lineGraphData.coordinateList.forEach(coordinates -> dataArray.addObject()
                    .put(KEY_X, coordinates.first())
                    .put(KEY_Y, coordinates.second()));
        });
    }

    @Override
    List<String> getEmployeeIdList() {
        return lineGraphDataList.stream().map(Data::getId).collect(Collectors.toList());
    }

    public static class Data {

        private final String id;
        private final List<Pair<Long, Double>> coordinateList;

        public Data(String id, List<Pair<Long, Double>> coordinateList) {
            this.id = id;
            this.coordinateList = coordinateList;
        }

        public String getId() {
            return id;
        }
    }

}
