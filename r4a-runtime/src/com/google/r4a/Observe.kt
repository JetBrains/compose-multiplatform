package com.google.r4a

@Composable
fun Observe(@Children body: @Composable() () -> Unit) =
    (CompositionContext.current as? ComposerCompositionContext)?.composer?.let { composer ->
        composer.startGroup(observer)
        composer.startJoin(false) { body() }
        body()
        composer.doneJoin(false)
        composer.endGroup()
    } ?: body()

private val observer = object {}