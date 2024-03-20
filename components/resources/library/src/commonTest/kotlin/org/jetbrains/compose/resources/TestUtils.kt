package org.jetbrains.compose.resources

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestQuantityStringResource(key: String) = QuantityStringResource(
    "PLURALS:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)