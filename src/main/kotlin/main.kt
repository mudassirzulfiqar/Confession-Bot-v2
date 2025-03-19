import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import repository.LogService
import repository.RemoteService

/**
 * Entry point of the Confession Bot application
 */
fun main() {
    try {
        val config = loadConfiguration()
        val (service, logService) = initializeServices(config)
        val serverCommandHandler = ServerCommandHandler(logService, service)
        val confessionBot = ConfessionBot(serverCommandHandler, logService)

        startBot(config.botToken, confessionBot).also { jda ->
            println("Bot is ready. Syncing channels with database...")
            syncConfiguredChannels(service, confessionBot, jda)
            registerSlashCommands(jda, confessionBot)
        }
    } catch (e: Exception) {
        println("Failed to start the bot:")
        e.printStackTrace()
    }
}

/**
 * Load configuration from environment variables
 */
private fun loadConfiguration(): Configuration {
    val dotenv = Dotenv.load()
    return Configuration(
        botToken = dotenv["BOT_TOKEN"]
            ?: throw IllegalStateException("BOT_TOKEN not found in environment"),
        databaseServerUrl = dotenv["DATABASE_SERVER_URL"]
            ?: throw IllegalStateException("DATABASE_SERVER_URL not found in environment"),
        apiKey = dotenv["API_KEY"]
            ?: throw IllegalStateException("API_KEY not found in environment")
    )
}

/**
 * Initialize repository services
 */
private fun initializeServices(config: Configuration): Pair<RemoteService, LogService> {
    val service = RemoteService(config.databaseServerUrl, config.apiKey)
    val logService = LogService(config.databaseServerUrl, config.apiKey)
    return service to logService
}

/**
 * Start the Discord bot with required configurations
 */
private fun startBot(botToken: String, confessionBot: ConfessionBot): JDA {
    val jdaBuilder = JDABuilder.createDefault(
        botToken,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.MESSAGE_CONTENT
    ).addEventListeners(confessionBot)

    return jdaBuilder.build().awaitReady()
}

/**
 * Register slash commands with Discord
 */
private fun registerSlashCommands(jda: JDA, confessionBot: ConfessionBot) {
    jda.updateCommands().addCommands(confessionBot.getCommandData()).queue()
    println("Slash commands registered successfully.")
}

/**
 * Synchronize configured channels from the database
 */
private fun syncConfiguredChannels(service: RemoteService, confessionBot: ConfessionBot, jda: JDA) {
    service.getAllConfiguredServers { result ->
        result.fold(
            onSuccess = { serverList ->
                println("Retrieved ${serverList.size} configured server(s) from database.")
                serverList.forEach { serverConfig ->
                    try {
                        val serverIdLong = serverConfig.serverId.toLong()
                        val channelObj = jda.getTextChannelById(serverConfig.channelId)

                        if (channelObj != null) {
                            confessionBot.registerConfiguredChannel(serverIdLong, channelObj)
                            println(
                                "Registered channel #${channelObj.name} for server ${
                                    jda.getGuildById(serverIdLong)?.name ?: serverConfig.serverId
                                }"
                            )
                        } else {
                            println("Warning: Could not find channel with ID ${serverConfig.channelId} for server ${serverConfig.serverId}")
                        }
                    } catch (e: Exception) {
                        println("Error processing server ${serverConfig.serverId}: ${e.message}")
                    }
                }
                println("Channel synchronization complete.")
            },
            onFailure = { error ->
                println("Failed to retrieve configured servers: ${error.message}")
            }
        )
    }
}

/**
 * Configuration data class for bot initialization
 */
private data class Configuration(
    val botToken: String,
    val databaseServerUrl: String,
    val apiKey: String
)