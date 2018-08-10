/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import android.content.Context
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup

private class ViewApplyAdapter : ApplyAdapter<View> {
    override fun View.insertAt(index: Int, instance: View) {
        (this as ViewGroup).addView(instance, index)
    }

    override fun View.removeAt(index: Int, count: Int) {
        (this as ViewGroup).removeViews(index, count)
    }

    override fun View.move(from: Int, to: Int, count: Int) {
        with(this as ViewGroup) {
            if (from > to) {
                var current = to
                repeat(count) {
                    val view = getChildAt(from)
                    removeViewAt(from)
                    addView(view, current)
                    current++
                }
            } else {
                repeat(count) {
                    val view = getChildAt(from)
                    removeViewAt(from)
                    addView(view, to - 1)
                }
            }
        }
    }
}

private class ViewComposer(val root: ViewGroup) : Composer<View>(SlotTable(), Applier(root, ViewApplyAdapter()))

internal class ComposerCompositionContext(val root: ViewGroup, private val rootComponent: Component) : CompositionContext() {
    companion object {
        val factory = object: Function3<Context, ViewGroup, Component, CompositionContext> {
            override fun invoke(context: Context, root: ViewGroup, component: Component): CompositionContext {
                val result = ComposerCompositionContext(root, component)
                result.context = context
                return result
            }
        }
    }

    private val composer = ViewComposer(root)
    private var currentComponent: Component? = null

    private var hasPendingFrame = false
    private var isComposing = false

    private val frameCallback = Choreographer.FrameCallback {
        hasPendingFrame = false
        recomposePending()
    }

    private fun recomposePending() {
        if (isComposing) return
        val prev = CompositionContext.current
        try {
            isComposing = true
            CompositionContext.current = this
            composer.recompose()
            composer.applyChanges()
        } finally {
            CompositionContext.current = prev
            isComposing = false
        }
    }

    override var context: Context = root.context

    override fun startRoot() {
        composer.slots.reset()
        composer.slots.beginReading()
        composer.startGroup(0)
    }

    override fun start(sourceHash: Int) = composer.startGroup(sourceHash)
    override fun start(sourceHash: Int, key: Any?) = composer.startGroup(if (key != null) composer.joinKey(sourceHash, key) else sourceHash)
    override fun startView(sourceHash: Int) = composer.startNode(sourceHash)
    override fun startView(sourceHash: Int, key: Any?) =
        composer.startNode(if (key != null) composer.joinKey(sourceHash, key) else sourceHash)

    override fun end() = composer.endGroup()
    override fun endView() = composer.endNode()

    override fun endRoot() {
        composer.endGroup()
        composer.slots.endReading()
        composer.finalRealizeSlots()
    }

    override fun joinKey(left: Any?, right: Any?): Any = composer.joinKey(left, right)

    override fun applyChanges() = composer.applyChanges()

    override fun setInstance(instance: Any) {
        assert(currentComponent == null) { "Unhandled recursion" }
        when (instance) {
            is View -> composer.emitNode(instance)
            is Component -> {
                currentComponent = instance
                composer.updateValue(instance)
                CompositionContext.associate(instance, this)
            }
            else -> error("Unknown instance type ${instance.javaClass}")
        }
    }

    override fun useInstance(): Any? {
        val instance = composer.peekSlot()
        return when (instance) {
            is View -> composer.useNode()
            is Component -> {
                composer.skipValue()
                composer.nextSlot()
                currentComponent = instance
                instance
            }
            else -> error("Unknown instance type ${instance}")
        }
    }

    override fun isInserting(): Boolean = composer.inserting

    override fun startCompose(willCompose: Boolean) {
        val component = currentComponent
        if (component != null) {
            composer.startCompose(!willCompose, component)
        }
        currentComponent = null
    }

    override fun endCompose(didCompose: Boolean) = composer.doneCompose(!didCompose)

    override fun attributeChanged(value: Any?) =
        if (composer.nextSlot() == value) {
            composer.skipValue()
            false
        } else {
            composer.updateValue(value)
            true
        }

    override fun attributeChangedOrInserting(value: Any?): Boolean = attributeChanged(value) || composer.inserting

    override fun recompose(component: Component) {
        component.recomposeCallback?.let { it() }

        // if we're not currently composing and a frame hasn't been scheduled, we want to schedule it
        if (!isComposing && !hasPendingFrame) {
            hasPendingFrame = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun recomposeSync(component: Component) {
        if (component == rootComponent) {
            val previousComposing = isComposing
            val prev = CompositionContext.current
            try {
                isComposing = true
                CompositionContext.current = this
                startRoot()
                setInstance(component)
                startCompose(true)
                component.compose()
                endCompose(true)
                endRoot()
                applyChanges()
            } finally {
                CompositionContext.current = prev
                isComposing = previousComposing
            }
        } else {
            component.recomposeCallback?.let { it() }
            if (!isComposing) {
                hasPendingFrame = false
                recomposePending()
            }
        }
    }

    override fun <T> getAmbient(key: Ambient<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> getAmbient(key: Ambient<T>, component: Component): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun debug() {}
}
