// ConfessionBot.kt
import com.sun.net.httpserver.HttpServer
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val token = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN environment variable not set")

class ConfessionBot : ListenerAdapter() {

    private val configuredChannels = mutableMapOf<Long, TextChannel>()
    private val logs = mutableListOf<String>()

    companion object {
        const val HI_RESPONSE = """
            Hello! It's **CONFESSION** Bot 🤫

            **Send any message to me starting with the following pattern:**
            - `!c <Write your first confession>` (e.g., `!c Issay sub pta ha`)
        """
        const val SET_CONFESSION_CHANNEL_RESPONSE =
            "This channel has been set as the confession channel."
        const val SET_CONFESSION_CHANNEL_ERROR = "This command can only be used in a server."
        const val NO_CONFESSION_CHANNEL_CONFIGURED =
            "No confession channel has been configured yet."
        const val NO_CONFESSION_CHANNEL_FOR_SERVER =
            "No confession channel has been configured for this server yet."
        const val EMPTY_CONFESSION_ERROR = "Your confession cannot be empty."
        const val CONFESSION_SENT_RESPONSE = "Your confession has been sent anonymously!"
        const val SEND_CONFESSIONS_VIA_DM = "Please send confessions as a DM to the bot."
        const val INVALID_COMMAND_RESPONSE = """
            Invalid command. Please use one of the following patterns:
            - `!hi`
            - `!configure`
            - `!c <your confession>` (in DM)
        """
        const val GENERIC_ERROR_RESPONSE =
            "An error occurred while processing your request. Please try again later."
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val message = event.message.contentRaw.trim()
        val channel = event.channel as TextChannel
        val timestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        logs.add("[$timestamp] ${event.author.asTag}: $message")
        println(logs.last())

        try {
            when {
                message.equals("!hi", ignoreCase = true) -> {
                    Commands.handleHiCommand(channel)
                }

                message.startsWith("!configure") -> {
                    Commands.handleSetConfessionCommand(event, channel, configuredChannels)
                }

                message.startsWith("!c") && event.isFromType(ChannelType.PRIVATE) -> {
                    Commands.handleConfessionCommand(event, channel, configuredChannels)
                }

                message.startsWith("!c") && event.isFromGuild -> {
                    channel.sendMessage(SEND_CONFESSIONS_VIA_DM).queue()
                }

                else -> {
                    Commands.handleInvalidCommand(channel)
                }
            }
        } catch (e: Exception) {
            println("Error processing message: $message")
            e.printStackTrace()
            channel.sendMessage(GENERIC_ERROR_RESPONSE).queue()
        }
    }
}

fun main() {
    try {
        JDABuilder.createDefault(
            token,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.MESSAGE_CONTENT
        )
            .addEventListeners(ConfessionBot())
            .build()

        println("Bot is running...")

        // Start a simple HTTP health check server
        val server = HttpServer.create(InetSocketAddress(8080), 0)
        server.createContext("/") { exchange ->
            val response = "Service is running!"
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        println("Health check server running on port 8080")

    } catch (e: Exception) {
        println("Failed to start the bot:")
        e.printStackTrace()
    }
}