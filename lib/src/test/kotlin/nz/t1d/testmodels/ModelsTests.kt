package nz.t1d.testmodels

import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.LocalDateTime
import kotlin.test.assertEquals

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

import com.charleskorn.kaml.Yaml
import java.io.File

import nz.t1d.testmodels.TestSuite
import nz.t1d.datamodels.Data

class ModelsTest {
    var listOfFiles = listOf(
        "../../t1d.tests/testdata/simple.yml"
    )

    @Test fun YAMLTest() {
        for (filePath in listOfFiles) {
            val result = Yaml.default.decodeFromString(TestSuite.serializer(), File(filePath).readText())
            result.runTest()
        }    
    }

}


