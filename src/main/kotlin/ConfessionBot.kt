// ConfessionBot.kt
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import repository.LogService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ConfessionBot(
    private val serverCommandHandler: ServerCommandHandler,
    private val logService: LogService
) : ListenerAdapter() {

    private val configuredChannels = mutableMapOf<Long, TextChannel>()
    private val logs = mutableListOf<String>()

    companion object {
        const val HI_RESPONSE = """
            🤫
            **Send any message to me starting with the following pattern:**
            - `!c <Write your first confession>` (e.g., `!c Issay sub pta ha`)
        """
        const val SET_CONFESSION_CHANNEL_RESPONSE =
            "This channel has been set as the confession channel."
        const val SET_CONFESSION_CHANNEL_ERROR = "This command can only be used in a server."
        const val SOMETHING_WENT_WRONG = "Something went wrong. Please try again later."
        const val NO_CONFESSION_CHANNEL_CONFIGURED =
            "No confession channel has been configured yet."
        const val NO_CONFESSION_CHANNEL_FOR_SERVER =
            "No confession channel has been configured for this server yet."
        const val EMPTY_CONFESSION_ERROR = "Your confession cannot be empty."
        const val CONFESSION_SENT_RESPONSE = "Your confession has been sent anonymously!"
        const val SEND_CONFESSIONS_VIA_DM = "Please send confessions as a DM to the bot."
        const val GENERIC_ERROR_RESPONSE =
            "An error occurred while processing your request. Please try again later."
        const val INVALID_COMMAND_RESPONSE = """
            Invalid command. Please use one of the following patterns:
            - `!hi
            - `!configure`
            - `!c <your confession>` (in DM)
        """
        const val INVALID_GUILD_ID = "Invalid guild ID."
        const val INVALID_CHANNEL_ID = "Invalid channel ID."
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return // Ignore messages from bots

        val message = event.message.contentRaw.trim()

        // Process the message based on channel type
        try {
            when (event.channel) {
                is TextChannel -> processServerMessage(event, message, event.channel as TextChannel)
                is PrivateChannel -> processPrivateMessage(
                    event,
                    message,
                    event.channel as PrivateChannel
                )

                else -> println("Unsupported channel type.")
            }
        } catch (e: Exception) {
            handleProcessingError(e, event)
        }
    }

    // Method to process server (guild) messages
    private fun processServerMessage(
        event: MessageReceivedEvent,
        message: String,
        channel: TextChannel
    ) {
        when {
            message.equals("!hi", ignoreCase = true) -> {
                serverCommandHandler.handleHiCommand(channel)
            }

            message.startsWith("!configure") -> {
                serverCommandHandler.handleSetConfessionCommand(event, channel, configuredChannels)
            }

            message.startsWith("!c") -> {
                channel.sendMessage(SEND_CONFESSIONS_VIA_DM).queue()
            }

            else -> {

            }
        }
    }

    // Method to process private (DM) messages
    private fun processPrivateMessage(
        event: MessageReceivedEvent,
        message: String,
        channel: PrivateChannel
    ) {
        when {
            message.startsWith("!c ") -> {
                serverCommandHandler.handleConfessionCommand(event, channel, configuredChannels)
                val timestamp =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val logEntry = "[$timestamp] ${event.author.asTag}: $message"

                // Add log entry and record it
                logs.add(logEntry)
                println(logEntry)
                logService.recordLog(logEntry, "INFO") { success, error ->
                    if (!success) println("Failed to record log: $error")
                }
            }

            message.startsWith("!channel ")->{
                serverCommandHandler.handleSetChannelCommand(event, channel, configuredChannels)
            }

            else -> {
                channel.sendMessage(INVALID_COMMAND_RESPONSE).queue()
            }
        }
    }

    // Method to handle errors
    private fun handleProcessingError(exception: Exception, event: MessageReceivedEvent) {
        val channel = event.channel
        val errorMessage = "Error processing message: ${event.message.contentRaw.trim()}"
        println(errorMessage)
        exception.printStackTrace()

        when (channel) {
            is TextChannel -> channel.sendMessage(GENERIC_ERROR_RESPONSE).queue()
            is PrivateChannel -> channel.sendMessage(GENERIC_ERROR_RESPONSE).queue()
            else -> println("Error occurred in unsupported channel type.")
        }
    }
}
