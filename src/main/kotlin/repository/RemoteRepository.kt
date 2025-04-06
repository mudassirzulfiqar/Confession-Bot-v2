package repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import models.ConfessionLog
import models.ServerConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Repository responsible for handling all remote API interactions.
 */
class RemoteRepository(private val serverUrl: String, private val apiKey: String) {

    private val client = OkHttpClient()

    private fun createRequestBody(payload: Any): RequestBody {
        val json = Gson().toJson(payload)
        return json.toRequestBody("application/json".toMediaType())
    }

    fun saveLog(log: ConfessionLog, callback: (Result<Unit>) -> Unit) {
        val url = "$serverUrl/rest/v1/logs"
        val body = createRequestBody(log)

        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(Result.success(Unit))
                } else {
                    callback(Result.failure(IOException("Failed to save log: ${response.body?.string()}")))
                }
            }
        })
    }

    fun saveServerConfig(config: ServerConfig, callback: (Result<Unit>) -> Unit) {
        val url = "$serverUrl/rest/v1/discord_server"
        val body = createRequestBody(config)

        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(Result.success(Unit))
                } else {
                    callback(Result.failure(IOException("Failed to save server config: ${response.body?.string()}")))
                }
            }
        })
    }

    fun getAllServerConfigs(callback: (Result<List<ServerConfig>>) -> Unit) {
        val url = "$serverUrl/rest/v1/discord_server"

        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        try {
                            val jsonArray = Gson().fromJson(responseBody, JsonArray::class.java)
                            val serverList = jsonArray.map { element ->
                                val obj = element.asJsonObject
                                ServerConfig(
                                    serverId = obj.get("server_id").asString,
                                    channelId = obj.get("channel_id").asString,
                                    createdAt = obj.get("created_at")?.asString
                                )
                            }
                            callback(Result.success(serverList))
                        } catch (e: Exception) {
                            callback(Result.failure(e))
                        }
                    } else {
                        callback(Result.success(emptyList()))
                    }
                } else {
                    callback(Result.failure(IOException("Failed to fetch server configs: ${response.body?.string()}")))
                }
            }
        })
    }
}