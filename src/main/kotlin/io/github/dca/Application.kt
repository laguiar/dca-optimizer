package io.github.dca

import io.ktor.server.application.*
import io.github.dca.plugins.configureHTTP
import io.github.dca.plugins.configureRouting
import io.github.dca.plugins.configureSerialization

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

// application.conf references the main function
@Suppress("unused")
fun Application.module() {
    configureSerialization()
//    configureHTTP()
    configureRouting()
}
