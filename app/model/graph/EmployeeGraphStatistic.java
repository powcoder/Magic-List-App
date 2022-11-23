https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Created by Corey on 6/26/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public abstract class EmployeeGraphStatistic extends GraphStatistic {

    public static final String KEY_ITEM_ID_INDEX_ARRAY = "item_id_index_array";
    public static final String KEY_ITEM_ID = "item_id";
    public static final String KEY_ITEM_IDS = "item_ids";
    public static final String KEY_IS_EMPLOYEE_GRAPH = "is_employee_graph";

    public EmployeeGraphStatistic(GraphType type, String title) {
        super(type, title);
    }

    @Override
    final void renderAsJsonObject(ObjectNode baseNode) {
        ArrayNode indexArray = baseNode.put(KEY_IS_EMPLOYEE_GRAPH, true)
                .putArray(KEY_ITEM_ID_INDEX_ARRAY);
        getEmployeeIdList().forEach(indexArray::add);

        renderBaseEmployeeObject(baseNode);
    }

    abstract void renderBaseEmployeeObject(ObjectNode baseNode);

    abstract List<String> getEmployeeIdList();

}
