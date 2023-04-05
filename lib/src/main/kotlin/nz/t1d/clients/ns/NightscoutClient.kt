package nz.t1d.clients.ns

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.*
import java.time.format.*
import java.util.*
import java.util.logging.Logger
import nz.t1d.datamodels.*
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
 
/*
curl -X GET "https://ns_url/api/v1/treatments.json?count=10"


curl -X GET "https://ns_url/api/v1/status.json"

curl -X GET "https://ns_url/api/v1/entries.json?count=10"

curl -X GET "https://ns_url/api/v1/profile"

curl -X GET "https://ns_url/api/v2/properties/iob"

curl -X GET "https://ns_url/api/v2/properties/cob"

curl -X GET "https://ns_url/api/v2/properties/basal"

curl -X GET 'https://ns_url/api/v1/entries.json?find\[dateString\]\[$gte\]=2023-04-04T22:00:00Z&find\[dateString\]\[$lte\]=2023-04-04T22:10:00Z'
dateString

curl -X GET 'https://ns_url/api/v1/treatments.json?find\[created_at\]\[$gte\]=2023-04-04T22:00:00Z&find\[created_at\]\[$lte\]=2023-04-04T22:30:00Z'
*/


@Serializable
data class Entry(
    @SerializedName("_id")
    val id: String,
    @SerializedName("dateString")
    val date: String,
    @SerializedName("sgv")
    val sgv: Float?,
    @SerializedName("delta")
    val delta: Float?,
    @SerializedName("direction")
    val direction: String?,
) { 
    fun getDate(): LocalDateTime {
        var utc = Instant.parse(this.date).atZone(ZoneOffset.UTC)
        var ldt = utc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        return ldt
    }
}

@Serializable
data class Treatment(
    @SerializedName("_id")
    val id: String,
    @SerializedName("eventType")
    val eventType: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("rate")
    val rate: Float?,
    @SerializedName("carbs")
    val carbs: Float?,
    @SerializedName("insulin")
    val insulin: Float?,
) { 
    fun getDate(): LocalDateTime {
        var utc = Instant.parse(this.createdAt).atZone(ZoneOffset.UTC)
        var ldt = utc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        return ldt
    }
}

@Serializable
data class CurrentBasalObject(val totalbasal: Float)
@Serializable
data class BasalObject(val current: CurrentBasalObject)
@Serializable
data class Basal(val basal: BasalObject)

@Serializable
data class IOBObject(val iob: Float, val basaliob: Float)
@Serializable
data class IOB(val iob: IOBObject)
////
// API
////

interface Nightscout {
    
    @GET("api/v1/entries.json")
    suspend fun entries(
        @Query("find[dateString][\$gte]") date_from: String,
        @Query("find[dateString][\$lte]") date_to: String,
        @Query("count") count: Int
    ): Response<List<Entry>>

    @GET("api/v1/treatments.json")
    suspend fun treatments(
        @Query("find[created_at][\$gte]") date_from: String,
        @Query("find[created_at][\$lte]") date_to: String,
        @Query("count") count: Int
    ): Response<List<Treatment>>

    @GET("api/v2/properties/iob.json")
    suspend fun iob(): Response<IOB>

    @GET("api/v2/properties/basal.json")
    suspend fun basal(): Response<Basal>
}

class NightscoutClient(url: String) {
    private val url = url
    val Log = Logger.getLogger(this.javaClass.name)
    val fmtr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val TAG = "DiasendClient"

    private val okclient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    // Client code
    private val retrofit: Retrofit by lazy {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()

        Retrofit.Builder()
            .client(okclient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(url)
            .build()
    }

    private val nightscoutClient: Nightscout by lazy {
        retrofit.create(Nightscout::class.java)
    }

    fun closeConnections(): Unit {
      // Stupid workaround to the issue of main hanging
      // https://github.com/square/retrofit/issues/3144
      okclient.dispatcher.executorService.shutdown()
      okclient.connectionPool.evictAll()
    }

    suspend fun getIOB(): IOBTotals {
        var iob = nightscoutClient.iob().body()
        if (iob == null) {
          return IOBTotals(-1f,-1f,-1f)
        }

        val iobv = iob.iob.iob
        val iobBasal = iob.iob.basaliob
        val iobBolus = iobv - iobBasal
        return IOBTotals(iobv, iobBolus, iobBasal)
    }

    suspend fun getCurrentBasal(): Float {
        var basal = nightscoutClient.basal().body()
        if (basal == null) {
          return -1f
        }
        return basal.basal.current.totalbasal
    }

    suspend fun getTreatments(date_from: LocalDateTime, date_to: LocalDateTime): Data {       
        val nsTreatments = nightscoutClient.treatments(
            date_from = date_from.format(fmtr),
            date_to = date_to.format(fmtr),
            count=100
        ).body()

        val dc = Data()
        if (nsTreatments == null) {
          return dc
        }

        for (d in nsTreatments) {
            if (d.rate != null) {
                dc.basal_changes.add(BasalChange(d.rate, d.getDate()))
            } else if (d.carbs != null && d.insulin != null) {
                dc.carbs.add(Carb(d.carbs, d.getDate()))
                dc.boluses.add(Bolus(d.insulin, d.getDate()))
            } else if (d.carbs != null) {
                dc.carbs.add(Carb(d.carbs, d.getDate()))
            } else if (d.insulin != null) {
                dc.boluses.add(Bolus(d.insulin, d.getDate()))
            }
            else {
                Log.info("Different TREATMENT ${d}")
            }
        }

        return dc
    }

    suspend fun getEntries(date_from: LocalDateTime, date_to: LocalDateTime): Data {
        val nsEntires = nightscoutClient.entries(
            date_from = date_from.format(fmtr),
            date_to = date_to.format(fmtr),
            count=400
        ).body()

        val dc = Data()
        if (nsEntires == null) {
          return dc
        }

        for (d in nsEntires) {
            if (d.date == null || d.sgv == null) {
                Log.info("BAD SGV ${d}")
                continue
            }
            dc.glucose_readings.add(GlucoseReading(d.sgv/18, d.getDate()))
        }

        return dc
    }
}