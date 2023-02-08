package nz.t1d.testmodels

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
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
            var file = File(filePath)
            val result = Yaml.default.decodeFromString(TestSuite.serializer(), file.readText())
            try {
                result.runTest(file.getName())
            }
            catch (e: Exception) {
                fail(e.message)
            }
        }    
    }

}


