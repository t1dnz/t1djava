package nz.t1d.datamodels

import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.math.truncate

class PatientData {

    var insulinBoluses: SortedSet<BolusInsulin> = sortedSetOf(timeOrder)
    var insulinBasalChanges: SortedSet<BasalInsulinChange> = sortedSetOf(timeOrder)
    var bglReadings: SortedSet<BGLReading> = sortedSetOf(timeOrder)
    var carbs: SortedSet<CarbIntake> = sortedSetOf(timeOrder)

    fun merge(pd: PatientData) {
        // Join all the data together
        insulinBoluses.addAll(pd.insulinBoluses)
        carbs.addAll(pd.carbs)
        insulinBasalChanges.addAll(pd.insulinBasalChanges)
        bglReadings.addAll(pd.bglReadings)
    }

    fun removeOldData() {
        val now = LocalDateTime.now()
        val midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        val midnightMinus = midnight.minusMinutes(300L)
        
        // Since this program can run for days to make sure we are discarding any really old data we remove it from the sets here
        insulinBoluses = insulinBoluses.filter { d -> d.time > midnightMinus }.toSortedSet(timeOrder)
        carbs = carbs.filter { d -> d.time > midnightMinus }.toSortedSet(timeOrder)
        insulinBasalChanges = insulinBasalChanges.filter { d -> d.time > midnightMinus }.toSortedSet(timeOrder)
        bglReadings = bglReadings.filter { d -> d.time > midnightMinus }.toSortedSet(timeOrder)
    }

    fun processBGLReadings() {
        // Because we can get notifications from multiple locations we can have ver similar duplicates
        // we remove all readings if they are within 1 minute of each other
        // keeping the diasend reading as priority because it is more truthy

        val bglList = bglReadings.toList()
        for (i in bglList.indices) {
            val d1 = bglList[i]

            // already been removed, skip
            if (!bglReadings.contains(d1)) {
                continue
            }

            // Find next value that will return a diff
            for (j in (i + 1)..(bglList.size - 1)) {
                val d2 = bglList[j]

                val secondsDuration = Duration.between(d2.time, d1.time).toMillis() / 1000
                // if the difference in time between readings is less than 150 seconds
                if (secondsDuration < 150) {
                    var notTruthy = d1.lessTruthy(d2)
                    bglReadings.remove(notTruthy)
                    continue
                }
            }
        }

        // assign the previous reading to each of the bglreadings so they can self calculate diff and such
        var futureReading: BGLReading? = null
        for (d in bglReadings) {
            if (futureReading != null) {
                futureReading.previousReading = d
            }
            futureReading = d
        }
    }

     fun processBasalChanges() {
        if (insulinBasalChanges.isEmpty()) {
            return
        }
        // Compress Basal Changes (there are a ton of useless ones)
        var previousMinsAgo: Long = -1
        var previousValue: Float = -1f
        val removeElemets = mutableListOf<BasalInsulinChange>()
        for (re in insulinBasalChanges) {
            if (re.minsAgo() == previousMinsAgo || re.value == previousValue) {
                removeElemets.add(re)
                continue
            }
            previousMinsAgo = re.minsAgo()
            previousValue = re.value

        }
        for (re in removeElemets) {
            insulinBasalChanges.remove(re)
        }
        
    }

     fun joinTogetherBolusInfo() {
        // We want to find events in a time range
        // We want to join the boluses with carbs
        for (bolus in insulinBoluses) {
            val closeCarbs = findEvents(bolus.time, bolus.time.plusMinutes(2), carbs)
            if (!closeCarbs.isEmpty()) {
                bolus.carbIntake = closeCarbs.first()
                carbs.remove(bolus.carbIntake)
            }
        }
    }

    fun todaysBGLReadings(): List<BGLReading> {
        val midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        return findEvents(midnight, LocalDateTime.now().plusMinutes(2), bglReadings)
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
        val reading = BGLReading(reading, time, unit)
        reading.source = source
        bglReadings.add(reading)
        processBGLReadings()
    }
}