package io.github.dca

import io.github.dca.plugins.configureRequestValidation
import io.github.dca.plugins.configureRouting
import io.github.dca.plugins.configureSerialization
import io.github.dca.plugins.configureStatusPages
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configureRequestValidation()
    configureRouting()
}
