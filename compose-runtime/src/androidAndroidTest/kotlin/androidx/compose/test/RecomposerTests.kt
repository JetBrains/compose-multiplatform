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

package androidx.compose.test

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.Composable
import androidx.compose.clearRoots
import androidx.compose.frames.currentFrame
import androidx.compose.getValue
import androidx.compose.invalidate
import androidx.compose.key
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RecomposerTests : BaseComposeTest() {
    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testNativeViewWithAttributes() {
        compose {
            TextView(id = 456, text = "some text")
        }.then { activity ->
            assertEquals(1, activity.root.childCount)

            val tv = activity.findViewById(456) as TextView
            assertEquals("some text", tv.text)

            assertEquals(tv, activity.root.getChildAt(0))
        }
    }

    @Test
    fun testSlotKeyChangeCausesRecreate() {
        var i = 1
        var tv1: TextView? = null
        val trigger = Trigger()
        compose {
            trigger.subscribe()
            // this should cause the textview to get recreated on every compose
            i++

            key(i) {
                TextView(id = 456, text = "some text")
            }
        }.then { activity ->
            tv1 = activity.findViewById(456) as TextView
            trigger.recompose()
        }.then { activity ->
            assertEquals("Compose got called twice", 3, i)

            val tv2 = activity.findViewById(456) as TextView

            assertFalse(
                "The text views should be different instances",
                tv1 === tv2
            )

            assertEquals(
                "The unused child got removed from the view hierarchy",
                1,
                activity.root.childCount
            )
        }
    }

    @Test
    fun testViewWithViewChildren() {
        compose {
            LinearLayout(id = 345) {
                TextView(id = 456, text = "some text")
                TextView(id = 567, text = "some text")
            }
        }.then { activity ->
            val ll = activity.findViewById(345) as LinearLayout
            val tv1 = activity.findViewById(456) as TextView
            val tv2 = activity.findViewById(567) as TextView

            assertEquals("The linear layout should be the only child of root", 1,
                activity.root.childCount)
            assertEquals("Both children should have been added", 2, ll.childCount)
            assertTrue(
                "Should be the expected TextView (1)",
                ll.getChildAt(0) === tv1
            )
            assertTrue(
                "Should be the expected TextView (2)",
                ll.getChildAt(1) === tv2
            )
        }
    }

    @Test
    fun testForLoop() {
        val items = listOf(1, 2, 3, 4, 5, 6)
        compose {
            LinearLayout(id = 345) {
                for (i in items) {
                    TextView(id = 456, text = "some text $i")
                }
            }
        }.then { activity ->
            val ll = activity.findViewById(345) as LinearLayout

            assertEquals("The linear layout should be the only child of root", 1,
                activity.root.childCount)
            assertEquals("Each item in the for loop should be a child", items.size, ll.childCount)
            items.forEachIndexed { index, i ->
                assertEquals(
                    "Should be the correct child", "some text $i",
                    (ll.getChildAt(index) as TextView).text
                )
            }
        }
    }

    @Test
    fun testRecompose() {
        val counter = Counter()

        compose {
            RecomposeTestComponentsA(
                counter,
                ClickAction.Recompose
            )
        }.then { activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(102) as TextView).performClick()

            // nothing should happen synchronously
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])
        }.then { activity ->
            // only the clicked view got rerendered
            assertEquals(1, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(2, counter["102"])

            // recompose() both the parent and the child... and show that the child only
            // recomposes once as a result
            (activity.findViewById(99) as LinearLayout).performClick()
            (activity.findViewById(102) as TextView).performClick()
        }.then {

            assertEquals(2, counter["A"])
            assertEquals(3, counter["100"])
            assertEquals(2, counter["101"])
            assertEquals(3, counter["102"])
        }
    }

    @Test
    fun testRootRecompose() {
        val counter = Counter()
        val trigger = Trigger()

        val listener =
            ClickAction.PerformOnView {
                trigger.recompose()
            }

        compose {
            trigger.subscribe()
            RecomposeTestComponentsA(
                counter,
                listener
            )
        }.then { activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(102) as TextView).performClick()

            // nothing should happen synchronously
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])
        }.then { activity ->
            // as we recompose ROOT on every tap, everything should be increased once, because two
            // clicks layed to one frame. None of these components are skippable, so each increments
            assertEquals(2, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(2, counter["101"])
            assertEquals(2, counter["102"])

            (activity.findViewById(99) as LinearLayout).performClick()
            (activity.findViewById(102) as TextView).performClick()
        }.then {
            // again, no matter what we tapped, we want to recompose root, so all counts increased
            assertEquals(3, counter["A"])
            assertEquals(3, counter["100"])
            assertEquals(3, counter["101"])
            assertEquals(3, counter["102"])
        }
    }

    // components for testing recompose behavior above
    sealed class ClickAction {
        object Recompose : ClickAction()
        class PerformOnView(val action: (View) -> Unit) : ClickAction()
    }

    @Composable fun RecomposeTestComponentsB(counter: Counter, listener: ClickAction, id: Int = 0) {
        counter.inc("$id")

        val recompose = invalidate

        TextView(id = id, onClickListener = View.OnClickListener {
            @Suppress("DEPRECATION")
            when (listener) {
                is ClickAction.Recompose -> recompose()
                is ClickAction.PerformOnView -> listener.action.invoke(it)
            }
        })
    }

    @Composable fun RecomposeTestComponentsA(counter: Counter, listener: ClickAction) {
        counter.inc("A")
        val recompose = invalidate
            LinearLayout(id = 99, onClickListener = View.OnClickListener {
                @Suppress("DEPRECATION")
                when (listener) {
                    is ClickAction.Recompose -> recompose()
                    is ClickAction.PerformOnView -> listener.action.invoke(it)
                }
            }) {
                for (id in 100..102) {
                    key(id) {
                        RecomposeTestComponentsB(
                            counter,
                            listener,
                            id
                        )
                    }
                }
            }
    }

    @Test
    fun testCorrectViewTree() {
        compose {
            LinearLayout {
                LinearLayout { }
                LinearLayout { }
            }
            LinearLayout { }
        }.then { activity ->
            assertChildHierarchy(activity.root) {
                """
                    <LinearLayout>
                        <LinearLayout />
                        <LinearLayout />
                    </LinearLayout>
                    <LinearLayout />
                """
            }
        }
    }

    @Test
    fun testCorrectViewTreeWithComponents() {

        @Composable fun B() {
            TextView()
        }

        compose {
            LinearLayout {
                LinearLayout {
                    B()
                }
                LinearLayout {
                    B()
                }
            }
        }.then { activity ->

            assertChildHierarchy(activity.root) {
                """
                <LinearLayout>
                    <LinearLayout>
                        <TextView />
                    </LinearLayout>
                    <LinearLayout>
                        <TextView />
                    </LinearLayout>
                </LinearLayout>
                """
            }
        }
    }

    @Test
    fun testCorrectViewTreeWithComponentWithMultipleRoots() {

        @Composable fun B() {
            TextView()
            TextView()
        }

        compose {
            LinearLayout {
                LinearLayout {
                    B()
                }
                LinearLayout {
                    B()
                }
            }
        }.then {

            assertChildHierarchy(activity.root) {
                """
                <LinearLayout>
                    <LinearLayout>
                        <TextView />
                        <TextView />
                    </LinearLayout>
                    <LinearLayout>
                        <TextView />
                        <TextView />
                    </LinearLayout>
                </LinearLayout>
                """
            }
        }
    }

    @Test // regression b/157111271
    fun testInsertDuringRecomposition() {
        var includeA by mutableStateOf(false)
        var someState by mutableStateOf(0)
        var someOtherState by mutableStateOf(1)

        @Composable fun B(@Suppress("UNUSED_PARAMETER") value: Int) {
            // empty
        }

        @Composable fun A() {
            B(someState)
            someState++
        }

        @Composable fun T() {
            subCompose {
                // Take up some slot space
                // This makes it more likely to reproduce bug 157111271.
                remember(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15) {
                    1
                }
                if (includeA) {
                    Wrapper {
                        B(0)
                        B(someOtherState)
                        B(2)
                        B(3)
                        B(4)
                        A()
                    }
                }
            }
        }

        compose {
            T()
        }.then {
            includeA = true
        }.then {
            someOtherState = 10
        }.then {
            // force recompose
        }
    }

    @Test
    fun testFrameTransition() {
        var frameId: Int? = null
        compose {
            frameId = currentFrame().id
        }.then {
            assertNotSame(frameId, currentFrame().id)
        }
    }
}

