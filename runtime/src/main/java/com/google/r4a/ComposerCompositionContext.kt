package com.google.r4a

import android.content.Context
import android.view.Choreographer

internal class ComposerCompositionContext(
    val root: Any,
    private val rootComponent: Component
) : CompositionContext(), Recomposer {
    companion object {
        val factory: Function4<Context, Any, Component, CompositionReference?, CompositionContext>
                by lazy {
                    object : Function4<
                            Context,
                            Any,
                            Component,
                            CompositionReference?,
                            CompositionContext
                            > {
                        override fun invoke(
                            context: Context,
                            root: Any,
                            component: Component,
                            ambientReference: CompositionReference?
                        ): CompositionContext {
                            val result = ComposerCompositionContext(root, component)
                            result.context = context
                            result.composer.ambientReference = ambientReference
                            ambientReference?.registerComposer(result.composer)
                            return result
                        }
                    }
                }
    }

    internal val composer by lazy { ViewComposer(root, context, this) }

    private var hasPendingFrame = false
    private var isComposing = false

    private val frameCallback = Choreographer.FrameCallback {
        hasPendingFrame = false
        recomposePending()
    }

    private val postRecomposeObservers = mutableListOf<() -> Unit>()

    private fun dispatchRecomposeObservers() {
        val listeners = postRecomposeObservers.toTypedArray()
        postRecomposeObservers.clear()
        listeners.forEach { it() }
    }

    private fun recomposePending() {
        if (isComposing) return
        val prev = CompositionContext.current
        try {
            isComposing = true
            CompositionContext.current = this
            composer.recompose()
            composer.applyChanges()
            dispatchRecomposeObservers()
        } finally {
            CompositionContext.current = prev
            isComposing = false
        }
    }

    override lateinit var context: Context

    override fun scheduleRecompose() {
        // if we're not currently composing and a frame hasn't been scheduled, we want to schedule it
        if (!isComposing && !hasPendingFrame) {
            hasPendingFrame = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun recomposeSync() {
        if (!isComposing) {
            hasPendingFrame = false
            recomposePending()
        }
    }

    override fun recompose(component: Component) {
        component.recomposeCallback?.let { it(false) }
    }

    override fun recomposeSync(component: Component) {
        if (component == rootComponent) {
            val previousComposing = isComposing
            val prev = CompositionContext.current
            try {
                isComposing = true
                CompositionContext.current = this
                val composer = composer
                composer.startRoot()
                composer.startGroup(invocation)
                component()
                composer.endGroup()
                composer.endRoot()
                composer.applyChanges()
            } finally {
                CompositionContext.current = prev
                isComposing = previousComposing
            }
        } else {
            component.recomposeCallback?.let { it(true) }
        }
    }

    override fun <T> getAmbient(key: Ambient<T>): T {
        return composer.parentAmbient(key)
    }

    override fun addPostRecomposeObserver(l: () -> Unit) {
        postRecomposeObservers.add(l)
    }

    override fun removePostRecomposeObserver(l: () -> Unit) {
        postRecomposeObservers.remove(l)
    }
}
