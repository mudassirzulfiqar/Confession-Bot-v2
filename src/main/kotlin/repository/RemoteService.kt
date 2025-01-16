package repository

import com.google.gson.Gson
import com.google.gson.JsonArray
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


    fun getLatestConfiguredServerId(callback: (String?, String?) -> Unit) {
        // Define the Supabase table
        val tableName = "discord_channels"
        val url = "$SERVER_URL/rest/v1/$tableName"

        // Add query parameters to sort by the timestamp and fetch the latest entry
        val queryUrl = "$url?order=created_at.desc&limit=1"

        // Create the OkHttp client
        val client = OkHttpClient()

        // Build the GET request
        val request = Request.Builder()
            .url(queryUrl)
            .addHeader("apiKey", API_KEY)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null, e.message) // Return error message to the callback
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        try {
                            // Parse the response using Gson
                            val jsonArray = Gson().fromJson(responseBody, JsonArray::class.java)
                            if (jsonArray.size() > 0) {
                                val latestEntry = jsonArray[0].asJsonObject
                                val serverId =
                                    latestEntry.get("server_id").asString // Adjust key based on your table schema
                                callback(serverId, null)
                            } else {
                                callback(null, "No configured server IDs found.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            callback(null, "Error parsing response.")
                        }
                    } else {
                        callback(null, "Empty response from server.")
                    }
                } else {
                    callback(null, response.body?.string()) // Return the error response
                }
            }
        })
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