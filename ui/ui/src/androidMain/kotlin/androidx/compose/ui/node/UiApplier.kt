/*
 * Copyright 2020 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeApi::class)
package androidx.compose.ui.node

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Applier
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.compose.ui.viewinterop.InternalInteropApi
import androidx.compose.ui.viewinterop.ViewBlockHolder

// TODO: evaluate if this class is necessary or not
private class Stack<T> {
    private val backing = ArrayList<T>()

    val size: Int get() = backing.size

    fun push(value: T) = backing.add(value)
    fun pop(): T = backing.removeAt(size - 1)
    fun peek(): T = backing[size - 1]
    fun isEmpty() = backing.isEmpty()
    fun isNotEmpty() = !isEmpty()
    fun clear() = backing.clear()
}

@OptIn(ExperimentalLayoutNodeApi::class)
class UiApplier(
    private val root: Any
) : Applier<Any> {
    private val stack = Stack<Any>()
    private data class PendingInsert(val index: Int, val instance: Any)
    // TODO(b/159073250): remove
    private val pendingInserts = Stack<PendingInsert>()

    private fun invalidNode(node: Any): Nothing =
        error("Unsupported node type ${node.javaClass.simpleName}")

    override var current: Any = root
        private set

    override fun down(node: Any) {
        stack.push(current)
        current = node
    }

    override fun up() {
        val instance = current
        val parent = stack.pop()
        current = parent
        // TODO(lmr): We should strongly consider removing this ViewAdapter concept
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
                            is LayoutNode -> {
                                val composeView = AndroidComposeView(parent.context)
                                parent.addView(composeView, index)
                                composeView.root.insertAt(0, instance)
                            }
                            else -> invalidNode(instance)
                        }
                    is LayoutNode ->
                        when (instance) {
                            is View -> {
                                // Wrap the instance in an AndroidViewHolder, unless the instance
                                // itself is already one.
                                @OptIn(InternalInteropApi::class)
                                val androidViewHolder =
                                    if (instance is AndroidViewHolder) {
                                        instance
                                    } else {
                                        ViewBlockHolder<View>(instance.context).apply {
                                            view = instance
                                        }
                                    }

                                parent.insertAt(index, androidViewHolder.toLayoutNode())
                            }
                            is LayoutNode -> parent.insertAt(index, instance)
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

    override fun insert(index: Int, instance: Any) {
        pendingInserts.push(
            PendingInsert(
                index,
                instance
            )
        )
    }

    override fun remove(index: Int, count: Int) {
        when (val node = current) {
            is ViewGroup -> node.removeViews(index, count)
            is LayoutNode -> node.removeAt(index, count)
            else -> invalidNode(node)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        when (val node = current) {
            is ViewGroup -> {
                if (from > to) {
                    var currentFrom = from
                    var currentTo = to
                    repeat(count) {
                        val view = node.getChildAt(currentFrom)
                        node.removeViewAt(currentFrom)
                        node.addView(view, currentTo)
                        currentFrom++
                        currentTo++
                    }
                } else {
                    repeat(count) {
                        val view = node.getChildAt(from)
                        node.removeViewAt(from)
                        node.addView(view, to - 1)
                    }
                }
            }
            is LayoutNode -> {
                node.move(from, to, count)
            }
            else -> invalidNode(node)
        }
    }

    override fun clear() {
        stack.clear()
        current = root
        when (root) {
            is ViewGroup -> root.removeAllViews()
            is LayoutNode -> root.removeAll()
            else -> invalidNode(root)
        }
    }
}