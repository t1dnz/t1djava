package nz.t1d.datamodels

import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.math.truncate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder

object DateAsLongSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

////
// Collection Class
////

@Serializable
data class Data(
    var glucose_reading: List<GlucoseReading>,
    var carb: List<Carb>,
    var bolus: List<Bolus>,
    var basal_change: List<BasalChange>,
)

@Serializable
data class Profile(
    var insulin_duration:  Int = 180,
    var insulin_onset: Int =  20,
    var insulin_peak: Int =  10,
)

////
// Data Classes
////
interface BaseDataClass {
    val time: LocalDateTime
    var value: Float

    fun secondsAgo(): Long {
        return Duration.between(time, LocalDateTime.now()).toMillis() / 1000L // toSeconds not supported yet
    }


    fun minsAgo(): Long {
        return Duration.between(time, LocalDateTime.now()).toMinutes()
    }

    fun minsAgoString(): String {
        val mins = minsAgo()
        val hours = truncate(mins / 60.0f).toLong()

        if (hours != 0L) {
            val hourMins = mins - (60 * hours)
            return "${hours}h ${hourMins}m ago"
        }
        return "${mins}m ago"
    }
}

@Serializable
data class BasalChange(
    override var value: Float,
    @Serializable(with = DateAsLongSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass

@Serializable
data class Bolus(
    override var value: Float,
    @Serializable(with = DateAsLongSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass {
    // The carb intake that is associated with this bolus
    var carbIntake: Carb? = null
}

@Serializable
data class Carb(
    override var value: Float,
    @Serializable(with = DateAsLongSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass



enum class DATA_SOURCE{CAMAPS_NOTIF, DIASEND}

@Serializable
data class GlucoseReading(
    override var value: Float,
    @Serializable(with = DateAsLongSerializer::class)
    override var time: LocalDateTime,
    var bglUnit: String = "mmol/L",
) : BaseDataClass {
    var source: DATA_SOURCE = DATA_SOURCE.DIASEND

    // reference to previous reading making this kind of a
    var previousReading: GlucoseReading? = null

    fun calculateDiff(): Float {
        previousReading?.let { pr ->
            val duration = Duration.between(pr.time, time).toMillis() / 1000
            // Calculate rate per min then multiply by 5
            val rate = (value - pr.value) / duration
            return rate * 300
        }
        return 0f
    }


    // Return the BGL reading we trust more to be correct
    fun lessTruthy(that: GlucoseReading) : GlucoseReading {
        if (source == DATA_SOURCE.DIASEND) {
            return that
        }
        if (that.source == DATA_SOURCE.DIASEND) {
            return this
        }

        // At this point they must both be notifs so we just return the older one
        if (time > that.time) {
            return this
        }
        return that
    }

    fun diffString(unit: Boolean = true): String {
        var  diff = calculateDiff()
        val dec = DecimalFormat("+#,##0.0;-#")
        if (unit) {
            return "${dec.format(diff)} $bglUnit"
        }
        return dec.format(diff)
    }
}







