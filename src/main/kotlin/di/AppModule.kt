package di

import ConfessionBot
import ServerCommandHandler
import org.koin.dsl.module
import repository.LogService
import repository.RemoteService

data class AppConfig(
    val serverUrl: String,
    val apiKey: String
)

val appModule = module {
    single { AppConfig(get(), get()) }
    single { LogService(get<AppConfig>().serverUrl, get<AppConfig>().apiKey) }
    single { RemoteService(get<AppConfig>().serverUrl, get<AppConfig>().apiKey) }
    single { ServerCommandHandler(get(), get()) }
    single { ConfessionBot(get(), get()) }
}