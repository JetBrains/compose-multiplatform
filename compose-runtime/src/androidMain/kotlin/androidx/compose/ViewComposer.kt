/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.adapters.getViewAdapterIfExists

class ViewAdapters {
    private val adapters = mutableListOf<(parent: Any, child: Any) -> Any?>()

    fun register(adapter: (parent: Any, child: Any) -> Any?) = adapters.add(adapter)
    fun adapt(parent: Any, child: Any): Any? =
        adapters.map { it(parent, child) }.filterNotNull().firstOrNull()
}

private fun invalidNode(node: Any): Nothing =
    error("Unsupported node type ${node.javaClass.simpleName}")

internal class ViewApplyAdapter(private val adapters: ViewAdapters? = null) :
    ApplyAdapter<Any> {
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
                    var currentFrom = from
                    var currentTo = to
                    repeat(count) {
                        val view = getChildAt(currentFrom)
                        removeViewAt(currentFrom)
                        addView(view, currentTo)
                        currentFrom++
                        currentTo++
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
                                val adaptedView = adapters?.adapt(parent, instance) as? View
                                    ?: error(
                                        "Could not convert ${
                                        instance.javaClass.simpleName
                                        } to a View"
                                    )
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
                                adapters?.adapt(parent, instance) as? Emittable
                                    ?: error(
                                        "Could not convert ${
                                        instance.javaClass.name
                                        } to an Emittable"
                                    )
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

internal actual fun UiComposer(
    context: Context,
    root: Any,
    slots: SlotTable,
    recomposer: Recomposer
): Composer<*> = ViewComposer(context, root, slots, recomposer)

class ViewComposer(
    val context: Context,
    val root: Any,
    slotTable: SlotTable,
    recomposer: Recomposer,
    val adapters: ViewAdapters? = ViewAdapters()
) : Composer<Any>(
    slotTable,
    Applier(root, ViewApplyAdapter(adapters)),
    recomposer
) {
    init {
        FrameManager.ensureStarted()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : View> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: ViewUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        ViewUpdater<T>(this, node).update()
        endNode()
    }

    @Suppress("PLUGIN_ERROR", "UNCHECKED_CAST")
    inline fun <T : ViewGroup> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: @Composable() () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        ViewUpdater<T>(this, node).update()
        children()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : Emittable> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: ViewUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        ViewUpdater<T>(this, node).update()
        endNode()
    }

    @Suppress("PLUGIN_ERROR", "UNCHECKED_CAST")
    inline fun <T : Emittable> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: @Composable() () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        ViewUpdater<T>(this, node).update()
        children()
        endNode()
    }

    @Suppress("PLUGIN_ERROR")
    inline fun call(
        key: Any,
        invalid: ViewValidator.() -> Boolean,
        block: @Composable() () -> Unit
    ) {
        startGroup(key)
        if (ViewValidator(this).invalid() || !skipping) {
            startGroup(invocation)
            block()
            endGroup()
        } else {
            skipCurrentGroup()
        }
        endGroup()
    }
}

