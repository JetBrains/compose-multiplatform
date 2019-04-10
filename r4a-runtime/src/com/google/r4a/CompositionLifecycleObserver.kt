package com.google.r4a

/**
 * Instances of classes implementing this interface are notified when they are initially
 * used during composition and when they are no longer being used.
 */
interface CompositionLifecycleObserver {
    /**
     * Called when the instance is used in composition.
     */
    fun onEnter()

    /**
     * Called when the instance is no longer used in the composition.
     */
    fun onLeave()
}

/**
 * Holds an instance of a CompositionLifecycleObserver and a count of how many times it is
 * used during composition.
 *
 * The holder can be used as a key for the identity of the instance.
 */
internal class CompositionLifecycleObserverHolder(val instance: CompositionLifecycleObserver) {
    var count: Int = 0
    override fun equals(other: Any?): Boolean = other === instance || other is CompositionLifecycleObserverHolder && instance === other.instance
    override fun hashCode(): Int = System.identityHashCode(instance)
}
