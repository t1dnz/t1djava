package nz.t1d.testmodels

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

import nz.t1d.datamodels.Data
import nz.t1d.datamodels.Profile
import nz.t1d.datamodels.LocalDateTimeSerializer
import java.time.LocalDateTime


import nz.t1d.datamodels.T1DModel

@Serializable
data class TestSuite(
    // List of readings
    val data: Data,
    val profile: Profile,


    val tests: Map<String, Test>
) {
    fun runTest(fileName: String) {
        for ((testName, value) in tests.entries) {
            value.runTest(this, fileName, testName)
        }
    }
}

@Serializable
data class Test(
    // List of additions/overrides
    val data: Data? = null,
    val profile: Profile? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    val now: LocalDateTime,

    val assert: Assertion,
) {
    fun runTest(testSuite: TestSuite, fileName: String, testName: String) {
        var profile = testSuite.profile.copy()

        if(this.profile != null) {
            profile = this.profile
        }

        val t1d = T1DModel.Builder().build(profile)
        t1d.addData(testSuite.data)
        t1d.addData(this.data)
        assert.assert(t1d, fileName, testName, now)
    }
}


@Serializable
data class Assertion(
    val basalIOB: Float? = null,
    val bolusIOB: Float? = null,
) {
    var fileName = ""
    var testName = ""

    fun assert(t1d: T1DModel, fileName: String, testName: String, now: LocalDateTime) {
        this.fileName = fileName
        this.testName = testName

        assertEquals(bolusIOB, t1d.bolusIOB(now), "bolusIOB") 
        assertEquals(basalIOB, t1d.basalIOB(now), "basalIOB") 
    }

    fun <T> assertEquals(assertionValue : T?, modelValue: T , name: String) {
        if(assertionValue == null) {
            return
        }
        if (assertionValue != modelValue) {
            throw Exception("$fileName($testName) failed: $name is incorrect, was $modelValue wanted $assertionValue")
        }
    }
}
