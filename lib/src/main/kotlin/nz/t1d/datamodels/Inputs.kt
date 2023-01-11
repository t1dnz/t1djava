package nz.t1d.datamodels

import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.math.truncate

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

data class BasalInsulinChange(
    override var value: Float,
    override var time: LocalDateTime,
) : BaseDataClass

data class BolusInsulin(
    override var value: Float,
    override var time: LocalDateTime,
) : BaseDataClass {
    private val TAG = "BolusInsulin"

    // The carb intake that is associated with this bolus
    var carbIntake: CarbIntake? = null

    fun valueAfterDecay(onset: Float, peak: Float, dia: Float): Float {
        // This is taken from the Bilinear algorithm here https://openaps.readthedocs.io/en/latest/docs/While%20You%20Wait%20For%20Gear/understanding-insulin-on-board-calculations.html
        // actual code here https://github.com/openaps/oref0/blob/88cf032aa74ff25f69464a7d9cd601ee3940c0b3/lib/iob/calculate.js#L36
        // basically insulin rate increases linearly from start to peak, then decreases linearly from peak to dia
        // this makes a triangle which we can calculate the area left which will be the total remaining insulin
        // TODO: maybe use the exponential insulin curves also described on the page above
        // TODO: take onset into consideration by squashing triangle a bit

        val minsAgo = minsAgo().toFloat()
        if (minsAgo >= dia) {
            return 0f
        }

        // percent of insulin used is 100 so we make a triangle whose area is 100
        // 1/2 w * h  = 100, w = dia, solve for h. so h = (100*2)/dia
        val height = 200f / dia // for 180 it is about 1.11

        // Now we can divide that into two right angle triangels, first the one going to peak the second coming down
        // Because it is linear up and linear down all we need to know is the interior angles of the two triangles to find the area
        val areaOfFirstTrialngle = (peak * height) / 2 // 1/2 * w * h
        val areaOfSecondTrialngle = ((dia - peak) * height) / 2 // 1/2 * w * h

        val slopeFirstTriangle = height / peak
        val slopeSecondTriangle = height / (dia - peak)


        var percentOfInsulinUsed = 0f
        var pastPeak = false
        var triangleArea = 0f
        if (minsAgo < peak) {
            pastPeak = false
            triangleArea = areaOfTriangleFromSlope(slopeFirstTriangle, minsAgo)
            percentOfInsulinUsed = triangleArea

        } else {
            pastPeak = true

            // The second triangle is at the back, so need to minus it from the total to find actual area
            triangleArea = areaOfTriangleFromSlope(slopeSecondTriangle, dia - minsAgo)

            percentOfInsulinUsed = areaOfFirstTrialngle + (areaOfSecondTrialngle - triangleArea)
        }

        val ret = (1 - (percentOfInsulinUsed / 100)) * value
        return ret
    }

    private fun areaOfTriangleFromSlope(slope: Float, width: Float): Float {
        val height = slope * width
        return (height * width) / 2f
    }
}

data class CarbIntake(
    override var value: Float,
    override var time: LocalDateTime,
) : BaseDataClass


enum class DATA_SOURCE{CAMAPS_NOTIF, DIASEND}

data class BGLReading(
    override var value: Float,
    override var time: LocalDateTime,
    var bglUnit: String = "mmol/L",
) : BaseDataClass {
    private val TAG = "BGLReading"
    var source: DATA_SOURCE = DATA_SOURCE.DIASEND

    // reference to previous reading making this kind of a
    var previousReading: BGLReading? = null

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
    fun lessTruthy(that: BGLReading) : BGLReading {
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







