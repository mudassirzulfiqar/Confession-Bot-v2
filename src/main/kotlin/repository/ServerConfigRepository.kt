package repository

import models.ServerConfig

/**
 * Repository responsible for managing server configurations.
 */
class ServerConfigRepository(private val remoteService: RemoteRepository) {

    fun saveServerConfig(config: ServerConfig, callback: (Result<Unit>) -> Unit) {
        remoteService.saveServerConfig(config, callback)
    }

    fun getAllServerConfigs(callback: (Result<List<ServerConfig>>) -> Unit) {
        remoteService.getAllServerConfigs(callback)
    }
}