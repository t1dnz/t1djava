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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

object timeOrder : Comparator<BaseDataClass> {
    override fun compare(p0: BaseDataClass?, p1: BaseDataClass?): Int {
        if (p1 == null || p0 == null) {
            return 0
        }
        return p1.time.compareTo(p0.time)
    }
}

class BaseDataClassSerializer<T: BaseDataClass>(private val dataSerializer: KSerializer<T>): KSerializer<SortedSet<T>> {
    override val descriptor: SerialDescriptor = ListSerializer(dataSerializer).descriptor
    override fun serialize(encoder: Encoder, value: SortedSet<T>) {
        encoder.encodeSerializableValue(ListSerializer(dataSerializer), value.toMutableList())
    }

    override fun deserialize(decoder: Decoder): SortedSet<T> {
        var values = decoder.decodeSerializableValue( ListSerializer(dataSerializer))
        var set: SortedSet<T> = sortedSetOf(timeOrder)
        set.addAll(values)
        return set
    }

}

////
// Collection Class
////

@Serializable
data class Data(
    @Serializable(with = BaseDataClassSerializer::class)
    val glucose_readings: SortedSet<GlucoseReading> = sortedSetOf(timeOrder),
    @Serializable(with = BaseDataClassSerializer::class)
    val carbs: SortedSet<Carb> = sortedSetOf(timeOrder),
    @Serializable(with = BaseDataClassSerializer::class)
    val boluses: SortedSet<Bolus> = sortedSetOf(timeOrder),
    @Serializable(with = BaseDataClassSerializer::class)
    val basal_changes: SortedSet<BasalChange> = sortedSetOf(timeOrder),
) {

    fun merge(pd: Data) {
        // Join all the data together
        glucose_readings.addAll(pd.glucose_readings)
        carbs.addAll(pd.carbs)
        boluses.addAll(pd.boluses)
        basal_changes.addAll(pd.basal_changes)
    }

    fun removeOldData() {
        val now = LocalDateTime.now()
        val midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        val midnightMinus = midnight.minusMinutes(300L)
        
        // Since this program can run for days to make sure we are discarding any really old data we remove it from the sets here
        boluses.removeIf { d -> d.time < midnightMinus }
        carbs.removeIf { d -> d.time < midnightMinus }
        basal_changes.removeIf { d -> d.time < midnightMinus }
        glucose_readings.removeIf { d -> d.time < midnightMinus }
    }

    fun processBGLReadings() {
        // Because we can get notifications from multiple locations we can have ver similar duplicates
        // we remove all readings if they are within 1 minute of each other
        // keeping the diasend reading as priority because it is more truthy

        val bglList = glucose_readings.toList()
        for (i in bglList.indices) {
            val d1 = bglList[i]

            // already been removed, skip
            if (!glucose_readings.contains(d1)) {
                continue
            }

            // Find next value that will return a diff
            for (j in (i + 1)..(bglList.size - 1)) {
                val d2 = bglList[j]

                val secondsDuration = Duration.between(d2.time, d1.time).toMillis() / 1000
                // if the difference in time between readings is less than 150 seconds
                if (secondsDuration < 150) {
                    var notTruthy = d1.lessTruthy(d2)
                    glucose_readings.remove(notTruthy)
                    continue
                }
            }
        }

        // assign the previous reading to each of the bglreadings so they can self calculate diff and such
        var futureReading: GlucoseReading? = null
        for (d in glucose_readings) {
            if (futureReading != null) {
                futureReading.previousReading = d
            }
            futureReading = d
        }
    }

     fun processBasalChanges() {
        if (basal_changes.isEmpty()) {
            return
        }
        // Compress Basal Changes (there are a ton of useless ones)
        var previousMinsAgo: Long = -1
        var previousValue: Float = -1f
        val removeElemets = mutableListOf<BasalChange>()
        for (re in basal_changes) {
            if (re.minsAgo() == previousMinsAgo || re.value == previousValue) {
                removeElemets.add(re)
                continue
            }
            previousMinsAgo = re.minsAgo()
            previousValue = re.value

        }
        for (re in removeElemets) {
            basal_changes.remove(re)
        }
        
    }

     fun joinTogetherBolusInfo() {
        // We want to find events in a time range
        // We want to join the boluses with carbs
        for (bolus in boluses) {
            val closeCarbs: List<Carb> = findEvents(bolus.time, bolus.time.plusMinutes(2), carbs)
            if (!closeCarbs.isEmpty()) {
                bolus.carbIntake = closeCarbs.first()
                carbs.remove(bolus.carbIntake)
            }
        }
    }

    fun todaysBGLReadings(): List<GlucoseReading> {
        val midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        return findEvents(midnight, LocalDateTime.now().plusMinutes(2), glucose_readings)
    }

    fun <T: BaseDataClass> findEvents(from: LocalDateTime, to: LocalDateTime, setOf : SortedSet<T>) : List<T>{
        val list = mutableListOf<T>()
        for (d in setOf) {
            if (d.time < from) {
                return list
            }
            if (d.time > to) {
                continue
            }
            list.add(d)
        }
        return list
    }

    fun addBGlReading(reading: Float, time: LocalDateTime, unit: String, source: DATA_SOURCE) {
        // store previoud data
        val reading = GlucoseReading(reading, time, unit)
        reading.source = source
        glucose_readings.add(reading)
        processBGLReadings()
    }

}

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
    @Serializable(with = LocalDateTimeSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass

@Serializable
data class Bolus(
    override var value: Float,
    @Serializable(with = LocalDateTimeSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass {
    // The carb intake that is associated with this bolus
    var carbIntake: Carb? = null
}

@Serializable
data class Carb(
    override var value: Float,
    @Serializable(with = LocalDateTimeSerializer::class)
    override var time: LocalDateTime,
) : BaseDataClass



enum class DATA_SOURCE{CAMAPS_NOTIF, DIASEND}

@Serializable
data class GlucoseReading(
    override var value: Float,
    @Serializable(with = LocalDateTimeSerializer::class)
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







