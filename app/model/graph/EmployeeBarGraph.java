https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Corey on 6/22/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class EmployeeBarGraph extends EmployeeGraphStatistic {

    private final List<Data> employeeDataList;
    private final List<String> employeeIdList;

    /**
     * @param title            The graph's title
     * @param employeeDataList A list of data that corresponds to each bar graph label mapping to a map of employee
     *                         IDs to the corresponding singular piece of data.
     * @param employeeIdList   A list containing the employee IDs that are present in the bar graph
     */
    public EmployeeBarGraph(String title, List<Data> employeeDataList, List<String> employeeIdList) {
        super(GraphType.BAR, title);
        this.employeeDataList = employeeDataList;
        this.employeeIdList = employeeIdList;
    }

    @Override
    void renderBaseEmployeeObject(ObjectNode baseNode) {
        final ArrayNode labelsNode = baseNode.putArray(KEY_LABELS);
        final ArrayNode dataSetsNode = baseNode.putArray(KEY_DATA_SETS);

        // Maps employee IDs to their corresponding object node
        Map<String, ObjectNode> employeeObjectNodeMap = new HashMap<>();

        employeeDataList.forEach(employeeData -> {
            labelsNode.add(escape(employeeData.label));

            employeeIdList.forEach(employeeId -> {
                ArrayNode dataNode;
                if (!employeeObjectNodeMap.containsKey(employeeId)) {
                    ObjectNode employeeNode = dataSetsNode.addObject()
                            .put(KEY_BORDER_WIDTH, 1)
                            .put(KEY_ITEM_ID, employeeId);
                    dataNode = employeeNode.putArray(KEY_DATA);

                    employeeObjectNodeMap.put(employeeId, employeeNode);
                } else {
                    dataNode = (ArrayNode) employeeObjectNodeMap.get(employeeId).get(KEY_DATA);
                }

                Double data = 0.0;
                if (employeeData.employeeDataMap.containsKey(employeeId)) {
                    data = employeeData.employeeDataMap.get(employeeId);
                }

                dataNode.add(data);
            });
        });
    }

    @Override
    List<String> getEmployeeIdList() {
        return employeeIdList;
    }

    public static class Data {

        private final String label;
        private final Map<String, Double> employeeDataMap;

        /**
         * @param label           The label that represents the X axis for the graph
         * @param employeeDataMap A map that corresponds to a map of employees and their corresponding data for
         *                        the given label
         */
        public Data(String label, Map<String, Double> employeeDataMap) {
            this.label = label;
            this.employeeDataMap = employeeDataMap;
        }

    }

}
