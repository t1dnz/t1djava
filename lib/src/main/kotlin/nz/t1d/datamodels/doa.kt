
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
interface DOAModel {
    fun insulinDuration(t1d: T1DModel, now: LocalDateTime): Float
    fun insulinOnset(t1d: T1DModel, now: LocalDateTime): Float
    fun insulinPeak(t1d: T1DModel, now: LocalDateTime): Float
}

class LinearDOAModel(val insulinDuration: Float, val insulinOnset: Float, val insulinPeak: Float): DOAModel {
    override fun insulinDuration(t1d: T1DModel, now: LocalDateTime): Float {
        return insulinDuration
    }

    override fun insulinOnset(t1d: T1DModel, now: LocalDateTime): Float {
        return insulinOnset
    }

    override fun insulinPeak(t1d: T1DModel, now: LocalDateTime): Float {
        return insulinPeak
    }
}