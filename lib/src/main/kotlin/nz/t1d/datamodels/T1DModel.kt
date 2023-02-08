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
class T1DModel private constructor(val iobModel: IOBModel, val doaModel: DOAModel) {
    val Log = Logger.getLogger(this.javaClass.name)

    // This is the information provided by the patient or their devices
    // Some of it is a guess, some of it is incorrect, some of the data might be conflicting
    // This is why it is in its own model
    val patientData: Data = Data()

    // This Builder pattern makes construction a bit easier
    class Builder {
        fun build(profile: Profile = Profile()): T1DModel {
            var iobModel: IOBModel = BilinearIOB() // Default
            when(profile.iob_model) {
                IOB_MODEL.BILINEAR -> iobModel = BilinearIOB()
            }

            var doaModel = LinearDOAModel(
                insulinDuration = profile.insulin_duration.toFloat(), 
                insulinOnset = profile.insulin_onset.toFloat(), 
                insulinPeak= profile.insulin_peak.toFloat()
            )
            var t1d = T1DModel(iobModel=iobModel, doaModel=doaModel)
            return t1d
        }
    }

    ////
    // Insluin on Board Model
    ////

    fun basalIOB(now:LocalDateTime = LocalDateTime.now()): Float {
        return iobModel.basalIOB(this, now)
    }

    fun bolusIOB(now:LocalDateTime = LocalDateTime.now()): Float {
        return iobModel.bolusIOB(this, now)
    }

    fun IOB(now:LocalDateTime = LocalDateTime.now()): Float {
        return bolusIOB(now) + basalIOB(now)
    }

    ////
    // Insulin Duration of Action Model
    ////

    fun insulinDuration(now:LocalDateTime = LocalDateTime.now()): Float {
        return doaModel.insulinDuration(this,now)
    }

    fun insulinOnset(now:LocalDateTime = LocalDateTime.now()): Float {
        return doaModel.insulinOnset(this,now)
    } 

    fun insulinPeak(now:LocalDateTime = LocalDateTime.now()): Float {
        return doaModel.insulinPeak(this,now)
    } 

    ////
    // Access to patiend data methods
    ////

    fun insulinBoluses(): SortedSet<Bolus> {
        return patientData.boluses
    }

    fun insulinBasalChanges(): SortedSet<BasalChange> {
        return patientData.basal_changes
    }

    fun bglReadings(): SortedSet<GlucoseReading> {
        return patientData.glucose_readings
    }

    fun carbs(): SortedSet<Carb> {
       return patientData.carbs
    }

    fun todaysBGLReadings(): List<GlucoseReading> {
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
        val todaysReadings = todaysBGLReadings()
        if (todaysReadings.isEmpty()) {
            return 0f
        }

        var todayTotalReadings = 0f
        var todayTotalInrangeReadings = 0f
        for (d in todaysReadings) {
            todayTotalReadings += 1
            if (d.value >= 3.9 && d.value <= 10) {
                todayTotalInrangeReadings += 1
            }
        }

        return todayTotalInrangeReadings / todayTotalReadings
    }

    fun meanBGL(): Float {
        val todaysReadings = todaysBGLReadings()
        if (todaysReadings.isEmpty()) {
            return 0f
        }

        var todayTotalReadings = 0f
        var sumTotalReadings = 0f
        for (d in todaysReadings) {
            todayTotalReadings += 1
            sumTotalReadings += d.value
        }

        return sumTotalReadings / todayTotalReadings
    }

    fun stdBGL(): Float {
        val todaysReadings = todaysBGLReadings()
        if (todaysReadings.isEmpty()) {
            return 0f
        }

        var todayTotalReadings = 0f
        var cmeanBGL = this.meanBGL()
        var tmpSTD: Double = 0.0;
        for (d in todaysReadings) {
            todayTotalReadings +=1
            tmpSTD += Math.pow((d.value - cmeanBGL).toDouble(), 2.0)
            
        }
        return Math.sqrt((tmpSTD / todayTotalReadings).toDouble()).toFloat()   
    }


    ////
    // Changing and remove data
    ////

    fun removeOldData() {
        patientData.removeOldData()
    }

    fun addData(data: Data?) {
        if (data == null) {
            return
        }
        this.patientData.merge(data)
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