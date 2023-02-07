package nz.t1d.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.required
import nz.t1d.clients.diasend.*
import java.time.LocalDateTime 
import kotlinx.coroutines.*
import com.charleskorn.kaml.Yaml
import java.io.File

import nz.t1d.testmodels.TestSuite
import nz.t1d.datamodels.Data
// run with gradle run --args=""
// diasend patient-data --date-from=<date> --date-to=<date>

// gradle run --args="diasend patient-data -u user -p password"
fun main(args: Array<String>) {
    class T1DCLI: CliktCommand() {
        override fun run() = Unit
    }

    T1DCLI().subcommands(
        testCommand(),
        diasendCommand()
    ).main(args)
}

fun testCommand(): CliktCommand {
    class TestCommand: CliktCommand(help = "test against a test yml file") {
        val file by option( "-f", "--file", help = "test yml file to run").file(mustExist = true, canBeDir = false).required()
        override fun run() {            
            val filetext = file.readText()
            val result = Yaml.default.decodeFromString(TestSuite.serializer(), filetext)
            result.runTest()
        }
    }
    return TestCommand()
}

fun diasendCommand(): CliktCommand {
    class Diasend: CliktCommand(help = "diasend client interface") {
        override fun run() = Unit
    }

    //
    // date -v-2d -u +"%Y-%m-%dT%H:%M:%SZ" Date minus 2 days
    //
    class PatientData: CliktCommand(help = "fetch patient data") {
        val dateFromStr by option( "-f", "--date-from", help = "date-from").default(LocalDateTime.now().minusHours(1).toString())
        val dateToStr by option(  "-t", "--date-to", help = "date-to").default(LocalDateTime.now().toString())

        val username by option( "-u", "--username", help = "username").required()
        val password by option( "-p", "--password", help = "password").required()

        override fun run() = runBlocking {
            val dateFrom = LocalDateTime.parse(dateFromStr)
            val dateTo = LocalDateTime.parse(dateToStr)

            val dc = DiasendClient(diasend_username=username, diasend_password=password)
            
            launch {
                try {
                    dc.getPatientData(date_from=dateFrom, date_to = dateTo)
                } catch (ex: Exception) {
                    println(ex)
                }
                dc.closeConnections()
            }
            
            println()
            // TODO output to a standard file format
        }
    }

    val diasend = Diasend()
    val patientData = PatientData()
    
    diasend.subcommands(patientData)

    return diasend
}