package repository

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException

class RemoteService(private val SERVER_URL: String, val API_KEY: String) {

    private fun createRequestBody(serverId: String, channelId: String): RequestBody {
        val payload = mapOf("server_id" to serverId, "channel_id" to channelId)
        val json = Gson().toJson(payload)
        return json.toRequestBody("application/json".toMediaType())
    }

    fun saveDiscordChannel(
        serverId: String,
        channelId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        // Define the Supabase table
        val tableName = "discord_channels"
        val url = "$SERVER_URL/rest/v1/$tableName"

        val body = createRequestBody(serverId, channelId)
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