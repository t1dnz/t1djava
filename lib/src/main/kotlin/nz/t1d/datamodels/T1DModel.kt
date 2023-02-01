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
class T1DModel(iobModel: IOBModel): IOBModel by iobModel {
    val Log = Logger.getLogger(this.javaClass.name)

    // This is the information provided by the patient or their devices
    // Some of it is a guess, some of it is incorrect, some of the data might be conflicting
    // This is why it is in its own model
    val patientData: PatientData = PatientData()
    
    // input
    var insulinBoluses: SortedSet<BolusInsulin> = sortedSetOf(timeOrder)
        get() = patientData.insulinBoluses
    var insulinBasalChanges: SortedSet<BasalInsulinChange> = sortedSetOf(timeOrder)
        get() = patientData.insulinBasalChanges
    var bglReadings: SortedSet<BGLReading> = sortedSetOf(timeOrder)
        get() = patientData.bglReadings
    var carbs: SortedSet<CarbIntake> = sortedSetOf(timeOrder)
        get() = patientData.carbs

    var insulinDuration = 180f
    var insulinOnset = 20f
    var insulinPeak = 60f

    var insulinCurrentBasal: Float = 0f
        get() { 
            if (insulinBasalChanges.size > 0) {
                return insulinBasalChanges.first().value 
            } 
            return 0.0f
        }

    var recentEvents: SortedSet<BaseDataClass> = sortedSetOf(timeOrder)
        get() {
            var recentEvents: SortedSet<BaseDataClass> = sortedSetOf(timeOrder)
            
            var cutofftime = LocalDateTime.now().minusMinutes(180)
        
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
    
    var timeInRange :Float = 0f
        get() {
            var todayTotalReadings = 0f
            var todayTotalInrangeReadings = 0f
            for (d in patientData.todaysBGLReadings()) {
                todayTotalReadings += 1
                if (d.value >= 3.9 && d.value <= 10) {
                    todayTotalInrangeReadings += 1
                }
            }

            return todayTotalInrangeReadings / todayTotalReadings
        }

    var meanBGL: Float = 0f
        get() {
            var todayTotalReadings = 0f
            var sumTotalReadings = 0f
            for (d in patientData.todaysBGLReadings()) {
                todayTotalReadings += 1
                sumTotalReadings += d.value
            }

            return sumTotalReadings / todayTotalReadings
        }

    var stdBGL: Float = 0f
        get() {
            var todayTotalReadings = 0f
            var cmeanBGL = this.meanBGL
            var tmpSTD: Double = 0.0;
            for (d in patientData.todaysBGLReadings()) {
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