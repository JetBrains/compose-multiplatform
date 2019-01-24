package com.google.r4a

/**
 * The Emittable interface tells R4A that the implementing class represents a
 * primitive node/type in the view hierarchy produced as a result of composition.
 * Conceptually similar to a RenderObject in flutter.  The result of composition is
 * an updated tree of Emittables, which R4A will maintain/mutate over time as
 * subsequent reconciliations are calculated.
 * To learn more: https://goto.google.com/r4a-emittable
 */
interface Emittable {
    fun emitInsertAt(index: Int, instance: Emittable)
    fun emitRemoveAt(index: Int, count: Int)
    fun emitMove(from: Int, to: Int, count: Int)
}