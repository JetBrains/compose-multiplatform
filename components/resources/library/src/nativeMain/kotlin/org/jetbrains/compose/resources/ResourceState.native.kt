package org.jetbrains.compose.resources

import kotlinx.coroutines.runBlocking

//there is no thread interruption on native targets, so the standard `runBlocking` is enough here
internal actual fun <T> runResourceBlocking(block: suspend () -> T): T = runBlocking { block() }