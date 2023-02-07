package nz.t1d.datamodels

import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.LocalDateTime
import kotlin.test.assertEquals

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ModelsTest {
    @Test fun Initializes() {
        GlucoseReading(value=1.2f, time=LocalDateTime.now())
        Carb(value=1.2f, time=LocalDateTime.now())
        Bolus(value=1.2f, time=LocalDateTime.now())
        BasalChange(value=1.2f, time=LocalDateTime.now())
    }
    
    @Test fun SerializesDeserializes() {
        val now = LocalDateTime.now()
        val model = Data(
            glucose_reading = listOf(GlucoseReading(value=1.2f, time=now)),
            carb            = listOf(Carb(value=1.2f, time=now)),
            bolus           = listOf(Bolus(value=1.2f, time=now)),
            basal_change    = listOf(BasalChange(value=1.2f, time=now))
        )

        val json = Json.encodeToString(model)
        println(json)
        val newModel = Json.decodeFromString<Data>(json)

        assertEquals(model, newModel)
    }
}


