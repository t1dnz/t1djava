package nz.t1d.clients.diasend

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Logger
import nz.t1d.datamodels.*
import java.time.ZoneId
import java.util.concurrent.TimeUnit
 
/*
mostly documented here https://github.com/PatrikTrestik/diasend-upload/blob/master/doc/diasend-api/DiasendAPI-1.0.0.yaml

curl -L -u 'a486o3nvdu88cg0sos4cw8cccc0o0cg.api.diasend.com:8imoieg4pyos04s44okoooowkogsco4' --request POST 'https://api.diasend.com/1/oauth2/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'username=<username>' \
--data-urlencode 'password=<password>' \
--data-urlencode 'scope=PATIENT DIASEND_MOBILE_DEVICE_DATA_RW'

Returns

{"access_token":"<token>","expires_in":"86400","token_type":"Bearer"}

curl -vv -L --request GET 'https://api.diasend.com/1/patieclientsnt/data?type=combined&date_from=<from>&date_to=<to>' --data-urlencode 'type=combined' -H "Authorization: Bearer <token>"

[
  {
    "type": "insulin_basal",
    "created_at": "2022-08-10T12:42:13",
    "value": 1,
    "unit": "U/h",
    "flags": []
  },
  {
    "type": "glucose",
    "created_at": "2022-08-10T12:42:17",
    "value": 7,
    "unit": "mmol/l",
    "flags": [
      {
        "flag": 123,
        "description": "Continous reading"
      }
    ]
  },
   {
    "type": "insulin_bolus",
    "created_at": "2022-07-30T13:20:00",
    "unit": "U",
    "total_value": 0.5,
    "spike_value": 0.5,
    "suggested": 0.5,
    "suggestion_overridden": "no",
    "suggestion_based_on_bg": "no",
    "suggestion_based_on_carb": "yes",
    "programmed_meal": 0.5,
    "flags": [
      {
        "flag": 1035,
        "description": "Bolus type ezcarb"
      }
    ]
  },
  {
    "type": "carb",
    "created_at": "2022-07-30T13:21:01",
    "value": "10",
    "unit": "g",
    "flags": []
  },
]
*/


@Serializable
data class AuthToken(
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("access_token")
    val accessToken: String,
)

@Serializable
data class DiasendDatumFlag(
    @SerializedName("flag")
    val flag: Int,
    @SerializedName("description")
    val description: String,
)

@Serializable
data class DiasendDatum(
    @SerializedName("type")
    val type: String,
    @SerializedName("created_at")
    @Contextual
    val createdAt: Date,
    @SerializedName("value")
    val value: Float,
    @SerializedName("total_value")
    val totalValue: Float,
    @SerializedName("unit")
    val unit: String,
    @SerializedName("flags")
    val flags: List<DiasendDatumFlag>,
    @SerializedName("suggestion_based_on_carb")
    val suggestionBasedOnCarb: String
)

interface Diasend {
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("scope") scope: String,
    ): Response<AuthToken>

    @GET("patient/data")
    suspend fun patientData(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("date_from") date_from: String,
        @Query("date_to") date_to: String,
    ): Response<List<DiasendDatum>>
}

class DiasendClient(diasend_username: String, diasend_password: String) {

    private val diasend_username = diasend_username
    private val diasend_password = diasend_password

    val Log = Logger.getLogger(this.javaClass.name)

    private val BASE_URL = "https://api.diasend.com/1/"
    private val TAG = "DiasendClient"
    val APP_USER_NAME_PASS =
        "a486o3nvdu88cg0sos4cw8cccc0o0cg.api.diasend.com:8imoieg4pyos04s44okoooowkogsco4"

    private var accessToken: String = ""
    private var accessTokenExpiry: LocalDateTime = LocalDateTime.now().minusDays(10) // force reauth at first

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
            .baseUrl(BASE_URL)
            .build()
    }

    private val diasendClient: Diasend by lazy {
        retrofit.create(Diasend::class.java)

    }

    // API code
    private suspend fun getAccessToken(): String {
        if (accessTokenExpiry > LocalDateTime.now()) {
            // If the access token has expired or on first init
            Log.info("Returning existing Auth token")
            return this.accessToken
        }
        Log.info("Authenticating Diasend Client")

        val authPayload = APP_USER_NAME_PASS
        val data = authPayload.toByteArray()
        val base64 = Base64.getEncoder().encodeToString(data)

        val response = diasendClient.getToken(
            authorization = "Basic $base64".trim(),
            grantType = "password",
            username = diasend_username,
            password = diasend_password,
            scope = "PATIENT DIASEND_MOBILE_DEVICE_DATA_RW"
        )

        val body = response.body() ?: throw Exception("No Access Token")

        accessToken = body.accessToken
        accessTokenExpiry = LocalDateTime.now().plusSeconds(body.expiresIn.toLong()-300) // Fetch the auth token minus 5 mins for safety
        return accessToken
    }

    fun closeConnections(): Unit {
      // Stupid workaround to the issue of main hanging
      // https://github.com/square/retrofit/issues/3144
      okclient.dispatcher.executorService.shutdown()
      okclient.connectionPool.evictAll()
    }

    suspend fun getPatientData(date_from: LocalDateTime, date_to: LocalDateTime): PatientData {
        val fmtr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val date_from_str = date_from.format(fmtr)
        val date_to_str = date_to.format(fmtr)
        val diasendPatientData = diasendClient.patientData(
            "Bearer ${getAccessToken()}",
            "combined",
            date_from = date_from_str,
            date_to = date_to_str,
            ).body()

        val dc = PatientData()
        if (diasendPatientData == null) {
          return dc
        }

        for (d in diasendPatientData) {
            var ld = d.createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            when (d.type) {
                "insulin_bolus" -> {
                    dc.insulinBoluses.add(BolusInsulin(d.totalValue, ld))
                }
                "carb" -> {
                    dc.carbs.add(CarbIntake(d.value, ld))
                }
                "insulin_basal" -> {
                    dc.insulinBasalChanges.add(BasalInsulinChange(d.value, ld))
                }
                "glucose" -> {
                    dc.bglReadings.add(BGLReading(d.value, ld))
                }
                else -> {
                    Log.info("UNKNOWN DIASEND TYPE ${d.type}")
                }
            }
        }

        return dc
    }
}