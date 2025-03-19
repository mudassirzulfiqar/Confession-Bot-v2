package repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import models.ServerConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Service responsible for managing Discord server configurations with the remote server
 */
class RemoteService(private val serverUrl: String, private val apiKey: String) {

    private fun createRequestBody(config: ServerConfig): RequestBody {
        val json = Gson().toJson(config)
        return json.toRequestBody("application/json".toMediaType())
    }

    fun getAllConfiguredServers(callback: (Result<List<ServerConfig>>) -> Unit) {
        val tableName = "discord_server"
        val url = "$serverUrl/rest/v1/$tableName"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("apiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (!responseBody.isNullOrEmpty()) {
                            val jsonArray = Gson().fromJson(responseBody, JsonArray::class.java)
                            val serverList = mutableListOf<ServerConfig>()

                            for (i in 0 until jsonArray.size()) {
                                val entry = jsonArray[i].asJsonObject
                                val config = ServerConfig(
                                    serverId = entry.get("server_id").asString,
                                    channelId = entry.get("channel_id").asString,
                                    createdAt = entry.get("created_at")?.asString
                                )
                                serverList.add(config)
                            }
                            callback(Result.success(serverList))
                        } else {
                            callback(Result.success(emptyList()))
                        }
                    } else {
                        callback(Result.failure(IOException("Failed to fetch servers: ${response.body?.string()}")))
                    }
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    fun saveDiscordChannel(config: ServerConfig, callback: (Result<Unit>) -> Unit) {
        val tableName = "discord_server"
        val url = "$serverUrl/rest/v1/$tableName"
        val body = createRequestBody(config)

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
                    callback(Result.failure(IOException("Failed to save channel: ${response.body?.string()}")))
                }
            }
        })
    }
}