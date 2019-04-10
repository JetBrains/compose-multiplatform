package com.google.r4a

@Stateful
abstract class Component : Recomposable {
    @HiddenAttribute
    internal var recomposeCallback: ((sync: Boolean) -> Unit)? = null
    private lateinit var compositionContext: ComposerCompositionContext
    private var composing = false

    protected fun recompose() {
        if (composing) return
        recomposeCallback?.invoke(false)
    }

    protected fun recomposeSync() {
        if (composing) return
        recomposeCallback?.invoke(true)
    }

    override fun setRecompose(recompose: (sync: Boolean) -> Unit) {
        this.recomposeCallback = recompose
    }

    @Composable
    abstract fun compose()

    private fun doCompose() {
        try {
            composing = true
            compose()
        } finally {
            composing = false
        }
    }

    @Composable
    override operator fun invoke() {
        compositionContext = CompositionContext.current as ComposerCompositionContext
        val composer = compositionContext.composer
        val callback = composer.startJoin(false) { doCompose() }
        doCompose()
        composer.doneJoin(false)
        setRecompose(callback)
    }
}
