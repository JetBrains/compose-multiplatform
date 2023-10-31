package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope

expect fun runBlockingTest(block: suspend CoroutineScope.() -> Unit)