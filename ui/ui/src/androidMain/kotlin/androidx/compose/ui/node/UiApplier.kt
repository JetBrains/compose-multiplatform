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
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.compose.ui.viewinterop.InternalInteropApi
import androidx.compose.ui.viewinterop.ViewBlockHolder

class UiApplier(root: Any) : AbstractApplier<Any>(root) {
    private fun invalidNode(node: Any): Nothing =
        error("Unsupported node type ${node.javaClass.simpleName}")

    override fun up() {
        val instance = current
        super.up()
        val parent = current
        if (parent is ViewGroup && instance is View) {
            instance.getViewAdapterIfExists()?.didUpdate(instance, parent)
        }
    }

    override fun insertTopDown(index: Int, instance: Any) {
        // Ignored. Insert is performed in [insertBottomUp] to build the tree bottom-up to avoid
        // duplicate notification when the child nodes enter the tree.
    }

    override fun insertBottomUp(index: Int, instance: Any) {
        val adapter = when (instance) {
            is View -> instance.getViewAdapterIfExists()
            else -> null
        }
        when (val parent = current) {
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

    override fun onClear() {
        when (root) {
            is ViewGroup -> root.removeAllViews()
            is LayoutNode -> root.removeAll()
            else -> invalidNode(root)
        }
    }

    override fun onEndChanges() {
        super.onEndChanges()
        if (root is ViewGroup) {
            clearInvalidObservations(root)
        } else if (root is LayoutNode) {
            (root.owner as? AndroidComposeView)?.clearInvalidObservations()
        }
    }

    private fun clearInvalidObservations(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AndroidComposeView) {
                child.clearInvalidObservations()
            } else if (child is ViewGroup) {
                clearInvalidObservations(child)
            }
        }
    }
}