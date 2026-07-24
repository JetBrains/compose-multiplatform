package org.jetbrains.compose.internal.publishing.utils

import kotlinx.io.IOException
import org.gradle.api.logging.Logger

fun <T> retry(
    logger: Logger? = null,
    count: Int = 5,
    initialDelay: Long,
    exponentialDelayMultiplier: Int = 3,
    block: () -> T
): T {
    var retries = 0
    var delay = initialDelay
    while (true) {
        try {
            return block()
        } catch (e: IOException) {
            if (retries >= count) {
                throw e
            }
            retries++
            logger?.warn("Operation failed: ${e.message}. Retrying ($retries/$count) in ${delay}ms...")
            Thread.sleep(delay)
            delay *= exponentialDelayMultiplier
        }
    }
}