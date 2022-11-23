https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.prospect;

import model.serialization.MagicListObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProspectState extends MagicListObject implements Serializable {

    public enum ClickType {
        PERSON, NOTIFICATIONS, APPOINTMENTS;

        public String getRawText() {
            return this.toString().toLowerCase();
        }
    }

    public enum StateClass {
        APPOINTMENT_CONTACT_STATUS, INDETERMINATE_CONTACT_STATUS, INVENTORY_CONTACT_STATUS,
        MISSED_APPOINTMENT_CONTACT_STATUS, OBJECTION_CONTACT_STATUS;

        public String getCssClass() {
            return this.toString().toLowerCase().replace("_", "-");
        }
    }

    private static final long serialVersionUID = -273819237123L;

    private static final String KEY_NOT_CONTACTED = "not_contacted";
    private static final String KEY_NOT_CONTACTED_DESCRIPTION = "Not Contacted";

    public static final ProspectState NOT_CONTACTED = new ProspectState(KEY_NOT_CONTACTED, KEY_NOT_CONTACTED_DESCRIPTION,
            true, false, false, false, false,
            false, false, false, "not-contacted", 0);

    private final String stateType;
    private final String stateDescription;
    private final boolean isDialSheetState;
    private final boolean isIndeterminate;
    private final boolean isObjection;
    private final boolean isIntroduction;
    private final boolean isInventory;
    private final boolean isMissedAppointment;
    private final boolean isParent;
    private final boolean isChild;
    private final String stateClass;
    private final int index;

    private List<ProspectState> children = null;

    public ProspectState(String stateType, String stateDescription, boolean isDialSheetState, boolean isIndeterminate,
                         boolean isObjection, boolean isIntroduction, boolean isInventory, boolean isMissedAppointment,
                         boolean isParent, boolean isChild, String stateClass, int index) {
        this.stateType = stateType;
        this.stateDescription = stateDescription;
        this.isDialSheetState = isDialSheetState;
        this.isIndeterminate = isIndeterminate;
        this.isObjection = isObjection;
        this.isIntroduction = isIntroduction;
        this.isInventory = isInventory;
        this.isMissedAppointment = isMissedAppointment;
        this.isParent = isParent;
        this.isChild = isChild;
        this.stateClass = stateClass;
        this.index = index;
    }

    public String getStateType() {
        return stateType;
    }

    public String getStateDescription() {
        return stateDescription;
    }

    public boolean isDialSheetState() {
        return isDialSheetState;
    }

    public boolean isIndeterminate() {
        return isIndeterminate;
    }

    public boolean isObjection() {
        return isObjection;
    }

    public boolean isIntroduction() {
        return isIntroduction;
    }

    public boolean isInventory() {
        return isInventory;
    }

    public boolean isMissedAppointment() {
        return isMissedAppointment;
    }

    public boolean isParent() {
        return isParent;
    }

    public boolean isChild() {
        return isChild;
    }

    public List<ProspectState> getChildren() {
        return children;
    }

    public void setChildren(List<ProspectState> prospectStates) {
        this.children = prospectStates;
    }

    public String getStateClass() {
        return stateClass;
    }

    /**
     * @return The position at which the given state should be in the list
     */
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return stateDescription;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ProspectState) && ((ProspectState) obj).stateType.equals(this.stateType);
    }

    @Override
    public int hashCode() {
        return stateType.hashCode();
    }

    /**
     * Converts this 1-D list of prospect states into a list of states with parents and children
     *
     * @param states All of the prospect states.
     */
    public static void convertProspectStatesToParentLists(List<ProspectState> states) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).isParent()) {
                ProspectState parent = states.get(i);
                i += 1; // move i one forward
                List<ProspectState> children = new ArrayList<>();
                while (i < states.size() && states.get(i).isChild()) {
                    children.add(states.remove(i));
                }
                parent.setChildren(children);
                i -= 1; // move i one backward in anticipation of the for loop moving it forward
            }
        }
    }

}
