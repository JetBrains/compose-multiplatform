package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.r4a.adapters.getViewAdapterIfExists
import java.util.*

internal class ViewApplyAdapter : ApplyAdapter<View> {
    private data class PendingInsert(val index: Int, val instance: View)

    private val pendingInserts = Stack<PendingInsert>()

    override fun View.start(instance: View) {}
    override fun View.insertAt(index: Int, instance: View) {
        pendingInserts.push(PendingInsert(index, instance))
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

    override fun View.end(instance: View, parent: View) {
        val adapter = instance.getViewAdapterIfExists()
        val parentGroup = parent as ViewGroup
        if (!pendingInserts.isEmpty()) {
            val pendingInsert = pendingInserts.peek()
            if (pendingInsert.instance === instance) {
                pendingInserts.pop()
                adapter?.willInsert(instance, parentGroup)
                parentGroup.addView(instance, pendingInsert.index)
                adapter?.didInsert(instance, parentGroup)
                return
            }
        }
        adapter?.didUpdate(instance, parentGroup)
    }
}

class ViewComposer(val root: ViewGroup, val context: Context) : Composer<View>(SlotTable(), Applier(root, ViewApplyAdapter())) {
    fun skipGroup(key: Any) {
        nextSlot()
        skipValue()
        skipGroup()
    }
}

/* inline */ class ViewComposition(@PublishedApi internal val composer: ViewComposer) {
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

typealias ViewUpdater<T> = ComposerUpdater<View, T>

@PublishedApi
internal val invocation = Object()
