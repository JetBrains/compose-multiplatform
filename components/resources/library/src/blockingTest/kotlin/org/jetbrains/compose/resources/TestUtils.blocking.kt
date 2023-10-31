package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) = runBlocking(block = block)