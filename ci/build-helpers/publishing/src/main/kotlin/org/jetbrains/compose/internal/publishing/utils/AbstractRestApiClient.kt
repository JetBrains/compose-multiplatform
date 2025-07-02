/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import okhttp3.*
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import org.gradle.api.logging.Logger
import java.net.URL
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

internal class RestApiClient(
    private val serverUrl: String,
    private val user: String,
    private val password: String,
    private val logger: Logger,
) : AutoCloseable {
    private val okClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(Duration.ofMinutes(1))
            .build()
    }

    fun buildRequest(urlPath: String, configure: Request.Builder.() -> Unit): Request =
        Request.Builder().apply {
            addHeader("Authorization", Credentials.basic(user, password))
            url(URL("$serverUrl/$urlPath"))
            configure()
        }.build()

    fun <T> execute(
        request: Request,
        retries: Int = 5,
        delaySec: Long = 10,
        processResponse: (ResponseBody) -> T
    ): T {
        val message = "Remote request #${globalRequestCounter.incrementAndGet()}"
        val startTimeNs = System.nanoTime()
        logger.info("$message: ${request.method} '${request.url}'")
        val delayMs = delaySec * 1000

        for (i in 1..retries) {
            try {
                return okClient.newCall(request).execute().use { response ->
                    val endTimeNs = System.nanoTime()
                    logger.info("$message: finished in ${(endTimeNs - startTimeNs)/1_000_000} ms")

                    if (!response.isSuccessful)
                        throw RequestError(request, response)

                    val responseBody = response.body ?: RealResponseBody(null, 0, Buffer())
                    processResponse(responseBody)
                }
            } catch (e: Exception) {
                if (i == retries) {
                    throw RuntimeException("$message: failed all $retries attempts, see nested exception for details", e)
                }
                logger.info("$message: retry #$i of $retries failed. Retrying in $delayMs ms\n${e.message}")
                Thread.sleep(delayMs)
            }
        }

        error("Unreachable")
    }

    override fun close() {
        okClient.connectionPool.evictAll()
    }

    companion object {
        private val globalRequestCounter = AtomicLong()
    }
}
