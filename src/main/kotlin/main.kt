import di.AppConfig
import di.appModule
import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.GlobalContext.get
import repository.LogService
import repository.RemoteService

/**
 * Entry point of the Confession Bot application
 */
fun main() {
    try {
        val config = loadConfiguration()
        initializeKoin(config)

        val confessionBot: ConfessionBot = get().get()
        startBot(config.botToken, confessionBot)
            .also { jda -> setupBot(jda, confessionBot) }
    } catch (e: Exception) {
        println("Failed to start the bot:")
        e.printStackTrace()
    } finally {
        stopKoin()
    }
}

private fun setupBot(jda: JDA, confessionBot: ConfessionBot) {
    println("Bot is ready. Syncing channels with database...")
    val service: RemoteService = get().get()
    syncConfiguredChannels(service, confessionBot, jda)
    registerSlashCommands(jda, confessionBot)
}

/**
 * Initialize Koin dependency injection
 */
private fun initializeKoin(config: Configuration) {
    startKoin {
        modules(appModule)
        get().declare(config.databaseServerUrl)
        get().declare(config.apiKey)
    }
}

/**
 * Load configuration from environment variables
 */
private data class Configuration(
    val botToken: String,
    val databaseServerUrl: String,
    val apiKey: String
)

private fun loadConfiguration() = Dotenv.load().let { env ->
    Configuration(
        botToken = env["BOT_TOKEN"]
            ?: throw IllegalStateException("BOT_TOKEN not found in environment"),
        databaseServerUrl = env["DATABASE_SERVER_URL"]
            ?: throw IllegalStateException("DATABASE_SERVER_URL not found in environment"),
        apiKey = env["API_KEY"] ?: throw IllegalStateException("API_KEY not found in environment")
    )
}

private fun startBot(botToken: String, confessionBot: ConfessionBot): JDA =
    JDABuilder.createDefault(
        botToken,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.MESSAGE_CONTENT
    )
        .addEventListeners(confessionBot)
        .build()
        .awaitReady()

private fun registerSlashCommands(jda: JDA, confessionBot: ConfessionBot) {
    jda.updateCommands()
        .addCommands(confessionBot.getCommandData())
        .queue()
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
            onFailure = { e ->
                println("Failed to retrieve configured servers: ${e.message}")
            }
        )
    }
}