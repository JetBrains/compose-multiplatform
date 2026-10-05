/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(InternalComposeApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "SEALED_INHERITOR_IN_DIFFERENT_MODULE", "SEALED_INHERITOR_IN_DIFFERENT_PACKAGE")

package org.jetbrains.compose.web

import androidx.compose.runtime.*
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.compose.web.internal.unsafeCast

/**
 * An insertion-only interpreter for the compiler protocol. Values never need to be compared or
 * retained because there is no second composition. Only local-provider scope, group position,
 * and effect callbacks survive a call.
 */
internal class SinglePassComposer : Composer {
    override val inserting get() = true
    override val skipping get() = false
    override val defaultsInvalid get() = false
    override val recomposeScope: RecomposeScope? get() = null
    override val recomposeScopeIdentity: Any? get() = null
    override fun shouldExecute(parametersChanged: Boolean, flags: Int) = true

    // Primitive stacks retain only the active path, not every group in the rendered document.
    private var children = IntArray(64)
    private var hashes = LongArray(64)
    private var depth = 0
    private var locals = emptyCompositionLocalMap
    private val providers = ArrayList<PersistentCompositionLocalMap>()
    private val observers = ArrayList<RememberObserver>()
    private val effects = ArrayList<() -> Unit>()
    private var rememberedCount = 0

    private fun enter(key: Int, keyed: Boolean = false) {
        if (depth + 1 == children.size) {
            val newSize = children.size * 2
            children = children.copyOf(newSize)
            hashes = hashes.copyOf(newSize)
        }
        val ordinal = if (keyed) 0 else children[depth]++
        val parentHash = if (depth == 0) 0L else hashes[depth - 1]
        hashes[depth] = (parentHash.rotateLeft(3) xor key.toLong()).rotateLeft(3) xor ordinal.toLong()
        children[++depth] = 0
    }

    private fun leave() {
        check(depth > 0)
        depth--
    }

    override val currentMarker get() = depth
    override fun endToMarker(marker: Int) {
        require(marker in 0..depth)
        depth = marker
    }

    override val compositeKeyHashCode: CompositeKeyHashCode
        // The runtime uses Long on JVM, JS, and Wasm, but exposes an expect class to common code.
        get() = (if (depth == 0) 0L else hashes[depth - 1]).unsafeCast<CompositeKeyHashCode>()

    /** Resets mutable state for reuse across keyed renders. Arrays retain their grown capacity. */
    fun reset() {
        depth = 0
        children[0] = 0
        locals = emptyCompositionLocalMap
        providers.clear()
        observers.clear()
        effects.clear()
        rememberedCount = 0
    }

    override fun startReplaceableGroup(key: Int) = enter(key)
    override fun endReplaceableGroup() = leave()
    override fun startReplaceGroup(key: Int) = enter(key)
    override fun endReplaceGroup() = leave()
    override fun startRestartGroup(key: Int): Composer {
        enter(key)
        return this
    }

    override fun endRestartGroup(): ScopeUpdateScope? {
        leave()
        return null
    }

    override fun startMovableGroup(key: Int, dataKey: Any?) =
        enter(if (dataKey is Enum<*>) dataKey.ordinal else dataKey?.hashCode() ?: key, dataKey != null)
    override fun endMovableGroup() = leave()
    override fun startReusableGroup(key: Int, dataKey: Any?) =
        enter(if (key == reuseKey && dataKey != null && dataKey !== Composer.Empty) dataKey.hashCode() else key)
    override fun endReusableGroup() = leave()
    override fun startDefaults() = enter(-127)
    override fun endDefaults() = leave()
    override fun disableReusing() {}
    override fun enableReusing() {}
    override fun deactivateToEndGroup(changed: Boolean) {}
    override fun skipToGroupEnd() = unsupported("skipping")
    override fun skipCurrentGroup() = unsupported("skipping")
    override fun joinKey(left: Any?, right: Any?): Any = JoinedKey(left, right)

    override fun rememberedValue(): Any = Composer.Empty
    override fun changed(value: Any?) = true
    override fun changedInstance(value: Any?) = true
    override fun changed(value: Boolean) = true
    override fun changed(value: Char) = true
    override fun changed(value: Byte) = true
    override fun changed(value: Short) = true
    override fun changed(value: Int) = true
    override fun changed(value: Float) = true
    override fun changed(value: Long) = true
    override fun changed(value: Double) = true

    override val currentCompositionLocalMap: CompositionLocalMap get() = locals
    override fun <T> consume(key: CompositionLocal<T>): T = locals.read(key)
    override fun startProviders(values: Array<out ProvidedValue<*>>) {
        providers.add(locals)
        locals = locals.builder().apply { putAll(updateCompositionMap(values, locals)) }.build()
    }
    override fun endProviders() {
        locals = providers.removeAt(providers.lastIndex)
    }
    override fun startProvider(value: ProvidedValue<*>) = startProviders(arrayOf(value))
    override fun endProvider() = endProviders()