@Composable
fun Wrapper(children: @Composable () -> Unit) {
    children()
}

fun assertChildHierarchy(root: ViewGroup, getHierarchy: () -> String) {
    val realHierarchy = printChildHierarchy(root)

    assertEquals(
        normalizeString(getHierarchy()),
        realHierarchy.trim()
    )
}

fun normalizeString(str: String): String {
    val lines = str.split('\n').dropWhile { it.isBlank() }.dropLastWhile {
        it.isBlank()
    }
    if (lines.isEmpty()) return ""
    val toRemove = lines.first().takeWhile { it == ' ' }.length
    return lines.joinToString("\n") { it.substring(Math.min(toRemove, it.length)) }
}

fun printChildHierarchy(root: ViewGroup): String {
    val sb = StringBuilder()
    for (i in 0 until root.childCount) {
        printView(root.getChildAt(i), 0, sb)
    }
    return sb.toString()
}

fun printView(view: View, indent: Int, sb: StringBuilder) {
    val whitespace = " ".repeat(indent)
    val name = view.javaClass.simpleName
    val attributes = printAttributes(view)
    if (view is ViewGroup && view.childCount > 0) {
        sb.appendln("$whitespace<$name$attributes>")
        for (i in 0 until view.childCount) {
            printView(view.getChildAt(i), indent + 4, sb)
        }
        sb.appendln("$whitespace</$name>")
    } else {
        sb.appendln("$whitespace<$name$attributes />")
    }
}

fun printAttributes(view: View): String {
    val attrs = mutableListOf<String>()

    // NOTE: right now we only look for id and text as attributes to print out... but we are
    // free to add more if it makes sense
    if (view.id != -1) {
        attrs.add("id=${view.id}")
    }

    if (view is TextView && view.text.length > 0) {
        attrs.add("text='${view.text}'")
    }

    val result = attrs.joinToString(" ", prefix = " ")
    if (result.length == 1) {
        return ""
    }
    return result
}

class Counter {
    private var counts = mutableMapOf<String, Int>()
    fun inc(key: String) = counts.getOrPut(key, { 0 }).let { counts[key] = it + 1 }
    fun reset() {
        counts = mutableMapOf()
    }

    operator fun get(key: String) = counts[key] ?: 0
}
