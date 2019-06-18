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

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.frames.currentFrame
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposerCompositionContextTests : ComposerComposeTestCase() {

    @Test
    fun testNativeViewWithAttributes() = compose {
        with(it) {
            // <TextView id={456} text="some text" />
            emitView(123, ::TextView) {
                set(456) { id = it }
                set("some text") { text = it }
            }
        }
    }.then { _, _, root, activity ->
        assertEquals(1, root.childCount)

        val tv = activity.findViewById(456) as TextView
        assertEquals("some text", tv.text)

        assertEquals(tv, root.getChildAt(0))
    }

    @Test
    fun testSlotKeyChangeCausesRecreate() {
        var i = 1

        compose {
            // this should cause the textview to get recreated on every compose
            i++

            with(it) {
                // <TextView id={456} text="some text" />
                emitView(i, ::TextView) {
                    set(456) { id = it }
                    set("some text") { text = it }
                }
            }
        }.then { _, component, root, activity ->
            val tv1 = activity.findViewById(456) as TextView

            component.recomposeCallback?.invoke(false)

            assertEquals("Compose got called twice", 3, i)

            val tv2 = activity.findViewById(456) as TextView

            assertFalse(
                "The text views should be different instances",
                tv1 === tv2
            )

            assertEquals(
                "The unused child got removed from the view hierarchy",
                1,
                root.childCount
            )
        }
    }

    @Test
    fun testViewWithViewChildren() {
        compose {
            // <LinearLayout id={345}>
            with(it) {
                emitViewGroup(100, ::LinearLayout, {
                    set(345) { id = it }
                }) {
                    // <TextView id={456} text="some text" />
                    emitView(101, ::TextView) {
                        set(456) { id = it }
                        set("some text") { text = it }
                    }
                    // <TextView id={567} text="some text" />
                    emitView(102, ::TextView) {
                        set(567) { id = it }
                        set("some text") { text = it }
                    }
                }
            }
        }.then { _, _, root, activity ->
            val ll = activity.findViewById(345) as LinearLayout
            val tv1 = activity.findViewById(456) as TextView
            val tv2 = activity.findViewById(567) as TextView

            assertEquals("The linear layout should be the only child of root", 1, root.childCount)
            assertEquals("Both children should have gotten added", 2, ll.childCount)
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
            // this should cause the textview to get recreated on every compose
            with(it) {
                emitViewGroup(100, ::LinearLayout, {
                    set(345) { id = it }
                }) {
                    for (i in items) {
                        // <TextView id={456} text="some text" />
                        emitView(101, ::TextView) {
                            set(456) { id = it }
                            set("some text $i") { text = it }
                        }
                    }
                }
            }
        }.then { _, _, root, activity ->
            val ll = activity.findViewById(345) as LinearLayout

            assertEquals("The linear layout should be the only child of root", 1, root.childCount)
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
            // <A />
            it.emitComponent(123) {
                RecomposeTestComponents.A(
                    counter,
                    RecomposeTestComponents.ClickAction.Recompose
                )
            }
        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // Robolectric will by default just run everything sync. pause() is needed to emulate
            // delays
            RuntimeEnvironment.getMasterScheduler().pause()

            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(102) as TextView).performClick()

            // nothing should happen synchronously
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().unPause()

            // only the clicked view got rerendered
            assertEquals(1, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(2, counter["102"])

            RuntimeEnvironment.getMasterScheduler().pause()

            // recompose() both the parent and the child... and show that the child only
            // recomposes once as a result
            (activity.findViewById(99) as LinearLayout).performClick()
            (activity.findViewById(102) as TextView).performClick()

            RuntimeEnvironment.getMasterScheduler().unPause()
            RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()

            assertEquals(2, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(3, counter["102"])
        }
    }

    @Test
    fun testRecomposeSync() {
        val counter = Counter()

        compose {
            // <A />
            it.emitComponent(123) {
                RecomposeTestComponents.A(
                    counter,
                    RecomposeTestComponents.ClickAction.RecomposeSync
                )
            }
        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // stop the time so only sync recomposes will take place
            RuntimeEnvironment.getMasterScheduler().pause()

            (activity.findViewById(100) as TextView).performClick()

            // only the clicked view got rerendered
            assertEquals(1, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // unpause to see if nothing is scheduled during recomposeSync()
            RuntimeEnvironment.getMasterScheduler().unPause()

            assertEquals(1, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().pause()
            // try to recompose the parent, but ensure that even if we tap textView several times,
            // it's all got recomposed once
            (activity.findViewById(99) as LinearLayout).performClick()
            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(100) as TextView).performClick()

            // only the twice clicked view got rerendered twice
            assertEquals(2, counter["A"])
            // this should be 5, as it gets recomposed synchronously 3 times due to TV taps
            assertEquals(5, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().unPause()
        }
    }

    @Test
    fun testRootRecompose() {
        val counter = Counter()

        val listener =
            RecomposeTestComponents.ClickAction.PerformOnView {
                Compose.findRoot(it)?.let {
                    it.recomposeCallback?.invoke(false)
                }
            }

        compose {
            // <A />
            it.emitComponent(123) {
                RecomposeTestComponents.A(
                    counter,
                    listener
                )
            }
        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // Robolectric will by default just run everything sync. pause() is needed to emulate
            // delays
            RuntimeEnvironment.getMasterScheduler().pause()

            (activity.findViewById(100) as TextView).performClick()
            (activity.findViewById(102) as TextView).performClick()

            // nothing should happen synchronously
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().unPause()

            // as we recompose ROOT on every tap, only root(and LinearLayout) counter should we
            // increased once, because two clicks layed to one frame
            assertEquals(2, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().pause()

            (activity.findViewById(99) as LinearLayout).performClick()
            (activity.findViewById(102) as TextView).performClick()

            RuntimeEnvironment.getMasterScheduler().unPause()
            RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()

            // again, no matter what we tappes, we want to recompose root, so LinearLayout's counter
            // got increased
            assertEquals(3, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])
        }
    }

    @Test
    fun testRootRecomposeSync() {
        val counter = Counter()

        val listener =
            RecomposeTestComponents.ClickAction.PerformOnView {
                Compose.findRoot(it)?.let {
                    it.recomposeCallback?.invoke(true)
                }
            }
        compose {
            // <A />
            it.emitComponent(123) {
                RecomposeTestComponents.A(
                    counter,
                    listener
                )
            }
        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // stop the time so only sync recomposes will take place
            RuntimeEnvironment.getMasterScheduler().pause()

            (activity.findViewById(100) as TextView).performClick()

            // important! as we recompose Root every time
            // no matter what we clicked, root (and LinearLayout) gets rerendered
            assertEquals(2, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            (activity.findViewById(99) as LinearLayout).performClick()

            assertEquals(3, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            RuntimeEnvironment.getMasterScheduler().unPause()
            RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()
            // make sure nothing has been scheduled inside recomposeSync()
            assertEquals(3, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])
        }
    }

    // components for testing recompose behavior above
    private object RecomposeTestComponents {
        sealed class ClickAction {
            object Recompose : ClickAction()
            object RecomposeSync : ClickAction()
            class PerformOnView(val action: (View) -> Unit) : ClickAction()
        }

        class B(val counter: Counter, val listener: ClickAction) : Component() {
            var id: Int = 0
            override fun compose() {
                counter.inc("$id")

                with(composer) {
                    // <TextView id={id} onClickListener={{ clickAction() }} />
                    emitView(24, ::TextView) {
                        set(id) { id = it }
                        set(View.OnClickListener {
                            @Suppress("DEPRECATION")
                            when (listener) {
                                is ClickAction.Recompose -> recompose()
                                is ClickAction.RecomposeSync -> recomposeSync()
                                is ClickAction.PerformOnView -> listener.action.invoke(it)
                            }
                        }) { setOnClickListener(it) }
                    }
                }
            }
        }

        class A(val counter: Counter, val listener: ClickAction) : Component() {
            override fun compose() {
                counter.inc("A")
                // <LinearLayout onClickListener={{ clickAction() }} id={99}>
                //     <B id={100} />
                //     <B id={101} />
                //     <B id={102} />
                // </LinearLayout>

                with(composer) {

                    // <LinearLayout id={99} onClickListener={{ clickAction() }}/>
                    emitViewGroup(897, ::LinearLayout, {
                        set(99) { id = it }
                        set(View.OnClickListener {
                            @Suppress("DEPRECATION")
                            when (listener) {
                                is ClickAction.Recompose -> recompose()
                                is ClickAction.RecomposeSync -> recomposeSync()
                                is ClickAction.PerformOnView -> listener.action.invoke(it)
                            }
                        }) { setOnClickListener(it) }
                    }) {
                        for (id in 100..102) {
                            // <B key={id} id={id} />
                            emitComponent(878983, id, {
                                B(
                                    counter,
                                    listener
                                )
                            }) { f ->
                                set(id) { f.id = it }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testCorrectViewTree() = compose {
        // <LinearLayout>
        //   <LinearLayout />
        //   <LinearLayout />
        // </LinearLayout>
        // <LinearLayout />

        with(it) {
            emitViewGroup(123, ::LinearLayout, {}) {
                emitView(123, ::LinearLayout)
                emitView(123, ::LinearLayout)
            }
            emitView(123, ::LinearLayout)
        }
    }.then { _, _, root, _ ->
        assertChildHierarchy(root) {
            """
                <LinearLayout>
                    <LinearLayout />
                    <LinearLayout />
                </LinearLayout>
                <LinearLayout />
            """
        }
    }

    @Test
    fun testCorrectViewTreeWithComponents() {

        class B : Component() {
            override fun compose() {
                with(composer) {
                    // <TextView />
                    emitView(123, ::TextView)
                }
            }
        }

        compose {
            // <LinearLayout>
            //   <LinearLayout>
            //     <B />
            //   </LinearLayout>
            //   <LinearLayout>
            //     <B />
            //   </LinearLayout>
            // </LinearLayout>

            with(it) {
                emitViewGroup(123, ::LinearLayout, {}) {
                    emitViewGroup(123, ::LinearLayout, {}) {
                        emitComponent(123, ::B)
                    }
                    emitViewGroup(123, ::LinearLayout, {}) {
                        emitComponent(123, ::B)
                    }
                }
            }
        }.then { _, _, root, _ ->

            assertChildHierarchy(root) {
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

        class B : Component() {
            override fun compose() {
                with(composer) {
                    // <TextView />
                    emitView(123, ::TextView)
                    emitView(124, ::TextView)
                }
            }
        }

        compose {
            // <LinearLayout>
            //   <LinearLayout>
            //     <B />
            //   </LinearLayout>
            //   <LinearLayout>
            //     <B />
            //   </LinearLayout>
            // </LinearLayout>

            with(it) {
                emitViewGroup(123, ::LinearLayout, {}) {
                    emitViewGroup(123, ::LinearLayout, {}) {
                        emitComponent(123, ::B)
                    }
                    emitViewGroup(123, ::LinearLayout, {}) {
                        emitComponent(123, ::B)
                    }
                }
            }
        }.then { _, _, root, _ ->

            assertChildHierarchy(root) {
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

    @Test
    fun testFrameTransition() {
        var frameId: Int? = null
        compose {
            frameId = currentFrame().id
        }.then { _, _, _, _ ->
            assertNotSame(frameId, currentFrame().id)
        }
    }
}

fun assertChildHierarchy(root: ViewGroup, getHierarchy: () -> String) {
    val realHierarchy = printChildHierarchy(root)

    TestCase.assertEquals(
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
