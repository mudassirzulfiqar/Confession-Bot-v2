import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val token = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN environment variable not set")

class ConfessionBot : ListenerAdapter() {

    private val configuredChannels =
        mutableMapOf<Long, TextChannel>() // Guild ID -> Confession Channel
    private val logs = mutableListOf<String>() // Log of messages with timestamps and sender details

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
            - `!setconfession`
            - `!c <your confession>` (in DM)
        """
        const val GENERIC_ERROR_RESPONSE =
            "An error occurred while processing your request. Please try again later."
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Ignore bot messages
        if (event.author.isBot) return

        val message = event.message.contentRaw.trim()
        val channel = event.channel
        val timestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // Log the message with timestamp and sender details
        logs.add("[$timestamp] ${event.author.asTag}: $message")
        println(logs.last())

        try {
            when {
                message.equals("!hi", ignoreCase = true) -> {
                    channel.sendMessage(HI_RESPONSE.trimIndent()).queue()
                }

                message.startsWith("!setconfession") -> {
                    if (event.isFromGuild) { // Check if the message is from a guild
                        val guildId = event.guild.idLong
                        configuredChannels[guildId] = channel as TextChannel
                        channel.sendMessage(SET_CONFESSION_CHANNEL_RESPONSE).queue()
                    } else {
                        channel.sendMessage(SET_CONFESSION_CHANNEL_ERROR).queue()
                    }
                }

                message.startsWith("!c") && event.isFromType(ChannelType.PRIVATE) -> {
                    val confession = message.removePrefix("!c").trim()
                    if (confession.isEmpty()) {
                        channel.sendMessage(EMPTY_CONFESSION_ERROR).queue()
                        return
                    }

                    val guildId =
                        configuredChannels.keys.firstOrNull() // Get any configured guild for simplicity
                    if (guildId == null) {
                        channel.sendMessage(NO_CONFESSION_CHANNEL_CONFIGURED).queue()
                        return
                    }

                    val confessionChannel = configuredChannels[guildId]
                    if (confessionChannel == null) {
                        channel.sendMessage(NO_CONFESSION_CHANNEL_FOR_SERVER).queue()
                        return
                    }

                    // Send confession anonymously
                    val embed = EmbedBuilder()
                        .setTitle("Anonymous Confession")
                        .setDescription(confession)
                        .setColor(0xFF5733) // Optional: Set a color for the embed
                        .build()

                    confessionChannel.sendMessageEmbeds(embed).queue()
                    channel.sendMessage(CONFESSION_SENT_RESPONSE).queue()
                }

                message.startsWith("!c") && event.isFromGuild -> {
                    channel.sendMessage(SEND_CONFESSIONS_VIA_DM).queue()
                }

                else -> {
                    channel.sendMessage(INVALID_COMMAND_RESPONSE.trimIndent()).queue()
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
    } catch (e: Exception) {
        println("Failed to start the bot:")
        e.printStackTrace()
    }
}