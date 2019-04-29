package com.google.r4a

import android.content.Context
import android.view.Choreographer

// TODO(lmr): this is really only needed for "composition management", but that could maybe move
// somewhere else. Consider ways to remove this class. Maybe should merge with FrameManager?
class CompositionContext private constructor(
    val root: Any,
    private val rootComponent: Component,
    makeComposer: CompositionContext.() -> Composer<*>
) : Recomposer {
    companion object {
        fun create(
            context: Context,
            root: Any,
            component: Component,
            compositionReference: CompositionReference?
        ) = create(root, component, compositionReference) { ViewComposer(root, context, this) }

        fun create(
            root: Any,
            component: Component,
            ambientReference: CompositionReference?,
            makeComposer: CompositionContext.() -> Composer<*>
        ): CompositionContext {
            val result = CompositionContext(root, component, makeComposer)
            result.composer.ambientReference = ambientReference
            ambientReference?.registerComposer(result.composer)
            return result
        }
    }

    internal val composer: Composer<*> = makeComposer()

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
        runWithCurrent {
            try {
                isComposing = true
                composer.recompose()
                composer.applyChanges()
                dispatchRecomposeObservers()
                FrameManager.nextFrame()
            } finally {
                isComposing = false
            }
        }
    }

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

    fun recompose() {
        runWithCurrent {
            val previousComposing = isComposing
            try {
                isComposing = true
                val composer = composer
                composer.startRoot()
                composer.startGroup(invocation)
                rootComponent()
                composer.endGroup()
                composer.endRoot()
                composer.applyChanges()
                FrameManager.nextFrame()
            } finally {
                isComposing = previousComposing
            }
        }
    }

    fun addPostRecomposeObserver(l: () -> Unit) {
        postRecomposeObservers.add(l)
    }

    fun removePostRecomposeObserver(l: () -> Unit) {
        postRecomposeObservers.remove(l)
    }
}

internal var currentCompositionContext: CompositionContext? = null
    private set

fun <T> CompositionContext.runWithCurrent(block: () -> T): T {
    val current = currentCompositionContext
    try {
        currentCompositionContext = this
        return block()
    } finally {
        currentCompositionContext = current
    }
}