package com.google.r4a

/**
 * This is a special function which kotlin will inline into a number representing the source location of the caller. At the moment,
 * this is just hardcoded to a constant, but we will implement the source location inlining in the future. Until this happens, effects
 * will be UNSAFE to use inside of conditional logic.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun sourceLocation(): Int = 0