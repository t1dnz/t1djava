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
            glucose_readings = sortedSetOf(timeOrder, GlucoseReading(value=1.2f, time=now)),
            carbs            = sortedSetOf(timeOrder, Carb(value=1.2f, time=now)),
            boluses          = sortedSetOf(timeOrder, Bolus(value=1.2f, time=now)),
            basal_changes    = sortedSetOf(timeOrder, BasalChange(value=1.2f, time=now))
        )


        val json = Json.encodeToString(model)
        val newModel = Json.decodeFromString<Data>(json)

        println(model)
        println(json)
        println(newModel)
        assertEquals(model, newModel)
    }
}