    override fun updateRememberedValue(value: Any?) {
        if (value is RememberObserver) observers.add(value)
    }
    override fun recordSideEffect(effect: () -> Unit) {
        effects.add(effect)
    }

    fun applyEffects() {
        check(depth == 0 && providers.isEmpty()) { "Unbalanced single-pass composition" }
        while (rememberedCount < observers.size) {
            observers[rememberedCount++].onRemembered()
        }
        for (effect in effects) effect()
    }

    fun dispose(originalFailure: Throwable?) {
        var failure = originalFailure
        fun cleanup(block: () -> Unit) {
            try {
                block()
            } catch (cause: Throwable) {
                if (failure == null) {
                    failure = cause
                } else if (failure !== cause) {
                    failure!!.addSuppressed(cause)
                }
            }
        }
        try {
            val remembered = if (rememberedCount == observers.size) {
                emptySet()
            } else {
                HashSet<RememberObserverIdentity>().apply {
                    for (i in 0 until rememberedCount) {
                        add(RememberObserverIdentity(observers[i]))
                    }
                }
            }
            // Once onRemembered is reached (even if it throws), forget every occurrence in reverse location order.
            // Include occurrences after the failed notification, and never abandon that instance.
            for (i in observers.lastIndex downTo 0) {
                val observer = observers[i]
                if (i < rememberedCount || RememberObserverIdentity(observer) in remembered) {
                    cleanup { observer.onForgotten() }
                }
            }
            val abandoned = HashSet<RememberObserverIdentity>()
            for (i in rememberedCount until observers.size) {
                val observer = observers[i]
                val identity = RememberObserverIdentity(observer)
                if (identity !in remembered && abandoned.add(identity)) {
                    cleanup { observer.onAbandoned() }
                }
            }
            if (originalFailure == null) failure?.let { throw it }
        } finally {
            // Retain storage capacity, but release request objects even when cleanup throws.
            reset()
        }
    }
    override val applyCoroutineContext get() = cancelledEffectContext

    override fun recordUsed(scope: RecomposeScope) {}
    override fun sourceInformation(sourceInformation: String) {}
    override fun sourceInformationMarkerStart(key: Int, sourceInformation: String) {}
    override fun sourceInformationMarkerEnd() {}
    override fun collectParameterInformation() {}
    override fun disableSourceInformation() {}
    override val compositionData: CompositionData get() = EmptyCompositionData
    override fun scheduleFrameEndCallback(action: () -> Unit) = CancellationHandle {}

    override val applier: Applier<*> get() = unsupported("custom Compose nodes")
    override fun startNode(): Unit = unsupported("custom Compose nodes")
    override fun startReusableNode(): Unit = unsupported("custom Compose nodes")
    override fun <T> createNode(factory: () -> T): Unit = unsupported("custom Compose nodes")
    override fun useNode(): Unit = unsupported("custom Compose nodes")
    override fun endNode(): Unit = unsupported("custom Compose nodes")
    override fun <V, T> apply(value: V, block: T.(V) -> Unit): Unit = unsupported("custom Compose nodes")
    override fun insertMovableContent(value: MovableContent<*>, parameter: Any?) {
        // SSR has no previous state to move. Render at the invocation site with its current locals.
        startMovableGroup(movableContentKey, value)
        hashes[depth - 1] = movableContentKey.toLong()
        try {
            @Suppress("UNCHECKED_CAST")
            val content: @Composable () -> Unit = { (value as MovableContent<Any?>).content(parameter) }
            @Suppress("UNCHECKED_CAST")
            (content as (Composer, Int) -> Unit)(this, 1)
        } finally {
            endMovableGroup()
        }
    }
    override fun insertMovableContentReferences(references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>): Unit = unsupported("movable content state transfer")
    override fun buildContext(): CompositionContext = unsupported("subcomposition")
    override val composition: ControlledComposition get() = unsupported("controlled composition access")
    private fun unsupported(feature: String): Nothing = throw UnsupportedOperationException(
        "Single-pass HTML rendering does not support $feature"
    )
}

private fun Long.rotateLeft(bitCount: Int): Long =
    (this shl bitCount) or (this ushr (Long.SIZE_BITS - bitCount))

private class RememberObserverIdentity(private val observer: RememberObserver) {
    override fun equals(other: Any?): Boolean =
        other is RememberObserverIdentity && observer === other.observer

    override fun hashCode(): Int = androidx.compose.runtime.internal.identityHashCode(observer)
}

private val emptyCompositionLocalMap = CompositionLocalMap.Empty.unsafeCast<PersistentCompositionLocalMap>()
private val cancelledEffectContext by lazy { Dispatchers.Unconfined + Job().apply { cancel() } }
private object EmptyCompositionData : CompositionData {
    override val compositionGroups: Iterable<CompositionGroup> get() = emptyList()
    override val isEmpty get() = true
}
