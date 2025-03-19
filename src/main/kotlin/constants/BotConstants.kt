package constants

object BotConstants {
    const val HI_RESPONSE = """
        🤫
        **Send any message to me starting with the following pattern:**
        - `!c <Write your first confession>` (e.g., `!c Issay sub pta ha`)
    """
    const val SET_CONFESSION_CHANNEL_RESPONSE = "This channel has been set as the confession channel."
    const val SET_CONFESSION_CHANNEL_ERROR = "This command can only be used in a server."
    const val SOMETHING_WENT_WRONG = "Something went wrong. Please try again later."
    const val NO_CONFESSION_CHANNEL_CONFIGURED = "No confession channel has been configured yet."
    const val NO_CONFESSION_CHANNEL_FOR_SERVER = "No confession channel has been configured for this server yet."
    const val EMPTY_CONFESSION_ERROR = "Your confession cannot be empty."
    const val CONFESSION_SENT_RESPONSE = "Your confession has been sent anonymously!"
    const val SEND_CONFESSIONS_VIA_DM = "Please send confessions as a DM to the bot."
    const val GENERIC_ERROR_RESPONSE = "An error occurred while processing your request. Please try again later."
    const val INVALID_COMMAND_RESPONSE = """
        Invalid command. Please use one of the following patterns:
        - `!hi
        - `!configure`
        - `!c <your confession>` (in DM)
    """
    const val INVALID_GUILD_ID = "Invalid guild ID."
    const val INVALID_CHANNEL_ID = "Invalid channel ID."
    const val CONFESSION_CHANNEL_REMOVED = "The confession channel has been removed."
    const val NO_CONFESSION_CHANNEL_TO_REMOVE = "No confession channel is configured to remove."
}