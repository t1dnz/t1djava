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
        
        var bolusIOB = model.bolusIOB()
        if (assert.bolusIOB != null && assert.bolusIOB != bolusIOB) {
            throw Exception("bolusIOB is bad, was $bolusIOB wanted ${assert.bolusIOB}")
        }
    }
}


@Serializable
data class Assertion(
    val basalIOB: Float? = null,
    val bolusIOB: Float? = null,
)
