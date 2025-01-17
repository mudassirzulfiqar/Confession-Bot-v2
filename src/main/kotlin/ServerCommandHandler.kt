// Commands.kt
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import repository.LogService
import repository.RemoteService

class ServerCommandHandler(val service: RemoteService) {

    fun handleHiCommand(channel: TextChannel) {
        channel.sendMessage(ConfessionBot.HI_RESPONSE.trimIndent()).queue()
    }

    fun handleSetConfessionCommand(
        event: MessageReceivedEvent,
        channel: TextChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        if (event.isFromGuild) {
            val guildId = event.guild.idLong
            configuredChannels[guildId] = channel
            service.saveDiscordChannel(guildId.toString(), channel.id, { success, error ->
                println("Success: $success, Error: $error")
                if (success) {
                    channel.sendMessage(ConfessionBot.SET_CONFESSION_CHANNEL_RESPONSE).queue()
                } else {
                    channel.sendMessage(ConfessionBot.SOMETHING_WENT_WRONG).queue()
                }
            })
        } else {
            channel.sendMessage(ConfessionBot.SET_CONFESSION_CHANNEL_ERROR).queue()
        }
    }

    fun handleConfessionCommand(
        event: MessageReceivedEvent,
        channel: TextChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val confession = event.message.contentRaw.removePrefix("!c").trim()
        if (confession.isEmpty()) {
            channel.sendMessage(ConfessionBot.EMPTY_CONFESSION_ERROR).queue()
            return
        }

        val guildId = configuredChannels.keys.firstOrNull()
        if (guildId == null) {
            channel.sendMessage(ConfessionBot.NO_CONFESSION_CHANNEL_CONFIGURED).queue()
            return
        }

        val confessionChannel = configuredChannels[guildId]
        if (confessionChannel == null) {
            channel.sendMessage(ConfessionBot.NO_CONFESSION_CHANNEL_FOR_SERVER).queue()
            return
        }

        val embed = EmbedBuilder().setTitle("Anonymous Confession").setDescription(confession)
            .setColor(0xFF5733).build()

        confessionChannel.sendMessageEmbeds(embed).queue()
        channel.sendMessage(ConfessionBot.CONFESSION_SENT_RESPONSE).queue()
    }

    fun handleConfessionCommand(
        event: MessageReceivedEvent,
        channel: PrivateChannel,
        configuredChannels: MutableMap<Long, TextChannel>
    ) {
        val confession = event.message.contentRaw.removePrefix("!c").trim()
        if (confession.isEmpty()) {
            channel.sendMessage(ConfessionBot.EMPTY_CONFESSION_ERROR).queue()
            return
        }

        val guildId = configuredChannels.keys.firstOrNull()
        if (guildId == null) {
            channel.sendMessage(ConfessionBot.NO_CONFESSION_CHANNEL_CONFIGURED).queue()
            return
        }

        val confessionChannel = configuredChannels[guildId]
        if (confessionChannel == null) {
            channel.sendMessage(ConfessionBot.NO_CONFESSION_CHANNEL_FOR_SERVER).queue()
            return
        }

        val embed = EmbedBuilder().setTitle("Anonymous Confession").setDescription(confession)
            .setColor(0xFF5733).build()

        confessionChannel.sendMessageEmbeds(embed).queue()
        channel.sendMessage(ConfessionBot.CONFESSION_SENT_RESPONSE).queue()
    }

    fun handleInvalidCommand(channel: TextChannel) {
        channel.sendMessage(ConfessionBot.INVALID_COMMAND_RESPONSE.trimIndent()).queue()
    }

}