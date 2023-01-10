package nz.t1d.datamodels

import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.LocalDateTime

class BasalTest {
    @Test fun Initializes() {
        val model = BasalInsulinChange(value=1.2f, time=LocalDateTime.now())
    }
}
