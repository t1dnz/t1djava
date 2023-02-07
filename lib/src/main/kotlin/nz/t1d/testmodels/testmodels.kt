package nz.t1d.testmodels

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

import nz.t1d.datamodels.Data
import nz.t1d.datamodels.Profile


@Serializable
data class TestSuite(
    // List of readings
    val data: Data,
    val profile: Profile,


    val tests: Map<String, Test>
)

@Serializable
data class Test(
    // List of additions/overrides
    val data: Data? = null,
    val profile: Profile? = null,

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