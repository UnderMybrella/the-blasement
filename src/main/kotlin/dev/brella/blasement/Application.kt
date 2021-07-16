package dev.brella.blasement

import io.ktor.application.*
import dev.brella.blasement.plugins.configureMonitoring
import dev.brella.blasement.plugins.configureRouting
import dev.brella.blasement.plugins.configureSerialization
import dev.brella.blasement.plugins.configureSockets
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.netty.handler.codec.http.HttpServerCodec
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit =
    EngineMain.main(args)

public object EngineMain {
    /**
     * Main function for starting EngineMain with Netty
     * Creates an embedded Netty application with an environment built from command line arguments.
     */
    @JvmStatic
    public fun main(args: Array<String>) {
        val applicationEnvironment = commandLineEnvironment(args)
        val engine = NettyApplicationEngine(applicationEnvironment) {
            loadConfiguration(applicationEnvironment.config)
            httpServerCodec = {
                HttpServerCodec(8192, 8192, 8192)
            }
        }

        engine.addShutdownHook {
            engine.stop(3, 5, TimeUnit.SECONDS)
        }
        engine.start(true)
    }

    private fun NettyApplicationEngine.Configuration.loadConfiguration(config: ApplicationConfig) {
        val deploymentConfig = config.config("ktor.deployment")
        loadCommonConfiguration(deploymentConfig)
        deploymentConfig.propertyOrNull("requestQueueLimit")?.getString()?.toInt()?.let {
            requestQueueLimit = it
        }
        deploymentConfig.propertyOrNull("shareWorkGroup")?.getString()?.toBoolean()?.let {
            shareWorkGroup = it
        }
        deploymentConfig.propertyOrNull("responseWriteTimeoutSeconds")?.getString()?.toInt()?.let {
            responseWriteTimeoutSeconds = it
        }
    }
}

/**
 * Please note that you can use any other name instead of *module*.
 * Also note that you can have more then one modules in your application.
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    configureRouting()
    configureSockets()
    configureSerialization()
    configureMonitoring()
}
