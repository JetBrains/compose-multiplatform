package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup
import java.util.*

abstract class CompositionContext {
    companion object {

        private val TAG_COMPOSITION_CONTEXT = "r4aCompositionContext".hashCode()
        private val COMPONENTS_TO_CONTEXT = WeakHashMap<Component, CompositionContext>()

        var factory: Function3<Context, ViewGroup, Component, CompositionContext> = CompositionContextImpl.factory
        var current: CompositionContext = CompositionContextImpl()

        fun create(context: Context, view: ViewGroup, component: Component): CompositionContext {
            return factory(context, view, component)
        }

        fun find(component: Component): CompositionContext? {
            return COMPONENTS_TO_CONTEXT[component]
        }

        fun associate(component: Component, context: CompositionContext) {
            COMPONENTS_TO_CONTEXT[component] = context
        }

        fun find(view: View): CompositionContext? {
            var node: View? = view
            while (node != null) {
                val cc = node.getTag(TAG_COMPOSITION_CONTEXT) as? CompositionContext
                if (cc != null) return cc
                node = node.parent as? View
            }
            return null
        }

        fun setRoot(view: View, context: CompositionContext) {
            view.setTag(TAG_COMPOSITION_CONTEXT, context)
        }
    }
    abstract var context: Context
    abstract fun recomposeFromRoot()
    abstract fun start(sourceHash: Int): Any?
    abstract fun start(sourceHash: Int, key: Any?): Any?
    abstract fun end()
    abstract fun setInstance(instance: Any)
    abstract fun updAttr(key: String, value: Any?): Boolean
    abstract fun compose()
    abstract fun debug()
}