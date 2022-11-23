https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import akka.japi.Pair;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import org.junit.Test;
import play.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Corey Caplan on 11/11/17.
 */
public class DialSheetAppointmentTest {

    private static final Logger.ALogger logger = Logger.of(DialSheetAppointmentTest.class);

    private static int id = 0;

    @Test
    public void getAppointmentsByType() throws Exception {
        List<DialSheetAppointment> appointmentList = Arrays.asList(
                getIntroDummy(), getIntroDummy(), getIntroDummy(), getIntroDummy(),
                getDiscoveryDummy(), getDiscoveryDummy(), getDiscoveryDummy(),
                getStrategyDummy(), getStrategyDummy(), getStrategyDummy(),
                getCloseDummy(), getCloseDummy(), getCloseDummy(), getCloseDummy(),
                getReviewDummy(), getReviewDummy(), getReviewDummy(), getReviewDummy(), getReviewDummy(), getReviewDummy()
        );

        List<Pair<ProspectState, List<DialSheetAppointment>>> appointmentsByType = DialSheetAppointment.getAppointmentsByType(appointmentList);

        assertEquals(5, appointmentsByType.size());

        logger.info("Appointments by type: {}", MagicListObject.prettyPrint(appointmentsByType));

        for (int i = 0; i < appointmentsByType.size(); i++) {
            Pair<ProspectState, List<DialSheetAppointment>> pair = appointmentsByType.get(i);
            String stateType = pair.first().getStateType();
            int size = pair.second().size();

            if (i == 0) {
                assertEquals("introduction", stateType);
                assertEquals(4, size);
            } else if (i == 1) {
                assertEquals("profile", stateType);
                assertEquals(3, size);
            } else if (i == 2) {
                assertEquals("strategy", stateType);
                assertEquals(3, size);
            } else if (i == 3) {
                assertEquals("close", stateType);
                assertEquals(4, size);
            } else if (i == 4) {
                assertEquals("review", stateType);
                assertEquals(6, size);
            }
        }

    }

    private static DialSheetAppointment getDummyByType(String stateType, ProspectState.StateClass stateClass, int index) {
        String appointmentId = "appointment_" + id;

        boolean isIntroduction = stateType.equalsIgnoreCase("introduction");

        ProspectState appointmentType = new ProspectState(stateType, stateType, false, false,
                false, isIntroduction, true, false, false, true,
                stateClass.getCssClass(), index);

        Prospect prospect = Prospect.Factory.createDummy("prospect_" + id);

        String notes = "Some appt notes...";

        id += 1;

        return new DialSheetAppointment(appointmentId, true, new Date().getTime(), prospect, notes,
                appointmentType, ProspectState.NOT_CONTACTED);
    }

    private static DialSheetAppointment getIntroDummy() {
        return getDummyByType("introduction", ProspectState.StateClass.APPOINTMENT_CONTACT_STATUS, 0);
    }

    private static DialSheetAppointment getDiscoveryDummy() {
        return getDummyByType("profile", ProspectState.StateClass.INVENTORY_CONTACT_STATUS, 1);
    }

    private static DialSheetAppointment getStrategyDummy() {
        return getDummyByType("strategy", ProspectState.StateClass.INVENTORY_CONTACT_STATUS, 2);
    }

    private static DialSheetAppointment getCloseDummy() {
        return getDummyByType("close", ProspectState.StateClass.INVENTORY_CONTACT_STATUS, 3);
    }

    private static DialSheetAppointment getReviewDummy() {
        return getDummyByType("review", ProspectState.StateClass.INVENTORY_CONTACT_STATUS, 4);
    }

}