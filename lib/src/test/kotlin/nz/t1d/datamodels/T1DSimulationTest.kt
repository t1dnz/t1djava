package nz.t1d.datamodels

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.time.LocalDateTime
import nz.t1d.datamodels.T1DModel
import nz.t1d.datamodels.Data

class T1DModelTest {

    @Test fun initializedWithSaneVariables() {
        val sim = T1DModel.Builder().bilinearIOBModel().build()
        // lists
        assertTrue(sim.insulinBoluses().isEmpty())
        assertTrue(sim.insulinBasalChanges().isEmpty())
        assertTrue(sim.bglReadings().isEmpty())
        assertTrue(sim.carbs().isEmpty())
        assertTrue(sim.recentEvents().isEmpty())

        // defaults
        assertEquals(sim.insulinDuration(), 180f)
        assertEquals(sim.insulinOnset(), 20f)
        assertEquals(sim.insulinPeak(), 60f)

        // no info
        
        assertEquals(sim.estimateBolusIOB(), 0f)
        assertEquals(sim.estimateBasalIOB(), 0f)
        assertEquals(sim.estimateIOB(), 0f)
        assertEquals(sim.insulinCurrentBasal(), 0f)

        assertEquals(sim.timeInRange(), 0f)
        assertEquals(sim.meanBGL(), 0f)
        assertEquals(sim.stdBGL(), 0f)
    }

    // @Test fun returnsBasicValues() {
    //     val sim = T1DModel.Builder.build()
        
    //     val dc = T1DInputs()

    //     dc.insulinBoluses.add(BolusInsulin(12f, LocalDateTime.now()))
        
    //     sim.addPatientData(dc)

    //     assertEquals(sim.insulinOnBoard, 12f)

    //     // addBGlReading
    //     // 

    // }

    // @Test fun insulinSettingsCanChange() {
    //     val sim = T1DModel()
    //     sim.insulinDuration = 100f
    //     sim.insulinOnset =  10f
    //     sim.insulinPeak = 20f

    //     assertEquals(sim.insulinDuration, 100f)
    // }


}
