package nz.t1d.datamodels

import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.LocalDateTime

class BGLReadingTest {
    @Test fun Initializes() {
        val model = BGLReading(value=1.2f, time=LocalDateTime.now())
    }
}
