package com.google.r4a

@Stateful
@Suppress("PLUGIN_ERROR")
abstract class Component {
    @HiddenAttribute
    internal var recomposeCallback: ((sync: Boolean) -> Unit)? = null
    private var composing = false

    protected fun recompose() {
        if (composing) return
        recomposeCallback?.invoke(false)
    }

    protected fun recomposeSync() {
        if (composing) return
        recomposeCallback?.invoke(true)
    }

    @Composable
    abstract fun compose()

    @Suppress("PLUGIN_ERROR")
    private fun doCompose() {
        try {
            composing = true
            compose()
        } finally {
            composing = false
        }
    }

    @Composable
    operator fun invoke() {
        val composer = currentComposerNonNull
        val callback = composer.startJoin(false) { doCompose() }
        doCompose()
        composer.doneJoin(false)
        recomposeCallback = callback
    }
}
