package com.google.r4a

interface Recomposable {
    fun setRecompose(recompose: () -> Unit)
    fun compose()
}

abstract class RecomposableContext {
    internal abstract fun enumParents(callback: (Recomposable) -> Boolean)
    internal abstract fun enumChildren(callback: (Recomposable) -> Boolean)
}

abstract class Composition<N> : RecomposableContext() {
    abstract val inserting: Boolean

    abstract fun startGroup(key: Any)
    abstract fun endGroup()
    abstract fun skipGroup()

    abstract fun startNode(key: Any)
    abstract fun <T : N> emitNode(factory: () -> T)
    abstract fun emitNode(node: N) // Deprecated - single threaded
    abstract fun useNode(): N // Deprecated - single threaded
    abstract fun endNode()

    abstract fun startCompose(valid: Boolean, recomposable: Recomposable)
    abstract fun doneCompose(valid: Boolean)

    abstract fun joinKey(left: Any?, right: Any?): Any

    abstract fun nextSlot(): Any?
    abstract fun peekSlot(): Any? // Deprecated - old runtime adapter

    abstract fun skipValue()
    abstract fun updateValue(value: Any?)
    abstract fun <V, T> apply(value: V, block: T.(V) -> Unit)
}

inline fun <N, T> Composition<N>.cache(valid: Boolean = true, block: () -> T): T {
    var result = nextSlot()
    if (result === SlotTable.EMPTY || !valid) {
        val value = block()
        updateValue(value)
        result = value
    } else skipValue()

    @Suppress("UNCHECKED_CAST")
    return result as T
}

inline fun <N, reified V> Composition<N>.changed(value: V): Boolean {
    return if (value != nextSlot()) {
        updateValue(value)
        true
    } else {
        skipValue()
        false
    }
}

inline fun <N, reified V> Composition<N>.applyNeeded(value: V): Boolean = changed(value) && !inserting

inline fun <N, V> Composition<N>.remember(block: () -> V): V = cache(true, block)

inline fun <N, V, reified P1> Composition<N>.remember(p1: P1, block: () -> V) = cache(!changed(p1), block)

inline fun <N, V, reified P1, reified P2> Composition<N>.remember(p1: P1, p2: P2, block: () -> V): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    return cache(valid, block)
}

inline fun <N, V, reified P1, reified P2, reified P3> Composition<N>.remember(p1: P1, p2: P2, p3: P3, block: () -> V): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    valid = !changed(p3) && valid
    return cache(valid, block)
}

inline fun <N, V, reified P1, reified P2, reified P3, reified P4> Composition<N>.remember(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    block: () -> V
): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    valid = !changed(p3) && valid
    valid = !changed(p4) && valid
    return cache(valid, block)
}

inline fun <N, V> Composition<N>.remember(vararg args: Any, block: () -> V): V {
    var valid = true
    for (arg in args) valid = !changed(arg) && valid
    return cache(valid, block)
}
