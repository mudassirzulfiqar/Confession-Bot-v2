import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import repository.LogService
import repository.RemoteService

fun main() {
    val dotenv = Dotenv.load()
    val botToken = dotenv["BOT_TOKEN"] ?: "default_value"
    val databaseServerUrl = dotenv["DATABASE_SERVER_URL"] ?: "default_value"
    val apiKey = dotenv["API_KEY"] ?: "default_value"

    val service = RemoteService(databaseServerUrl, apiKey)
    val logService = LogService(databaseServerUrl, apiKey)
    val serverCommandHandler = ServerCommandHandler(logService, service)

    val confessionBot = ConfessionBot(serverCommandHandler, logService)
    val jdaBuilder = JDABuilder.createDefault(
        botToken,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.MESSAGE_CONTENT
    ).addEventListeners(confessionBot)

    try {
        val jda = jdaBuilder.build()
        jda.awaitReady()
        println("Bot is ready. Syncing channels with database...")

        // Synchronize all configured channels with the database
        syncConfiguredChannels(service, confessionBot, jda)

        // Register slash commands
        jda.updateCommands().addCommands(confessionBot.getCommandData()).queue()
        println("Slash commands registered successfully.")

    } catch (e: Exception) {
        println("Failed to start the bot:")
        e.printStackTrace()
    }
}

/**
 * Synchronizes all configured channels from the Supabase database with the bot's in-memory configuration
 */
fun syncConfiguredChannels(service: RemoteService, confessionBot: ConfessionBot, jda: JDA) {
    service.getAllConfiguredServers { serverList, error ->
        if (serverList != null) {
            println("Retrieved ${serverList.size} configured server(s) from database.")

            for ((serverId, channelId) in serverList) {
                try {
                    val serverIdLong = serverId.toLong()
                    val channelObj = jda.getTextChannelById(channelId)

                    if (channelObj != null) {
                        confessionBot.registerConfiguredChannel(serverIdLong, channelObj)
                        println(
                            "Registered channel #${channelObj.name} for server ${
                                jda.getGuildById(
                                    serverIdLong
                                )?.name ?: serverId
                            }"
                        )
                    } else {
                        println("Warning: Could not find channel with ID $channelId for server $serverId")
                    }
                } catch (e: Exception) {
                    println("Error processing server $serverId: ${e.message}")
                }
            }

            println("Channel synchronization complete.")
        } else {
            println("Failed to retrieve configured servers: $error")
        }
    }
}