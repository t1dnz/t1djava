package nz.t1d.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.required
import nz.t1d.clients.diasend.*
import java.time.LocalDateTime 
import kotlinx.coroutines.*

// run with gradle run --args=""
// diasend patient-data --date-from=<date> --date-to=<date>

// gradle run --args="diasend patient-data -u user -p password"
fun main(args: Array<String>) {
    class T1DCLI: CliktCommand() {
        override fun run() = Unit
    }

    T1DCLI().subcommands(diasendCommand()).main(args)
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
                dc.getPatientData(date_from=dateFrom, date_to = dateTo)
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