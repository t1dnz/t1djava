package nz.t1d.datamodels
import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

import kotlin.NotImplementedError

// Insulin on board models
interface IOBModel {
    fun basalIOB(t1d: T1DModel): Float
    fun bolusIOB(t1d: T1DModel): Float
}


class BilinearIOB(): IOBModel {
   override fun basalIOB(t1d: T1DModel): Float {
        var iob : Float = 0f
        for (d in this.insulinBasalBoluses(t1d)) {
            val bolusesRemaingInsulin = valueAfterDecay(t1d.insulinOnset(), t1d.insulinPeak(), t1d.insulinDuration(), d)
            iob += bolusesRemaingInsulin
        }

        return iob
        
    }

    override fun bolusIOB(t1d: T1DModel): Float {
        var iob: Float  = 0f
        for (d in t1d.insulinBoluses()) {
            val bolusesRemaingInsulin = valueAfterDecay(t1d.insulinOnset(), t1d.insulinPeak(), t1d.insulinDuration(), d)
            iob += bolusesRemaingInsulin
        }

        return iob
    }

    fun insulinBasalBoluses(t1d: T1DModel): SortedSet<Bolus> {
        
        // Basal insulin is much harder because it is changes in a curve where the integral is the total.
        var insulinBasalBoluses: SortedSet<Bolus> = sortedSetOf(timeOrder)
 
        if(t1d.insulinBasalChanges().isEmpty()) {
            return insulinBasalBoluses
        }
        // Get now every 4 minutes calculate the basal rate, add that value as a bolusChange to insulinBasalBoluses
        var basalTime = LocalDateTime.now()
        var insulinBasalChangesList = ArrayDeque(t1d.insulinBasalChanges().toMutableList())
        var currentBasal = insulinBasalChangesList.first()
        var isEmpty = false

        val midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        val midnightMinus = midnight.minusMinutes(300L)

        while (basalTime > midnightMinus) {
            basalTime = basalTime.minusMinutes(4)
            while (basalTime < currentBasal.time) {
                if (insulinBasalChangesList.isEmpty()) {
                    isEmpty = true
                    break
                } else {
                    currentBasal = insulinBasalChangesList.pop()
                }
            }
            if (isEmpty) {
                break
            }
            // divide by 15 to make the rate per 4 mins from per hour
            insulinBasalBoluses.add(Bolus(currentBasal.value / 15.0f, basalTime))
        }
        return insulinBasalBoluses
    }

    fun valueAfterDecay(onset: Float, peak: Float, dia: Float, datum: BaseDataClass): Float {
        // This is taken from the Bilinear algorithm here https://openaps.readthedocs.io/en/latest/docs/While%20You%20Wait%20For%20Gear/understanding-insulin-on-board-calculations.html
        // actual code here https://github.com/openaps/oref0/blob/88cf032aa74ff25f69464a7d9cd601ee3940c0b3/lib/iob/calculate.js#L36
        // basically insulin rate increases linearly from start to peak, then decreases linearly from peak to dia
        // this makes a triangle which we can calculate the area left which will be the total remaining insulin
        // TODO: maybe use the exponential insulin curves also described on the page above
        // TODO: take onset into consideration by squashing triangle a bit

        val minsAgo = datum.minsAgo().toFloat()
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
        var triangleArea: Float = 0f
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

        val ret = (1 - (percentOfInsulinUsed / 100)) * datum.value
        return ret
    }

    private fun areaOfTriangleFromSlope(slope: Float, width: Float): Float {
        val height = slope * width
        return (height * width) / 2f
    }

}