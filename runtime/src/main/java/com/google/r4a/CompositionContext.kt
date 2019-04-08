package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap

abstract class CompositionContext {
    companion object {

        private val TAG_ROOT_COMPONENT = "r4aRootComponent".hashCode()
        private val EMITTABLE_ROOT_COMPONENT = WeakHashMap<Emittable, Component>()
        private val COMPONENTS_TO_CONTEXT = WeakHashMap<Component, CompositionContext>()

        val factory: Function4<Context, Any, Component, Ambient.Reference?, CompositionContext>
            get() = ComposerCompositionContext.factory

        var current: CompositionContext = EmptyCompositionContext()

        fun create(
            context: Context,
            group: Any,
            component: Component,
            reference: Ambient.Reference?
        ): CompositionContext {
            val cc = factory(context, group, component, reference)
            when (group) {
                is View -> setRoot(group, component)
                is Emittable -> setRoot(group, component)
            }
            return cc
        }

        fun find(component: Component): CompositionContext? {
            return COMPONENTS_TO_CONTEXT[component]
        }

        fun associate(component: Component, context: CompositionContext) {
            COMPONENTS_TO_CONTEXT[component] = context
        }

        fun recompose(component: Component) {
            component.recomposeCallback?.invoke(false)
        }

        fun recomposeSync(component: Component) {
            component.recomposeCallback?.invoke(true)
        }

        fun findRoot(view: View): Component? {
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

        fun disposeComposition(container: ViewGroup, parent: Ambient.Reference? = null) {
            container.setTag(TAG_ROOT_COMPONENT, null)
        }

        fun disposeComposition(container: Emittable, parent: Ambient.Reference? = null) {
            val context = EMITTABLE_ROOT_COMPONENT[container]
            // TODO(lmr): clear the ambient reference?
            EMITTABLE_ROOT_COMPONENT.remove(container)
//            parent?.registerComposer()
        }

        fun getRootComponent(emittable: Emittable): Component? {
            return EMITTABLE_ROOT_COMPONENT[emittable]
        }

        fun setRoot(view: View, component: Component) {
            view.setTag(TAG_ROOT_COMPONENT, component)
        }

        fun setRoot(emittable: Emittable, component: Component) {
            EMITTABLE_ROOT_COMPONENT[emittable] = component
        }
    }

    abstract var context: Context
    abstract fun recompose(component: Component)
    abstract fun scheduleRecompose()
    abstract fun recomposeSync(component: Component)
    abstract fun <T : Any?> getAmbient(key: Ambient<T>): T

    internal abstract fun addPostRecomposeObserver(l: () -> Unit)
    internal abstract fun removePostRecomposeObserver(l: () -> Unit)
}

inline fun ViewComposition.group(key: Int, block: () -> Unit) {
    try {
        composer.startGroup(key)
        block()
    } finally {
        composer.endGroup()
    }
}

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    ctor: () -> T,
    noinline block: ViewValidator.(f: T) -> Boolean
): Unit = emitComponent(loc, null, ctor, block)

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    ctor: () -> T
): Unit = emitComponent(loc, null, ctor, { true })

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T
): Unit = emitComponent(loc, key, ctor, { true })

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T,
    noinline block: ViewValidator.(f: T) -> Boolean
): Unit = call(
    joinKey(loc, key),
    ctor,
    block,
    { f -> f() }
)

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
): Unit = emitView(loc, null, ctor, updater)

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    ctor: (context: Context) -> T
): Unit = emitView(loc, null, ctor, {})

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T
): Unit = emitView(loc, key, ctor, {})

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
): Unit = emit(
    joinKey(loc, key),
    ctor,
    updater
)

inline fun <reified T : ViewGroup> ViewComposition.emitViewGroup(
    loc: Int,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emitViewGroup(loc, null, ctor, updater, block)

inline fun <reified T : ViewGroup> ViewComposition.emitViewGroup(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: @Composable() () -> Unit
) = emit(
    joinKey(loc, key),
    ctor,
    updater,
    block
)

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
) = emitEmittable(loc, null, ctor, updater, {})

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emitEmittable(loc, null, ctor, updater, block)

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    key: Int?,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emit(
    joinKey(loc, key),
    ctor,
    updater,
    block
)

inline fun <reified T> ViewComposition.provideAmbient(
    key: Ambient<T>,
    value: T,
    noinline children: @Composable() () -> Unit
) = emitComponent(
    0,
    { key.Provider(value, children) },
    { provider -> update(value) { provider.value = it } or update(children) {
        provider.children = it
    } }
)

inline fun <reified T> ViewComposition.consumeAmbient(
    key: Ambient<T>,
    noinline children: @Composable() (T) -> Unit
) = emitComponent(
    0,
    { key.Consumer(children) },
    { consumer -> update(children) { consumer.children = it } }
)

@Suppress("NOTHING_TO_INLINE")
inline fun ViewComposition.portal(
    location: Int,
    noinline children: @Composable() (Ambient.Reference) -> Unit
) = emitComponent(
    location,
    { Ambient.Portal(children) },
    { portal -> update(children) { portal.children = it } }
)