/* inline */ class ViewValidator(val composer: Composer<*>) {
    // TODO: Add more overloads for common primitive types like String and Float etc to avoid boxing
    // and the immutable check
    @Suppress("NOTHING_TO_INLINE")
    fun changed(value: Int) = with(composer) {
        if ((nextSlot() as? Int)?.let { value != it } ?: true || inserting) {
            updateValue(value)
            true
        } else {
            skipValue()
            false
        }
    }

    fun <T> changed(value: T) = with(composer) {
        if (nextSlot() != value || inserting) {
            updateValue(value)
            true
        } else {
            skipValue()
            false
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    fun updated(value: Int) = with(composer) {
        inserting.let { inserting ->
            if (((nextSlot() as? Int)?.let { it != value } ?: true) || inserting) {
                updateValue(value)
                !inserting
            } else {
                skipValue()
                false
            }
        }
    }

    fun <T> updated(value: T) = with(composer) {
        inserting.let { inserting ->
            if (nextSlot() != value || inserting) {
                updateValue(value)
                !inserting
            } else {
                skipValue()
                false
            }
        }
    }

    inline fun set(value: Int, /*crossinline*/ block: (value: Int) -> Unit): Boolean =
        changed(value).also { if (it) block(value) }

    inline fun <reified T> set(value: T, /*crossinline*/ block: (value: T) -> Unit): Boolean =
        changed(value).also { if (it) block(value) }

    inline fun update(value: Int, /*crossinline*/ block: (value: Int) -> Unit): Boolean =
        updated(value).also { if (it) block(value) }

    inline fun <reified T> update(value: T, /*crossinline*/ block: (value: T) -> Unit): Boolean =
        updated(value).also { if (it) block(value) }

    @Suppress("UNUSED")
    fun <T> changedUnchecked(@Suppress("UNUSED_PARAMETER") value: T) = true

    @Suppress("UNUSED")
    inline fun <T> setUnchecked(value: T, block: (value: T) -> Unit): Boolean {
        block(value)
        return true
    }

    @Suppress("UNUSED")
    inline fun <T> updateUnchecked(value: T, block: (value: T) -> Unit): Boolean {
        block(value)
        return true
    }

    /*inline*/ operator fun Boolean.plus(other: Boolean) = this || other
}

@Suppress("UNCHECKED_CAST")
/*inline */ class ComposerUpdater<N, T : N>(val composer: Composer<N>, val node: T) {
    inline fun set(
        value: Int,
        /*crossinline*/
        block: T.(value: Int) -> Unit
    ) = with(composer) {
        if (inserting || nextSlot() != value) {
            updateValue(value)
            node.block(value)
//            val appliedBlock: T.(value: Int) -> Unit = { block(it) }
//            composer.apply(value, appliedBlock)
        } else skipValue()
    }

    inline fun <reified V> set(
        value: V,
        /*crossinline*/
        block: T.(value: V) -> Unit
    ) = with(composer) {
        if (inserting || nextSlot() != value) {
            updateValue(value)
            node.block(value)
//            val appliedBlock: T.(value: V) -> Unit = { block(it) }
//            composer.apply(value, appliedBlock)
        } else skipValue()
    }

    inline fun update(
        value: Int,
        /*crossinline*/
        block: T.(value: Int) -> Unit
    ) = with(composer) {
        if (inserting || nextSlot() != value) {
            updateValue(value)
            node.block(value)
//            val appliedBlock: T.(value: Int) -> Unit = { block(it) }
//            if (!inserting) composer.apply(value, appliedBlock)
        } else skipValue()
    }

    inline fun <reified V> update(
        value: V,
        /*crossinline*/
        block: T.(value: V) -> Unit
    ) = with(composer) {
        if (inserting || nextSlot() != value) {
            updateValue(value)
            node.block(value)
//            val appliedBlock: T.(value: V) -> Unit = { block(it) }
//            if (!inserting) composer.apply(value, appliedBlock)
        } else skipValue()
    }
}

actual val currentComposerNonNull: Composer<*>
    get() = currentComposer ?: emptyComposition()

private fun emptyComposition(): Nothing =
    error("Composition requires an active composition context")

val composer get() = currentComposerNonNull as ViewComposer

internal actual var currentComposer: Composer<*>? = null
    private set

actual fun <T> Composer<*>.runWithCurrent(block: () -> T): T {
    val prev = currentComposer
    try {
        currentComposer = this
        return block()
    } finally {
        currentComposer = prev
    }
}

actual fun <T> Composer<*>.runWithComposing(block: () -> T): T {
    val wasComposing = isComposing
    try {
        isComposing = true
        return block()
    } finally {
        isComposing = wasComposing
    }
}

fun ViewComposer.registerAdapter(
    adapter: (parent: Any, child: Any) -> Any?
) = adapters?.register(adapter)

typealias ViewUpdater<T> = ComposerUpdater<Any, T>