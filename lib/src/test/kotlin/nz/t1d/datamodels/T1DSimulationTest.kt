package nz.t1d.datamodels

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.time.LocalDateTime
import nz.t1d.datamodels.T1DModel
import nz.t1d.datamodels.Data

class T1DModelTest {

    @Test fun initializedWithSaneVariables() {
        val profile = Profile()
        val t1d = T1DModel.Builder().build(profile)
        // lists
        assertTrue(t1d.insulinBoluses().isEmpty())
        assertTrue(t1d.insulinBasalChanges().isEmpty())
        assertTrue(t1d.bglReadings().isEmpty())
        assertTrue(t1d.carbs().isEmpty())
        assertTrue(t1d.recentEvents().isEmpty())

        // defaults
        assertEquals(t1d.insulinDuration(), 180f)
        assertEquals(t1d.insulinOnset(), 20f)
        assertEquals(t1d.insulinPeak(), 60f)

        // no info
        
        assertEquals(t1d.bolusIOB(), 0f)
        assertEquals(t1d.basalIOB(), 0f)
        assertEquals(t1d.IOB(), 0f)
        assertEquals(t1d.insulinCurrentBasal(), 0f)

        assertEquals(t1d.timeInRange(), 0f)
        assertEquals(t1d.meanBGL(), 0f)
        assertEquals(t1d.stdBGL(), 0f)
    }

    @Test fun returnsBasicValues() {
        val t1d = T1DModel.Builder().build()
        
        val dc = Data()

        dc.boluses.add(Bolus(12f, LocalDateTime.now()))
        
        t1d.addData(dc)

        assertEquals(t1d.IOB(), 12f)

        // addBGlReading
        // 

    }

    // @Test fun insulinSettingsCanChange() {
    //     val t1d = T1DModel.Builder.build()
    //     t1d.insulinDuration = 100f
    //     t1d.insulinOnset =  10f
    //     t1d.insulinPeak = 20f

    //     assertEquals(t1d.insulinDuration, 100f)
    // }


}
