package org.jetbrains.compose.resources

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)