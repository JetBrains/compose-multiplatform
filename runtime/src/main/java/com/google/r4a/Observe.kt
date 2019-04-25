package com.google.r4a

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