package com.google.r4a

import com.google.r4a.adapters.Dimension

/**
 * Just a dummy implementation to prove the behavior for a couple simple cases.
 * TODO: Should return true for deeply immutable objects, frozen objects, primitives, value types, inline classes of immutables, @Model
 * TODO: When we know at compile time, we shouldn't be doing a runtime check for this
 */
@PublishedApi
internal fun isEffectivelyImmutable(value: Any?): Boolean {
    return when (value) {
        is String,
        is Int,
        is Double,
        is Float,
        is Short,
        is Byte,
        is Char,
        is Boolean -> true
        is Dimension -> true
        else -> false
    }
}