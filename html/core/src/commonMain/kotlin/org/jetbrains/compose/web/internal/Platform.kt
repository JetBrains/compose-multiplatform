package org.jetbrains.compose.web.internal

import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent

/**
 * Performs the unchecked cast used by the browser implementation while keeping
 * the call available to common code. JVM uses a regular Kotlin cast because its
 * browser declarations are stubs rather than JavaScript values.
 */
@PublishedApi
internal expect fun <T> Any?.unsafeCast(): T

/**
 * Small common contract for state that must not keep DOM elements alive.
 *
 */
internal interface WeakMap<K : Any, V : Any> {
    fun delete(key: K)
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun set(key: K, value: V)
}

internal expect fun <K : Any, V : Any> createWeakMap(): WeakMap<K, V>

/**
 * Reads browser-only mouse movement fields while preserving the existing
 * zero fallback for browsers where the fields are absent.
 */
internal expect fun MouseEvent.movementXOrZero(): Int

internal expect fun MouseEvent.movementYOrZero(): Int

/**
 * Preserves Compose HTML's legacy KeyboardEvent.locale API even though the
 * property is absent from the portable kotlinx-browser declarations.
 */
internal expect fun KeyboardEvent.localeCompat(): String

/**
 * Portable snapshot of the animation fields exposed by browser animation
 * events. kotlinx-browser 0.5.0 does not declare AnimationEvent itself.
 */
internal data class AnimationEventDetails(
    val animationName: String,
    val elapsedTime: Number,
    val pseudoElement: String,
)

internal expect fun Event.animationEventDetails(): AnimationEventDetails
