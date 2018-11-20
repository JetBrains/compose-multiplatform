package com.google.r4a

@Composable
abstract class Component : Recomposable {
    internal var recomposeCallback: (() -> Unit)? = null

    protected fun recompose() {
        CompositionContext.recompose(this)
    }

    protected fun recomposeSync() {
        CompositionContext.recomposeSync(this)
    }

    override fun setRecompose(recompose: () -> Unit) {
        this.recomposeCallback = recompose
    }
}
