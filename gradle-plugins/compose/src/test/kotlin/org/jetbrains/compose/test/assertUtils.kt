package org.jetbrains.compose.test

internal fun <T> Collection<T>.checkContains(vararg elements: T) {
    val expectedElements = elements.toMutableSet()
    forEach { expectedElements.remove(it) }
    if (expectedElements.isNotEmpty()) {
        error("Expected elements are missing from the collection: [${expectedElements.joinToString(", ")}]")
    }
}