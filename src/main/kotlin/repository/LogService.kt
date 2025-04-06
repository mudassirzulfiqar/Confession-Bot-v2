package repository

import models.ConfessionLog

/**
 * Service responsible for handling logging operations.
 */
class LogService(private val remoteService: RemoteRepository) {

    fun recordLog(log: ConfessionLog, callback: (Result<Unit>) -> Unit) {
        remoteService.saveLog(log, callback)
    }
}