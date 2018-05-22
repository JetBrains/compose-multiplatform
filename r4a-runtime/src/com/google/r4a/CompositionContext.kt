package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup
import java.util.*

abstract class CompositionContext {
    companion object {

        private val TAG_ROOT_COMPONENT = "r4aRootComponent".hashCode()
        private val COMPONENTS_TO_CONTEXT = WeakHashMap<Component, CompositionContext>()

        var factory: Function3<Context, ViewGroup, Component, CompositionContext> = CompositionContextImpl.factory
        var current: CompositionContext = CompositionContextImpl()

        fun create(context: Context, view: ViewGroup, component: Component): CompositionContext {
            val cc = factory(context, view, component)
            setRoot(view, component)
            return cc
        }

        fun find(component: Component): CompositionContext? {
            return COMPONENTS_TO_CONTEXT[component]
        }

        fun associate(component: Component, context: CompositionContext) {
            COMPONENTS_TO_CONTEXT[component] = context
        }

        fun recompose(component: Component) {
            find(component)?.recompose(component)
        }

        fun find(view: View): Component? {
            var node: View? = view
            while (node != null) {
                val cc = node.getTag(TAG_ROOT_COMPONENT) as? Component
                if (cc != null) return cc
                node = node.parent as? View
            }
            return null
        }

        fun getRootComponent(view: View): Component? {
            return view.getTag(TAG_ROOT_COMPONENT) as? Component
        }

        fun setRoot(view: View, component: Component) {
            view.setTag(TAG_ROOT_COMPONENT, component)
        }
    }
    abstract var context: Context
    abstract fun recompose(component: Component)
    abstract fun start(sourceHash: Int): Any?
    abstract fun start(sourceHash: Int, key: Any?): Any?
    abstract fun end()
    abstract fun setInstance(instance: Any)
    abstract fun updateAttribute(value: Any?): Boolean
    abstract fun compose()
    abstract fun debug()
}