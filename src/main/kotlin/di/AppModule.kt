package di

import ConfessionBot
import ServerCommandHandler
import org.koin.dsl.module
import repository.LogService
import repository.RemoteRepository
import repository.ServerConfigRepository

data class AppConfig(
    val serverUrl: String,
    val apiKey: String
)

val appModule = module {
    single { AppConfig(get(), get()) }
    single { RemoteRepository(get<AppConfig>().serverUrl, get<AppConfig>().apiKey) }
    single { LogService(get()) }
    single { ServerConfigRepository(get()) }
    single { ServerCommandHandler(get(), get()) }
    single { ConfessionBot(get(), get()) }
}