/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable

/**
 * Browser implementation for saving benchmark results.
 * Instead of trying to save directly to disk (which would fail with UnsupportedOperationException),
 * this implementation sends the results to a server via HTTP.
 */
actual fun saveBenchmarkStats(name: String, stats: BenchmarkStats) {
    GlobalScope.launch {
        BenchmarksSaveServerClient.sendBenchmarkResult(name, stats)
    }
}

/**
 * Client for sending benchmark results to the server
 */
object BenchmarksSaveServerClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private var resultsSendingInProgress = 0

    private fun serverUrlRoot(): String {
        val protocol = window.location.protocol
        val hostname = window.location.hostname
        return "$protocol//$hostname:$BENCHMARK_SERVER_PORT"
    }

    private fun serverUrl(): String {
        return "${serverUrlRoot()}/benchmark"
    }

    /**
     * Sends benchmark results to the server
     */
    suspend fun sendBenchmarkResult(name: String, stats: BenchmarkStats) {
        resultsSendingInProgress++
        println("Sending results: $name")
        sendBenchmarkResult(name, stats.toJsonString())
        resultsSendingInProgress--
        println("Benchmark result sent to server: ${serverUrl()}")
    }

    private suspend fun sendBenchmarkResult(name: String, stats: String) {
        try {

            val result = BenchmarkResultToServer(
                name = name,
                stats = stats
            )

            client.post(serverUrl()) {
                contentType(ContentType.Application.Json)
                setBody(result)
            }
        } catch (e: Throwable) {
            println("Error sending benchmark result to server: ${e.message}")
        }
    }

    suspend fun stopServer() {
        while (resultsSendingInProgress > 0) {
            yield()
        }
        sendBenchmarkResult("", "")
    }

    suspend fun isServerAlive(): Boolean {
        // waiting for the server to start for 2 seconds
        val TIMEOUT = 2000
        val DELTA = 100L
        var delayed = 0L
        while (delayed < TIMEOUT) {
            try {
                return client.get(serverUrlRoot()).status == HttpStatusCode.OK
            } catch (_: Throwable) {
                delayed += DELTA
                delay(DELTA)
            }
        }
        return false
    }
}

/**
 * Data class for sending benchmark results to the server
 */
@Serializable
data class BenchmarkResultToServer(
    val name: String,
    val stats: String // JSON string of BenchmarkStats
)