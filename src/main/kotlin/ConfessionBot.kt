// ConfessionBot.kt
import constants.BotConstants
import models.ConfessionLog
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import repository.LogService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

/**
 * Main bot class that handles Discord events and manages confession functionality
 */
class ConfessionBot(
    private val serverCommandHandler: ServerCommandHandler,
    private val logService: LogService
) : ListenerAdapter() {

    private val configuredChannels = mutableMapOf<Long, TextChannel>()

    /**
     * Register a configured channel from the database during synchronization
     */
    fun registerConfiguredChannel(serverId: Long, channel: TextChannel) {
        configuredChannels[serverId] = channel
    }

    /**
     * Get the current configured channels map
     */
    fun getConfiguredChannels(): Map<Long, TextChannel> {
        return configuredChannels.toMap()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return // Ignore messages from bots

        val message = event.message.contentRaw.trim()

        try {
            when (event.channel) {
                is TextChannel -> processServerMessage(event, message, event.channel as TextChannel)
                is PrivateChannel -> processPrivateMessage(
                    event, message, event.channel as PrivateChannel
                )

                else -> println("Unsupported channel type.")
            }
        } catch (e: Exception) {
            handleProcessingError(e, event)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.user.isBot) return

        when (event.name) {
            "confess" -> handleConfessCommand(event)
            "configure" -> handleConfigureCommand(event)
            "remove" -> handleRemoveCommand(event)
            else -> event.reply(BotConstants.INVALID_COMMAND_RESPONSE).setEphemeral(true).queue()
        }
    }

    private fun handleConfessCommand(event: SlashCommandInteractionEvent) {
        val confession = event.getOption("message")?.asString
        if (confession.isNullOrBlank()) {
            event.reply(BotConstants.EMPTY_CONFESSION_ERROR).setEphemeral(true).queue()
            return
        }

        val channel = configuredChannels[event.guild?.idLong]
        if (channel != null) {
            serverCommandHandler.handleConfessionCommand(channel, confession)
            logConfession(event.user.asTag, confession)
            event.reply(BotConstants.CONFESSION_SENT_RESPONSE).setEphemeral(true).queue()
        } else {
            event.reply(BotConstants.NO_CONFESSION_CHANNEL_CONFIGURED).setEphemeral(true).queue()
        }
    }

    private fun handleConfigureCommand(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel as? TextChannel
        if (channel != null) {
            serverCommandHandler.handleSetConfessionCommand(event, channel, configuredChannels)
        } else {
            event.reply(BotConstants.SET_CONFESSION_CHANNEL_ERROR).setEphemeral(true).queue()
        }
    }

    private fun handleRemoveCommand(event: SlashCommandInteractionEvent) {
        serverCommandHandler.handleRemoveConfessionChannelCommand(event, configuredChannels)
    }

    fun getCommandData(): List<CommandData> {
        return listOf(
            Commands.slash("confess", "Send a confession anonymously")
                .addOption(OptionType.STRING, "message", "The confession message", true),
            Commands.slash("configure", "Configure the confession channel")
                .addOption(
                    OptionType.CHANNEL,
                    "channel",
                    "The channel to set as confession channel",
                    true
                ),
            Commands.slash("remove", "Remove the configured confession channel")
        )
    }

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
                channel.sendMessage(BotConstants.SEND_CONFESSIONS_VIA_DM).queue()
            }
        }
    }

    private fun processPrivateMessage(
        event: MessageReceivedEvent,
        message: String,
        channel: PrivateChannel
    ) {
        when {
            message.startsWith("!c ") -> {
                serverCommandHandler.handleConfessionCommand(event, channel, configuredChannels)
                logConfession(event.author.asTag, message)
            }

            message.startsWith("!channel ") -> {
                serverCommandHandler.handleSetChannelCommand(event, channel, configuredChannels)
            }

            else -> {
                channel.sendMessage(BotConstants.INVALID_COMMAND_RESPONSE).queue()
            }
        }
    }

    private fun logConfession(userTag: String, message: String) {
        val timestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val log = ConfessionLog(
            message = "$userTag: $message",
            level = "INFO",
            timestamp = timestamp
        )
        logService.recordLog(log) { result ->
            result.onFailure { println("Failed to record log: ${it.message}") }
        }
    }

    private fun handleProcessingError(exception: Exception, event: MessageReceivedEvent) {
        val channel = event.channel
        val errorMessage = "Error processing message: ${event.message.contentRaw.trim()}"
        println(errorMessage)
        exception.printStackTrace()

        when (channel) {
            is TextChannel -> channel.sendMessage(BotConstants.GENERIC_ERROR_RESPONSE).queue()
            is PrivateChannel -> channel.sendMessage(BotConstants.GENERIC_ERROR_RESPONSE).queue()
            else -> println("Error occurred in unsupported channel type.")
        }
    }
}
