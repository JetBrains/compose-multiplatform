package com.google.r4a

@Composable
fun Observe(@Children body: @Composable() () -> Unit) =
    composer.composer.let { composer ->
        composer.startGroup(observer)
        composer.startJoin(false) { body() }
        body()
        composer.doneJoin(false)
        composer.endGroup()
    }

private val observer = Object()