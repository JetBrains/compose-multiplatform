package com.google.r4a

/**
 * This component creates a scope which will be the root of recomposition for any reads or writes to
 * [Model] classes that happen inside of it. This can be used to improve performance in situations
 * where you know that a specific [Model] object will need to change at high frequencies, and you
 * want to reduce the burden of recomposition.  It is recommended that you not introduce [Observe]
 * into the composition until it is clear that is necessary to improve performance.
 *
 * @param body The composable content to observe
 *
 * @see Model
 * @see invalidate
 * @see Recompose
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Observe(@Children body: @Composable() () -> Unit) =
    currentComposerNonNull.let { composer ->
        composer.startGroup(observer)
        composer.startJoin(false) { body() }
        body()
        composer.doneJoin(false)
        composer.endGroup()
    }

private val observer = Object()