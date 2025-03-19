import constants.BotConstants
import models.ConfessionLog
import models.ServerConfig
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import repository.RemoteService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import repository.LogService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Handles all server-related commands and interactions for the confession bot
 */
class ServerCommandHandler(
    private val logService: LogService,
    private val remoteService: RemoteService
) : ListenerAdapter() {

    fun handleHiCommand(channel: TextChannel) {
        channel.sendMessage(BotConstants.HI_RESPONSE.trimIndent()).queue()
    }

    fun handleSetConfessionCommand(
        event: MessageReceivedEvent,
        channel: TextChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        if (event.isFromGuild) {
            val guildId = event.guild.idLong
            configuredChannels[guildId] = channel

            val config = ServerConfig(guildId.toString(), channel.id)
            remoteService.saveDiscordChannel(config) { result ->
                result.fold(
                    onSuccess = {
                        channel.sendMessage(BotConstants.SET_CONFESSION_CHANNEL_RESPONSE).queue()
                    },
                    onFailure = {
                        channel.sendMessage(BotConstants.SOMETHING_WENT_WRONG).queue()
                    }
                )
            }
        } else {
            channel.sendMessage(BotConstants.SET_CONFESSION_CHANNEL_ERROR).queue()
        }
    }

    fun handleConfessionCommand(
        event: MessageReceivedEvent,
        channel: PrivateChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val confession = event.message.contentRaw.removePrefix("!c").trim()
        if (confession.isEmpty()) {
            channel.sendMessage(BotConstants.EMPTY_CONFESSION_ERROR).queue()
            return
        }

        val guildId = configuredChannels.keys.firstOrNull()
        if (guildId == null) {
            channel.sendMessage(BotConstants.NO_CONFESSION_CHANNEL_CONFIGURED).queue()
            return
        }

        val confessionChannel = configuredChannels[guildId]
        if (confessionChannel == null) {
            channel.sendMessage(BotConstants.NO_CONFESSION_CHANNEL_FOR_SERVER).queue()
            return
        }

        val embed = EmbedBuilder()
            .setTitle("Anonymous Confession")
            .setDescription(confession)
            .setColor(0xFF5733)
            .build()

        confessionChannel.sendMessageEmbeds(embed).queue()
        channel.sendMessage(BotConstants.CONFESSION_SENT_RESPONSE).queue()

        // Log the confession
        val timestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val log = ConfessionLog(
            message = "${event.author.asTag}: $confession",
            level = "INFO",
            timestamp = timestamp
        )
        logService.recordLog(log) { result ->
            result.onFailure { println("Failed to record log: ${it.message}") }
        }
    }

    fun handleSetChannelCommand(
        event: MessageReceivedEvent,
        channel: PrivateChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val guildId = event.message.contentRaw.removePrefix("!channel").trim().toLongOrNull()
        if (guildId == null) {
            channel.sendMessage(BotConstants.INVALID_GUILD_ID).queue()
            return
        }

        val guildChannel = event.jda.getTextChannelById(guildId)
        if (guildChannel == null) {
            channel.sendMessage(BotConstants.INVALID_CHANNEL_ID).queue()
            return
        }

        configuredChannels[guildId] = guildChannel

        val config = ServerConfig(guildId.toString(), guildChannel.id)
        remoteService.saveDiscordChannel(config) { result ->
            result.fold(
                onSuccess = {
                    channel.sendMessage(BotConstants.SET_CONFESSION_CHANNEL_RESPONSE).queue()
                },
                onFailure = { error ->
                    channel.sendMessage("${BotConstants.SOMETHING_WENT_WRONG} Error: ${error.message}")
                        .queue()
                }
            )
        }
    }

    fun handleConfessionCommand(channel: TextChannel, confession: String) {
        val embed = EmbedBuilder()
            .setTitle("Anonymous Confession")
            .setDescription(confession)
            .setColor(0xFF5733)
            .build()

        channel.sendMessageEmbeds(embed).queue()
    }

    fun handleSetConfessionCommand(
        event: SlashCommandInteractionEvent,
        channel: TextChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val guildId = event.guild?.idLong
        if (guildId == null) {
            event.reply(BotConstants.SET_CONFESSION_CHANNEL_ERROR).setEphemeral(true).queue()
            return
        }

        configuredChannels[guildId] = channel

        val config = ServerConfig(guildId.toString(), channel.id)
        remoteService.saveDiscordChannel(config) { result ->
            result.fold(
                onSuccess = {
                    event.reply("Confession channel set to ${channel.name}").setEphemeral(true)
                        .queue()
                },
                onFailure = { error ->
                    println("Error saving channel configuration: ${error.message}")
                    event.reply(BotConstants.SOMETHING_WENT_WRONG).setEphemeral(true).queue()
                }
            )
        }
    }

    fun handleRemoveConfessionChannelCommand(
        event: SlashCommandInteractionEvent,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val guildId = event.guild?.idLong
        if (guildId == null) {
            event.reply(BotConstants.SET_CONFESSION_CHANNEL_ERROR).setEphemeral(true).queue()
            return
        }

        configuredChannels.remove(guildId)
        event.reply(BotConstants.CONFESSION_CHANNEL_REMOVED).setEphemeral(true).queue()
    }
}