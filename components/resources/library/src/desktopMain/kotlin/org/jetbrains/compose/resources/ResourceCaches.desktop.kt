package org.jetbrains.compose.resources

import kotlinx.coroutines.runBlocking

/**
 * Clears any cached resources maintained internally by the system.
 *
 * It can be useful to release memory or reset cached resources that
 * may be changed or no longer be required.
 */
@ExperimentalResourceApi
fun ResourceCaches.clearBlocking() {
    runBlocking { clear() }
}