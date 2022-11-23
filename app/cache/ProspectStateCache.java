https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package cache;

import controllers.BaseController;
import database.ProspectStateDBAccessor;
import model.prospect.ProspectState;

import java.util.List;
import java.util.Optional;

/**
 * Created by Corey Caplan on 10/19/17.
 */
public class ProspectStateCache {

    private static final String KEY_ALL_PERSON_STATES = "all_person_states";
    private static final String KEY_ALL_PERSON_STATES_WITH_CHILDREN = "all_person_states_with_children";
    private static final String KEY_OBJECTION_STATES = "objection_states";

    /**
     * @param companyName N
     * @return
     */
    public static List<ProspectState> getAllStates(String companyName) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        String key = KEY_ALL_PERSON_STATES + companyName;
        List<ProspectState> prospectStates = wrapper.get(key);
        return Optional.ofNullable(prospectStates)
                .orElseGet(() -> {
                    List<ProspectState> states = new ProspectStateDBAccessor(BaseController.getDatabase()).getAllStates(companyName);
                    wrapper.set(key, MemCacheWrapper.FIFTEEN_MINUTES, states);
                    return states;
                });
    }

    public static List<ProspectState> getAllStatesWithChildren(String companyName) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        String key = KEY_ALL_PERSON_STATES_WITH_CHILDREN + companyName;
        List<ProspectState> stateList = wrapper.get(key);
        return Optional.ofNullable(stateList)
                .orElseGet(() -> {
                    List<ProspectState> states = new ProspectStateDBAccessor(BaseController.getDatabase()).getAllStates(companyName);
                    ProspectState.convertProspectStatesToParentLists(states);
                    wrapper.set(key, MemCacheWrapper.FIFTEEN_MINUTES, states);
                    return states;
                });
    }

    public static List<ProspectState> getAllDialSheetStates(String companyName) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        String key = KEY_OBJECTION_STATES + companyName;
        List<ProspectState> states = wrapper.get(key);
        return Optional.ofNullable(states)
                .orElseGet(() -> {
                    List<ProspectState> stateList = new ProspectStateDBAccessor(BaseController.getDatabase())
                            .getAllPagesDialSheetObjectionStates(companyName);
                    wrapper.set(key, MemCacheWrapper.FIFTEEN_MINUTES, stateList);
                    return stateList;
                });
    }

}
