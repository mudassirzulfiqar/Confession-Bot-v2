import io.github.cdimascio.dotenv.Dotenv
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
    val serverCommandHandler = ServerCommandHandler(service)

    service.getLatestConfiguredServerId { serverId, error ->
        if (serverId != null) {
            println("Latest server ID: $serverId")
            try {
                val jda = JDABuilder.createDefault(
                    botToken,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT
                ).addEventListeners(ConfessionBot(serverCommandHandler, logService)).build()

                println("Bot is running...")
            } catch (e: Exception) {
                println("Failed to start the bot:")
                e.printStackTrace()
            }

        } else {
            println("Failed to fetch server ID: $error")
        }
    }


}