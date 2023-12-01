package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope

expect fun runBlockingTest(block: suspend CoroutineScope.() -> Unit)

internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)