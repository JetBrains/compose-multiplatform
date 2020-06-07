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
@file:OptIn(ComposeCompilerApi::class, ExperimentalComposeApi::class)
package androidx.compose.test

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composable
import androidx.compose.ComposeCompilerApi
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.Composition
import androidx.compose.ExperimentalComposeApi
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.compositionFor
import androidx.ui.node.UiComposer

interface Emittable {
    fun emitInsertAt(index: Int, instance: Emittable)
    fun emitRemoveAt(index: Int, count: Int)
    fun emitMove(from: Int, to: Int, count: Int)
}

internal class EmittableApplyAdapter : ApplyAdapter<Any> {
    override fun Any.start(instance: Any) {}
    override fun Any.insertAt(index: Int, instance: Any) {
        when (this) {
            is ViewGroup -> insertAt(index, instance)
            is Emittable -> emitInsertAt(index, instance as Emittable)
            else -> error("unexpected node")
        }
    }

    override fun Any.removeAt(index: Int, count: Int) {
        when (this) {
            is ViewGroup -> removeViews(index, count)
            is Emittable -> emitRemoveAt(index, count)
            else -> error("unexpected node")
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
            else -> error("unexpected node")
        }
    }

    override fun Any.end(instance: Any, parent: Any) {}
}

class EmittableComposer(
    val context: Context,
    val root: Any,
    slotTable: SlotTable,
    recomposer: Recomposer
) : Composer<Any>(
    slotTable,
    Applier(
        root,
        EmittableApplyAdapter()
    ),
    recomposer
) {
    init {
        FrameManager.ensureStarted()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: ViewUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        ViewUpdater(this, node).update()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ViewGroup> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        ViewUpdater(this, node).update()
        children()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Emittable> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: ViewUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        ViewUpdater(this, node).update()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Emittable> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: ViewUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        ViewUpdater(this, node).update()
        children()
        endNode()
    }
}

typealias ViewUpdater<T> = ComposerUpdater<Any, T>

class ComponentNodeScope { val composer: UiComposer get() = error("should not get called") }
class EmittableScope { val composer: EmittableComposer get() = error("should not get called") }

class Node(val name: String, var value: String = "") : Emittable {
    val children = mutableListOf<Node>()

    override fun emitInsertAt(index: Int, instance: Emittable) {
        children.add(index, instance as Node)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        repeat(count) { children.removeAt(index) }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        if (from > to) {
            repeat(count) {
                children.add(to + it, children.removeAt(from))
            }
        } else if (from < to) {
            repeat(count) {
                children.add(to - 1, children.removeAt(from))
            }
        }
    }
}

fun Activity.setEmittableContent(content: @Composable () -> Unit): Composition {
    val root = Node("Root")
    val composition = compositionFor(root, Recomposer.current()) { slotTable, recomposer ->
        EmittableComposer(
            this,
            root,
            slotTable,
            recomposer
        )
    }
    composition.setContent(content)
    return composition
}
