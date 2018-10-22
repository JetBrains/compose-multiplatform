package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.r4a.adapters.getViewAdapterIfExists
import java.util.*

class ViewAdapters {
    private val adapters = mutableListOf<(parent: Any, child: Any) -> Any?>()

    fun register(adapter: (parent: Any, child: Any) -> Any?) = adapters.add(adapter)
    fun adapt(parent: Any, child: Any): Any? = adapters.map { it(parent, child) }.filterNotNull().firstOrNull()
}

private fun invalidNode(node: Any): Nothing = error("Unsupported node type ${node.javaClass.simpleName}")

internal class ViewApplyAdapter(private val adapters: ViewAdapters? = null) : ApplyAdapter<Any> {
    private data class PendingInsert(val index: Int, val instance: Any)

    private val pendingInserts = Stack<PendingInsert>()

    override fun Any.start(instance: Any) {}
    override fun Any.insertAt(index: Int, instance: Any) {
        pendingInserts.push(PendingInsert(index, instance))
    }

    override fun Any.removeAt(index: Int, count: Int) {
        when (this) {
            is ViewGroup -> removeViews(index, count)
            is Emittable -> emitRemoveAt(index, count)
            else -> invalidNode(this)
        }
    }

    override fun Any.move(from: Int, to: Int, count: Int) {
        when (this) {
            is ViewGroup -> {
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
            is Emittable -> {
                emitMove(from, to, count)
            }
            else -> invalidNode(this)
        }
    }

    override fun Any.end(instance: Any, parent: Any) {
        val adapter = when (instance) {
            is View -> instance.getViewAdapterIfExists()
            else -> null
        }
        if (pendingInserts.isNotEmpty()) {
            val pendingInsert = pendingInserts.peek()
            if (pendingInsert.instance == instance) {
                val index = pendingInsert.index
                pendingInserts.pop()

                when (parent) {
                    is ViewGroup ->
                        when (instance) {
                            is View -> {
                                adapter?.willInsert(instance, parent)
                                parent.addView(instance, index)
                                adapter?.didInsert(instance, parent)
                            }
                            is Emittable -> {
                                val adaptedView = adapters?.adapt(parent, instance) as? View ?: error("Could not convert ${instance.javaClass.simpleName} to a View")
                                adapter?.willInsert(adaptedView, parent)
                                parent.addView(adaptedView, index)
                                adapter?.didInsert(adaptedView, parent)
                            }
                            else -> invalidNode(instance)

                        }
                    is Emittable ->
                        when (instance) {
                            is View -> parent.emitInsertAt(
                                index,
                                adapters?.adapt(parent, instance) as? Emittable ?: error("Could not convert ${instance.javaClass.name} to an Emittable")
                            )
                            is Emittable -> parent.emitInsertAt(index, instance)
                            else -> invalidNode(instance)
                        }
                    else -> invalidNode(parent)
                }
                return
            }
        }
        if (parent is ViewGroup)
            adapter?.didUpdate(instance as View, parent)
    }
}

class ViewComposer(val root: ViewGroup, val context: Context, val adapters: ViewAdapters? = ViewAdapters()) : Composer<Any>(SlotTable(), Applier(root, ViewApplyAdapter(adapters))) {
    fun skipGroup(key: Any) {
        nextSlot()
        skipValue()
        skipGroup()
    }
}

/* inline */ class ViewComposition(val composer: ViewComposer) {
    inline fun <T : View> emit(key: Any, crossinline ctor: (context: Context) -> T, update: ViewUpdater<T>.() -> Unit) {
        composer.startNode(key)
        if (composer.inserting)
            composer.createNode { ctor(composer.context) }
        else composer.useNode()
        ViewUpdater<T>(composer).update()
        composer.endNode()
    }

    inline fun <T : ViewGroup> emit(
        key: Any,
        crossinline ctor: (context: Context) -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        composer.startNode(key)
        if (composer.inserting)
            composer.createNode { ctor(composer.context) }
        else composer.useNode()
        ViewUpdater<T>(composer).update()
        children()
        composer.endNode()
    }

    inline fun <T : Emittable> emit(key: Any, crossinline ctor: () -> T, update: ViewUpdater<T>.() -> Unit) {
        composer.startNode(key)
        if (composer.inserting)
            composer.createNode { ctor() }
        else composer.useNode()
        ViewUpdater<T>(composer).update()
        composer.endNode()
    }

    inline fun <T : Emittable> emit(
        key: Any,
        crossinline ctor: () -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        composer.startNode(key)
        if (composer.inserting)
            composer.createNode { ctor() }
        else composer.useNode()
        ViewUpdater<T>(composer).update()
        children()
        composer.endNode()
    }

    inline fun joinKey(left: Any, right: Any?): Any = composer.joinKey(left, right)

    inline fun call(key: Any, crossinline invalid: ViewValidator.() -> Boolean, crossinline block: () -> Unit) {
        composer.startGroup(key)
        if (ViewValidator(composer).invalid() || composer.inserting) {
            composer.startGroup(invocation)
            block()
            composer.endGroup()
        } else {
            composer.skipGroup(invocation)
        }
        composer.endGroup()
    }

    inline fun <T> call(
        key: Any,
        crossinline ctor: () -> T,
        crossinline invalid: ViewValidator.(f: T) -> Boolean,
        crossinline block: (f: T) -> Unit
    ) {
        composer.startGroup(key)
        val f = composer.remember { ctor() }
        if (ViewValidator(composer).invalid(f) || composer.inserting) {
            composer.startGroup(invocation)
            block(f)
            composer.endGroup()
        } else {
            composer.skipGroup(invocation)
        }
        composer.endGroup()
    }
}

/* inline */ class ViewValidator(val composer: ViewComposer) {
    inline fun changed(value: Int) = if (composer.nextSlot() != value || composer.inserting) {
        composer.updateValue(value)
        true
    } else {
        composer.skipValue()
        false
    }

    inline fun <reified T> changed(value: T) = if (composer.nextSlot() != value || composer.inserting) {
        composer.updateValue(value)
        true
    } else {
        composer.skipValue()
        false
    }

    inline fun updated(value: Int) = composer.inserting.let { inserting ->
        if (composer.nextSlot() != value || inserting) {
            composer.updateValue(value)
            !inserting
        } else {
            composer.skipValue()
            false
        }
    }

    inline fun <reified T> updated(value: T) = composer.inserting.let { inserting ->
        if (composer.nextSlot() != value || inserting) {
            composer.updateValue(value)
            !inserting
        } else {
            composer.skipValue()
            false
        }
    }

    inline fun set(value: Int, crossinline block: (value: Int) -> Unit): Boolean = changed(value).also { if (it) block(value) }
    inline fun <reified T> set(value: T, crossinline block: (value: T) -> Unit): Boolean = changed(value).also { if (it) block(value) }
    inline fun update(value: Int, crossinline block: (value: Int) -> Unit): Boolean = updated(value).also { if (it) block(value) }
    inline fun <reified T> update(value: T, crossinline block: (value: T) -> Unit): Boolean = updated(value).also { if (it) block(value) }

    inline operator fun Boolean.plus(other: Boolean) = this || other
}

/* inline */ class ComposerUpdater<N, T:N>(val composer: Composer<N>) {
    inline fun set(value: Int, crossinline block: T.(value: Int) -> Unit) {
        if (composer.inserting || composer.nextSlot() != value) {
            composer.updateValue(value)
            val appliedBlock: T.(value: Int) -> Unit = { block(it) }
            composer.apply(value, appliedBlock)
        } else composer.skipValue()
    }

    inline fun <reified V> set(value: V, crossinline block: T.(value: V) -> Unit) {
        if (composer.inserting || composer.nextSlot() != value) {
            composer.updateValue(value)
            val appliedBlock: T.(value: V) -> Unit = { block(it) }
            composer.apply(value, appliedBlock)
        } else composer.skipValue()
    }

    inline fun update(value: Int, crossinline block: T.(value: Int) -> Unit) {
        if (composer.inserting || composer.nextSlot() != value) {
            composer.updateValue(value)
            val appliedBlock: T.(value: Int) -> Unit = { block(it) }
            if (!composer.inserting) composer.apply(value, appliedBlock)
        } else composer.skipValue()
    }

    inline fun <reified V> update(value: V, crossinline block: T.(value: V) -> Unit) {
        if (composer.inserting || composer.nextSlot() != value) {
            composer.updateValue(value)
            val appliedBlock: T.(value: V) -> Unit = { block(it) }
            if (!composer.inserting) composer.apply(value, appliedBlock)
        } else composer.skipValue()
    }
}

val composer get() = ViewComposition((CompositionContext.current as ComposerCompositionContext).composer)
fun ViewComposition.registerAdapter(adapter: (parent: Any, child: Any) -> Any?) = composer.adapters?.register(adapter)

typealias ViewUpdater<T> = ComposerUpdater<Any, T>

@PublishedApi
internal val invocation = Object()
