package nz.t1d.testModels

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TestSuite(
    // List of readings
    val glucose: List<Reading>,
    val carbs: List<Reading>,
    val basal_rate: List<Reading>,
    val bolus: List<Reading>,

    // List of assumptions
    val insulin_duration: Float,
    val insulin_onset: Float,
    val insulin_peak: Float,

    val tests: Map<String, Test>
)

@Serializable
data class Test(
    // List of readings
    val glucose: List<Reading>? = null,
    val carbs: List<Reading>?= null,
    val basal_rate: List<Reading>?= null,
    val bolus: List<Reading>?= null,

    // List of assumptions
    val insulin_duration: Float?= null,
    val insulin_onset: Float?= null,
    val insulin_peak: Float?= null,

    val now: String,
    val assert: Assertion,
)

@Serializable
data class Assertion(
    val linear_iob: LinearIOBAssertion?,
    val bilinear_iob: BiLinearIOBAssertion?,
)

@Serializable
data class LinearIOBAssertion(
    val basal: Float,
    val bolus: Float,
)

@Serializable
data class BiLinearIOBAssertion( 
    val basal: Float,
    val bolus: Float,
)

@Serializable
data class Reading(
    val time: String,
    val value: Float,
)