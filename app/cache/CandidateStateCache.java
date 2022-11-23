https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package cache;

import controllers.BaseController;
import database.ManagerCandidateDBAccessor;
import model.manager.CandidateState;

import java.util.List;
import java.util.Optional;

/**
 * Created by Corey Caplan on 10/19/17.
 */
public class CandidateStateCache {

    private static final String KEY_ALL_CANDIDATE_STATES = "all_candidate_states";

    public static List<CandidateState> getAllCandidateStates(String companyName) {
        MemCacheWrapper wrapper = MemCacheWrapper.getInstance();
        String key = KEY_ALL_CANDIDATE_STATES + companyName;
        List<CandidateState> candidateStates = wrapper.get(key);
        return Optional.ofNullable(candidateStates)
                .orElseGet(() -> {
                    List<CandidateState> states = new ManagerCandidateDBAccessor(BaseController.getDatabase())
                            .getAllCandidateStates(companyName);
                    wrapper.set(key, MemCacheWrapper.FIFTEEN_MINUTES, states);
                    return states;
                });
    }

}
