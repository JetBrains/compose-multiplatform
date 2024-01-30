package org.jetbrains.compose.resources

import kotlinx.coroutines.*

actual typealias TestReturnType = Any
actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit): TestReturnType =
    TODO("Implement if necessary. We focus on k/wasm target for now")