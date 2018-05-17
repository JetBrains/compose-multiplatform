package com.google.r4a

@Composable
abstract class Component {
    abstract fun compose()
    protected fun recompose() {
        CompositionContext.recompose(this)
    }
}
