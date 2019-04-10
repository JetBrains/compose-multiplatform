package com.google.r4a

/**
 * Effects are positionally memoized which means that the "resolving" of them depends on execution order and the fact that the
 * resolve happens inside of composition. As a result, we want to use a DslMarker to try and prevent common mistakes of people
 * trying to resolve effects outside of composition.
 *
 * For example, the following should be illegal:
 *
 *     +onCommit {
 *       val x = +state { 123 }
 *     }
 *
 * The `+state` call is illegal because the onCommit callback does not execute during composition, but it would compile without
 * any error if @EffectsDsl wasn't used.
 */
@DslMarker
annotation class EffectsDsl
