package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    TODO("To be implemented in PR 4031")
}