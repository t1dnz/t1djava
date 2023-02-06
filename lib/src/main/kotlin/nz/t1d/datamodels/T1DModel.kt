package nz.t1d.datamodels

import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.math.truncate
import java.util.logging.Logger



// This is the group of models used to calculate the various states of t1d, e.g. insulin on board...
class T1DModel private constructor(var iobModel: IOBModel = NullIOBModel() ): IOBModel by iobModel {
    val Log = Logger.getLogger(this.javaClass.name)

    // This Builder pattern makes construction a bit easier
    class Builder {
        var t1d = T1DModel()

        var iobModel: IOBModel = NullIOBModel()
        var iobModelSet: Boolean = false

        fun bilinearIOBModel() = apply {
            t1d.iobModel = BiLinearIOB(t1d)
            iobModelSet = true
        }

        fun build(validate: Boolean = true): T1DModel {
            if (validate) {
                if(!iobModelSet) {
                    throw Exception("IOBModel not set")
                }
            }
            return t1d
        }
    }


    // This is the information provided by the patient or their devices
    // Some of it is a guess, some of it is incorrect, some of the data might be conflicting
    // This is why it is in its own model
    val patientData: PatientData = PatientData()

    fun insulinDuration(): Float {
        return 180f
    }

    fun insulinOnset(): Float {
        return 20f
    } 

    fun insulinPeak(): Float {
        return 60f
    } 

    // input
    fun insulinBoluses(): SortedSet<BolusInsulin> {
        return patientData.insulinBoluses
    }

    fun insulinBasalChanges(): SortedSet<BasalInsulinChange> {
        return patientData.insulinBasalChanges
    }

    fun bglReadings(): SortedSet<BGLReading> {
        return patientData.bglReadings
    }

    fun carbs(): SortedSet<CarbIntake> {
       return patientData.carbs
    }

    fun todaysBGLReadings(): List<BGLReading> {
        return patientData.todaysBGLReadings()
    }


    fun insulinCurrentBasal(): Float {
        if (insulinBasalChanges().size > 0) {
            return insulinBasalChanges().first().value 
        } 
        return 0.0f
    }

    fun recentEvents(): SortedSet<BaseDataClass> {

        var cutofftime = LocalDateTime.now().minusMinutes(180)
        var carbs = carbs()
        var insulinBoluses = insulinBoluses()

        var recentEvents: SortedSet<BaseDataClass> = sortedSetOf(timeOrder)
        

        if (!insulinBoluses.isEmpty()) recentEvents.add(insulinBoluses.first()) // always add last bolus
        if(!carbs.isEmpty()) recentEvents.add(carbs.first()) // always add last Carb

        for (d in insulinBoluses) {
            if (d.time > cutofftime) {
                recentEvents.add(d)
            }
        }

        for (d in carbs) {
            if (d.time > cutofftime) {
                recentEvents.add(d)
            }
        }
        return recentEvents
    }

    //////
    // Stats
    ///////
    
    fun timeInRange(): Float {
        var todayTotalReadings = 0f
        var todayTotalInrangeReadings = 0f
        for (d in todaysBGLReadings()) {
            todayTotalReadings += 1
            if (d.value >= 3.9 && d.value <= 10) {
                todayTotalInrangeReadings += 1
            }
        }

        return todayTotalInrangeReadings / todayTotalReadings
    }

    fun meanBGL(): Float {
        var todayTotalReadings = 0f
        var sumTotalReadings = 0f
        for (d in todaysBGLReadings()) {
            todayTotalReadings += 1
            sumTotalReadings += d.value
        }

        return sumTotalReadings / todayTotalReadings
    }

    fun stdBGL(): Float {
        var todayTotalReadings = 0f
        var cmeanBGL = this.meanBGL()
        var tmpSTD: Double = 0.0;
        for (d in todaysBGLReadings()) {
            todayTotalReadings +=1
            tmpSTD += Math.pow((d.value - cmeanBGL).toDouble(), 2.0)
            
        }
        return Math.sqrt((tmpSTD / todayTotalReadings).toDouble()).toFloat()   
    }


    fun removeOldData() {
        patientData.removeOldData()
    }

    fun addPatientData(patientData: PatientData) {
        this.patientData.merge(patientData)
    }

    fun processData() {
        // Take the basal changes and calculate the equivilant boluses
        patientData.processBasalChanges()
        patientData.processBGLReadings()
        patientData.joinTogetherBolusInfo()
        // TODO process and join bolus and carbs into a single item

        // end of compression
    
    }

    fun addBGlReading(reading: Float, time: LocalDateTime, unit: String, source: DATA_SOURCE) {
        patientData.addBGlReading(reading, time, unit, source)
    }

}