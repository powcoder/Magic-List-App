https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import model.serialization.MagicListObject;

import java.io.Serializable;

/**
 * Created by Corey Caplan on 9/6/17.
 */
public class DialSheetContactType extends MagicListObject implements Serializable {

    private static final long serialVersionUID = 1743432833213434L;

    private final String stateType;
    private String stateDescription;
    private final String stateClass;
    private final int stateCount;
    private final boolean isIndeterminate;
    private final boolean isObjection;
    private final boolean isInventory;

    public DialSheetContactType(String contactStatus, String stateDescription, String stateClass, int stateCount,
                                boolean isIndeterminate, boolean isObjection, boolean isInventory) {
        this.stateType = contactStatus;
        this.stateDescription = stateDescription;
        this.stateClass = stateClass;
        this.stateCount = stateCount;
        this.isIndeterminate = isIndeterminate;
        this.isObjection = isObjection;
        this.isInventory = isInventory;
    }

    public String getStateType() {
        return stateType;
    }

    public String getStateDescription() {
        return stateDescription;
    }

    public String getStateClass() {
        return stateClass;
    }

    public int getStateCount() {
        return stateCount;
    }

    public boolean isIndeterminate() {
        return isIndeterminate;
    }

    public boolean isObjection() {
        return isObjection;
    }

    public boolean isInventory() {
        return isInventory;
    }
}
