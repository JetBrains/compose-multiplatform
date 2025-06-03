/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable

/**
 * Data class for receiving benchmark results from client.
 */
@Serializable
data class BenchmarkResultFromClient(
    val name: String,
    val stats: String // JSON string of BenchmarkStats
)

/**
 * Starts a Ktor server to receive benchmark results from browsers
 * and save them to disk in the same format as the direct disk saving mechanism.
 */
object BenchmarksSaveServer {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start(port: Int = BENCHMARK_SERVER_PORT) {
        if (server != null) {
            println("Benchmark server is already running")
            return
        }

        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json()
            }
            install(CORS) {
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowHeader(HttpHeaders.ContentType)
                anyHost()
            }
            routing {
                post("/benchmark") {
                    val result = call.receive<BenchmarkResultFromClient>()
                    if (result.name.isEmpty()) {
                        println("Stopping server! Received empty name from client")
                        call.respond(HttpStatusCode.OK, "Server stopped.")
                        stop()
                        return@post
                    }
                    println("Received benchmark result for: ${result.name}")

                    withContext(Dispatchers.IO) {
                        if (Config.saveStatsToJSON) {
                            saveJson(result.name, result.stats)
                        }

                        if (Config.saveStatsToCSV) {
                            // TODO: for CSV, we would need to convert JSON to the values
                            println("CSV results are not yet supported for the browser.")
                        }
                    }

                    call.respond(HttpStatusCode.OK, "Benchmark result saved")
                }

                get("/") {
                    call.respondText("Benchmark server is running", ContentType.Text.Plain)
                }
            }
        }.start(wait = true)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        println("Benchmark server stopped")
        System.exit(0)
    }
}
