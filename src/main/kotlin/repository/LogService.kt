package repository

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class LogService(private val SERVER_URL: String, private val API_KEY: String) {

    private fun createRequestBody(logMessage: String, logLevel: String): RequestBody {
        val payload = mapOf(
            "message" to logMessage,
            "level" to logLevel)
        val json = Gson().toJson(payload)
        return json.toRequestBody("application/json".toMediaType())
    }

    fun recordLog(logMessage: String, logLevel: String, callback: (Boolean, String?) -> Unit) {
        // Define the Supabase table
        val tableName = "logs"
        val url = "$SERVER_URL/rest/v1/$tableName"

        val body = createRequestBody(logMessage, logLevel)

        // Create the OkHttp client
        val client = OkHttpClient()

        // Build the request
        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, response.body?.string())
                }
            }
        })
    }
}