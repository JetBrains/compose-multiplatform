package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope

expect class TestReturnType

expect fun runBlockingTest(block: suspend CoroutineScope.() -> Unit): TestReturnType

internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)