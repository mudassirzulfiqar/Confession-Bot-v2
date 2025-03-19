package repository

import com.google.gson.Gson
import models.ConfessionLog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Service responsible for handling logging operations with the remote server
 */
class LogService(private val serverUrl: String, private val apiKey: String) {

    private fun createRequestBody(log: ConfessionLog): RequestBody {
        val json = Gson().toJson(log)
        return json.toRequestBody("application/json".toMediaType())
    }

    fun recordLog(log: ConfessionLog, callback: (Result<Unit>) -> Unit) {
        val tableName = "logs"
        val url = "$serverUrl/rest/v1/$tableName"
        val body = createRequestBody(log)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(Result.success(Unit))
                } else {
                    callback(Result.failure(IOException("Failed to record log: ${response.body?.string()}")))
                }
            }
        })
    }
}