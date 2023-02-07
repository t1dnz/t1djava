package nz.t1d.testmodels

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

import nz.t1d.datamodels.Data
import nz.t1d.datamodels.Profile

import nz.t1d.datamodels.T1DModel

@Serializable
data class TestSuite(
    // List of readings
    val data: Data,
    val profile: Profile,


    val tests: Map<String, Test>
) {
    fun runTest() {
        for ((key, value) in tests.entries) {
            println(key)
            value.runTest(this)
        }
    }
}

@Serializable
data class Test(
    // List of additions/overrides
    val data: Data? = null,
    val profile: Profile? = null,

    val now: String,
    val assert: Assertion,
) {
    fun runTest(testSuite: TestSuite) {
        val model = T1DModel.Builder().build()
        model.addData(testSuite.data)
        if(this.data != null) {
            model.addData(this.data)
        }
        
        var estimateBolusIOB = model.estimateBolusIOB()
        if (assert.bilinear_iob != null && assert.bilinear_iob.bolus != estimateBolusIOB) {
            throw Exception("estimateBolusIOB is bad, was $estimateBolusIOB wanted ${assert.bilinear_iob.bolus}")
        }
    }
}


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