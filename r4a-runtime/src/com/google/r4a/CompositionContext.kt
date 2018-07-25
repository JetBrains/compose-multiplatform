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

        fun recomposeSync(component: Component) {
            find(component)?.recomposeSync(component)
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

        fun <T : Any?> getAmbient(key: Ambient<T>, component: Component): T = find(component)!!.getAmbient(key)
    }

    abstract fun start(sourceHash: Int)
    abstract fun start(sourceHash: Int, key: Any?)
    abstract fun setInstance(instance: Any)
    abstract fun useInstance(): Any?
    abstract fun isInserting(): Boolean
    abstract fun willCompose()
    abstract fun attributeChanged(value: Any?): Boolean
    abstract fun attributeChangedOrInserting(value: Any?): Boolean
    abstract fun end()


    abstract var context: Context
    abstract fun recompose(component: Component)
    abstract fun recomposeSync(component: Component)
    abstract fun <T : Any?> getAmbient(key: Ambient<T>): T
    abstract fun <T : Any?> getAmbient(key: Ambient<T>, component: Component): T
    abstract fun debug()
}


class Updater<T>(
    val cc: CompositionContext,
    val el: T
) {
    inline fun <reified V> set(value: V, noinline block: T.(V) -> Unit) {
        if (cc.attributeChangedOrInserting(value)) {
            el.block(value)
        }
    }
}

inline fun CompositionContext.group(key: Int, key2: Any? = null, block: () -> Unit = {}) {
    start(key, key2)
    block()
    end()
}

inline fun <reified T : Component> CompositionContext.emitComponent(
    loc: Int,
    ctor: () -> T,
    block: Updater<T>.() -> Unit
) = emitComponent(loc, null, ctor, block)

inline fun <reified T : Component> CompositionContext.emitComponent(
    loc: Int,
    ctor: () -> T
) = emitComponent(loc, null, ctor, {})

inline fun <reified T : Component> CompositionContext.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T
) = emitComponent(loc, key, ctor, {})

inline fun <reified T : Component> CompositionContext.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T,
    block: Updater<T>.() -> Unit
) = group(loc, key) {
    val el: T
    if (isInserting()) {
        el = ctor()
        setInstance(el)
    } else {
        el = useInstance() as T
    }
    Updater(this, el).block()
    // TODO(lmr): do pruning
    willCompose()
    el.compose()
}


inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    ctor: (context: Context) -> T,
    updater: Updater<T>.() -> Unit,
    block: () -> Unit
) = emitView(loc, null, ctor, updater, block)

inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    ctor: (context: Context) -> T,
    updater: Updater<T>.() -> Unit
) = emitView(loc, null, ctor, updater, {})

inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    ctor: (context: Context) -> T
) = emitView(loc, null, ctor, {}, {})

inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    updater: Updater<T>.() -> Unit
) = emitView(loc, key, ctor, updater, {})

inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T
) = emitView(loc, key, ctor, {}, {})

inline fun <reified T : View> CompositionContext.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    updater: Updater<T>.() -> Unit,
    block: () -> Unit
) = group(loc, key) {
    val el: T
    if (isInserting()) {
        el = ctor(context)
        setInstance(el)
    } else {
        el = useInstance() as T
    }
    Updater(this, el).updater()
    block()
}

inline fun <reified T> CompositionContext.provideAmbient(
    key: Ambient<T>,
    value: T,
    noinline children: () -> Unit
) = group(0) {
    val el: Ambient<T>.Provider
    if (isInserting()) {
        el = key.Provider(value, children)
        setInstance(el)
    } else {
        el = useInstance() as Ambient<T>.Provider
    }
    if (attributeChanged(value)) {
        el.value = value
    }
    if (attributeChanged(children)) {
        el.children = children
    }
    el.compose()
}

inline fun <reified T> CompositionContext.consumeAmbient(
    key: Ambient<T>,
    noinline children: (T) -> Unit
) = group(0) {
    val el: Ambient<T>.Consumer
    if (isInserting()) {
        el = key.Consumer()
        setInstance(el)
    } else {
        el = useInstance() as Ambient<T>.Consumer
    }
    if (attributeChangedOrInserting(children)) {
        el.children = children
    }
    el.compose()
}
