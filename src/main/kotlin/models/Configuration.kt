package models

/**
 * Configuration data class for bot initialization
 */
data class Configuration(
    val botToken: String,
    val databaseServerUrl: String,
    val apiKey: String
)