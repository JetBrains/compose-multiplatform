package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking


actual typealias TestReturnType = Unit

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit): TestReturnType {
    return runBlocking { block() }
}

