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

@file:Suppress("USELESS_CAST")

package androidx.compose

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
class HotReloadTests: BaseComposeTest() {
    @After
    fun teardown() {
        Compose.clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun composeView() {
        var value = "First value"

        compose {
            column {
                text(text = "Hello", id = 101)
                text(text = "World", id = 102)
                text(text = value, id = 103)
            }
        }.then { activity ->
            assertEquals(activity.findViewById<TextView>(103).text, value)
            value = "Second value"
        }.then { activity ->
            assertNotEquals(activity.findViewById<TextView>(103).text, value)

            Compose.simulateHotReload(activity)

            assertEquals(activity.findViewById<TextView>(103).text, value)
        }
    }

    @Test
    fun composeEmittable() {
        var value = "First value"

        val activity = activityRule.activity

        // Set the content of the view
        activity.uiThread {
            activity.setContent {
                columnNode {
                    textNode(text = "Hello")
                    textNode(text = "World")
                    textNode(text = value)
                }
            }
        }

        // Allow the composition to complete
        activity.waitForAFrame()

        fun target() = activity.content.children[0].children[2]

        // Assert that the composition has the correct value
        assertEquals(target().value, value)

        value = "Second value"

        // Ensure the composition hasn't changed
        activity.waitForAFrame()
        assertNotEquals(target().value, value)

        // Simulate hot-reload
        activity.uiThread {
            Compose.simulateHotReload(activity)
        }

        // Detect tha tthe node changed
        assertEquals(target().value, value)
    }
}

fun text(text: String, id: Int = -1) {
    composer.emit(
        48,
        { context ->
            TextView(context).apply {
                if (id >= 0) this.id = id
            }
        },
        {
            set(text) { this.text = it }
        }
    )
}

fun column(children: () -> Unit) {
    composer.emit(
        key = 59,
        ctor = { context ->
            LinearLayout(context)
        },
        update = { },
        children = children as (@Composable() () -> Unit)
    )
}

fun textNode(text: String) {
    composer.emit(
        key = 93,
        ctor = { Node("Text") } as () -> Node,
        update = {
            set(text) { this.value = it }
        }
    )
}

fun columnNode(children: () -> Unit) {
    composer.emit(
        key = 93,
        ctor = { Node("Text") } as () -> Node,
        update = { },
        children = children as (@Composable() () -> Unit)
    )
}

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

fun Activity.setContent(content: () -> Unit) {
    val composeView = contentView as? ViewEmitWrapper
        ?: ViewEmitWrapper(this).also {
            setContentView(it)
        }
    val root = Node("Root")
    composeView.emittable = root
    Compose.composeInto(root, this, null, content as (@Composable() () -> Unit))
}

val Activity.contentView: View get() =
    window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

val Activity.content: Node get() =
    (contentView as ViewEmitWrapper).emittable as Node

class ViewEmitWrapper(context: Context) : View(context) {
    var emittable: Emittable? = null
}
