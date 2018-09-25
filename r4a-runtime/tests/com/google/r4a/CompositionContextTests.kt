@file:Suppress("unused", "LocalVariableName", "UnnecessaryVariable")

package com.google.r4a

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class CompositionContextTests : ComposeTestCase() {

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
        }.then { cc, component, root, activity ->
            val tv1 = activity.findViewById(456) as TextView

            cc.recompose(component)

            assertEquals("Compose got called twice", 3, i)

            val tv2 = activity.findViewById(456) as TextView

            assertFalse("The text views should be different instances", tv1 === tv2)

            assertEquals("The unused child got removed from the view hierarchy", 1, root.childCount)
        }
    }

    @Test
    fun testViewWithViewChildren() {
        compose {
            // this should cause the textview to get recreated on every compose

            // <LinearLayout id={345}>
            with(it) {
                emitView(100, ::LinearLayout, {
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
            assertTrue("Should be the expected TextView (1)", ll.getChildAt(0) === tv1)
            assertTrue("Should be the expected TextView (2)", ll.getChildAt(1) === tv2)
        }
    }


    @Test
    fun testForLoop() {
        val items = listOf(1, 2, 3, 4, 5, 6)
        compose {
            // this should cause the textview to get recreated on every compose
            with(it) {
                emitView(100, ::LinearLayout, {
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
                assertEquals("Should be the correct child", "some text $i", (ll.getChildAt(index) as TextView).text)
            }
        }
    }


    @Test
    fun testRecompose() {
        val counter = Counter()

        class B : Component() {
            var id: Int = 0
            override fun compose() {
                counter.inc("$id")

                with(CompositionContext.current) {
                    // <TextView id={id} onClickListener={{ recomposeSync() }} />
                    emitView(24, ::TextView) {
                        set(id) { id = it }
                        set(View.OnClickListener { recompose() }) { setOnClickListener(it) }
                    }
                }
            }
        }

        class A : Component() {
            override fun compose() {
                counter.inc("A")
                // <LinearLayout onClickListener={{ recompose() }} id={99}>
                //     <B id={100} />
                //     <B id={101} />
                //     <B id={102} />
                // </LinearLayout>

                with(CompositionContext.current) {

                    // <LinearLayout onClickListener={{ recompose() }} id={99}>
                    emitView(897, ::LinearLayout, {
                        set(99) { id = it }
                        set(View.OnClickListener { recompose() }) { setOnClickListener(it) }
                    }) {
                        for (id in 100..102) {
                            // <B key={id} id={id} />
                            emitComponent(878983, id, ::B) {
                                set(id) { this.id = it }
                            }
                        }
                    }
                }
            }
        }


        compose {
            // <A />
            it.emitComponent(123, ::A)

        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            // Robolectric will by default just run everything sync. pause() is needed to emulate delays
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

            assertEquals(2, counter["A"])
            assertEquals(3, counter["100"])
            assertEquals(2, counter["101"])
            assertEquals(3, counter["102"])
        }
    }

    @Test
    fun testRecomposeSync() {
        val counter = Counter()

        class B : Component() {
            var id: Int = 0
            override fun compose() {
                counter.inc("$id")

                with(CompositionContext.current) {
                    // <TextView id={id} onClickListener={{ recomposeSync() }} />
                    emitView(24, ::TextView) {
                        set(id) { id = it }
                        set(View.OnClickListener { recomposeSync() }) { setOnClickListener(it) }
                    }
                }
            }
        }

        class A : Component() {
            override fun compose() {
                counter.inc("A")
                // <LinearLayout>
                //     <B id={100} />
                //     <B id={101} />
                //     <B id={102} />
                // </LinearLayout>

                with(CompositionContext.current) {
                    // <LinearLayout>
                    emitView(897, ::LinearLayout, {}) {
                        for (id in 100..102) {
                            // <B key={id} id={id} />
                            emitComponent(878983, id, ::B) {
                                set(id) { this.id = it }
                            }
                        }
                    }
                }
            }
        }


        compose {
            // <A />
            it.emitComponent(123, ::A)
        }.then { _, _, _, activity ->
            // everything got rendered once
            assertEquals(1, counter["A"])
            assertEquals(1, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])

            (activity.findViewById(100) as TextView).performClick()

            // only the clicked view got rerendered
            assertEquals(1, counter["A"])
            assertEquals(2, counter["100"])
            assertEquals(1, counter["101"])
            assertEquals(1, counter["102"])
        }
    }

    @Test
    fun testPreservesTree() = useTreeSlots {
        compose {
            with(it) {
                emitView(123, ::TextView) {
                    set("some text") { text = it }
                }
            }
        }.then { cc, component, _, _ ->
            val before = cc.treeAsString()
            cc.recompose(component)
            val after = cc.treeAsString()
            assertEquals(before, after)
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
            emitView(123, ::LinearLayout, {}) {
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
                with(CompositionContext.current) {
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
                emitView(123, ::LinearLayout, {}) {
                    emitView(123, ::LinearLayout, {}) {
                        emitComponent(123, ::B)
                    }
                    emitView(123, ::LinearLayout, {}) {
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
    fun testReorderingComponents() {
        useTreeSlots {
            var current = listOf(101, 102, 103, 104)

            class Item : Component() {
                var thing: Int = 0
                override fun compose() {
                    with(CompositionContext.current) {
                        emitView(2, ::TextView) {
                            set(thing) { id = it }
                            set("$thing") { text = it }
                        }
                    }
                }
            }

            class Reordering : Component() {
                var things: List<Int> = listOf()
                override fun compose() {
//                <LinearLayout>
//                    for (thing in things) {
//                        <Item key=thing thing />
//                    }
//                </LinearLayout>
                    with(CompositionContext.current) {
                        emitView(0, ::LinearLayout, {}) {
                            for (thing in things) {
                                emitComponent(1, thing, ::Item) {
                                    set(thing) { this.thing = it }
                                }
                            }
                        }
                    }
                }
            }
            compose {
                it.emitComponent(0, ::Reordering) {
                    set(current) { things = it }
                }
            }.then { cc, component, root, activity ->

                assertChildHierarchy(root) {
                    """
                <LinearLayout>
                    <TextView id=101 text='101' />
                    <TextView id=102 text='102' />
                    <TextView id=103 text='103' />
                    <TextView id=104 text='104' />
                </LinearLayout>
                """
                }

                assertSlotTable(cc) {
                    """
                <ROOT><0-0> (open: false, index: 0)
                  Reordering<0-null> (open: false, index: 0)
                    LinearLayout<0-null> (open: false, index: 0)
                      Item<1-101> (open: false, index: 0)
                        TextView<2-null> (open: false, index: 0)
                      Item<1-102> (open: false, index: 1)
                        TextView<2-null> (open: false, index: 1)
                      Item<1-103> (open: false, index: 2)
                        TextView<2-null> (open: false, index: 2)
                      Item<1-104> (open: false, index: 3)
                        TextView<2-null> (open: false, index: 3)
                """
                }

                val el101 = activity.findViewById(101)
                val el102 = activity.findViewById(102)
                val el103 = activity.findViewById(103)
                val el104 = activity.findViewById(104)

                current = listOf(101, 103, 102, 104)

                cc.recompose(component)

                assertChildHierarchy(root) {
                    """
                <LinearLayout>
                    <TextView id=101 text='101' />
                    <TextView id=103 text='103' />
                    <TextView id=102 text='102' />
                    <TextView id=104 text='104' />
                </LinearLayout>
                """
                }

                assertSlotTable(cc) {
                    """
                <ROOT><0-0> (open: false, index: 0)
                  Reordering<0-null> (open: false, index: 0)
                    LinearLayout<0-null> (open: false, index: 0)
                      Item<1-101> (open: false, index: 0)
                        TextView<2-null> (open: false, index: 0)
                      Item<1-103> (open: false, index: 1)
                        TextView<2-null> (open: false, index: 1)
                      Item<1-102> (open: false, index: 2)
                        TextView<2-null> (open: false, index: 2)
                      Item<1-104> (open: false, index: 3)
                        TextView<2-null> (open: false, index: 3)
                """
                }

                val el2101 = activity.findViewById(101)
                val el2102 = activity.findViewById(102)
                val el2103 = activity.findViewById(103)
                val el2104 = activity.findViewById(104)

                assert(el101 === el2101)
                assert(el102 === el2102)
                assert(el103 === el2103)
                assert(el104 === el2104)
            }
        }
    }

    @Test
    fun testReorderingViews() {
        useTreeSlots {
            var current = listOf(101, 102, 103, 104)

            class Reordering : Component() {
                var things: List<Int> = listOf()
                override fun compose() {
//                <LinearLayout>
//                    for (thing in things) {
//                        <TextView key=thing id=thing text="$thing" />
//                    }
//                </LinearLayout>
                    with(CompositionContext.current) {
                        emitView(0, ::LinearLayout, {}) {
                            for (thing in things) {
                                emitView(1, thing, ::TextView) {
                                    set(thing) { id = it }
                                    set("$thing") { text = it }
                                }
                            }
                        }
                    }
                }
            }
            compose {
                it.emitComponent(0, ::Reordering) {
                    set(current) { things = it }
                }
            }.then { cc, component, root, activity ->

                assertChildHierarchy(root) {
                    """
                <LinearLayout>
                    <TextView id=101 text='101' />
                    <TextView id=102 text='102' />
                    <TextView id=103 text='103' />
                    <TextView id=104 text='104' />
                </LinearLayout>
                """
                }

                assertSlotTable(cc) {
                    """
                 <ROOT><0-0> (open: false, index: 0)
                   Reordering<0-null> (open: false, index: 0)
                     LinearLayout<0-null> (open: false, index: 0)
                       TextView<1-101> (open: false, index: 0)
                       TextView<1-102> (open: false, index: 1)
                       TextView<1-103> (open: false, index: 2)
                       TextView<1-104> (open: false, index: 3)
                """
                }

                val el101 = activity.findViewById(101)
                val el102 = activity.findViewById(102)
                val el103 = activity.findViewById(103)
                val el104 = activity.findViewById(104)

                current = listOf(101, 103, 102, 104)

                cc.recompose(component)


                assertSlotTable(cc) {
                    """
                 <ROOT><0-0> (open: false, index: 0)
                   Reordering<0-null> (open: false, index: 0)
                     LinearLayout<0-null> (open: false, index: 0)
                       TextView<1-101> (open: false, index: 0)
                       TextView<1-103> (open: false, index: 1)
                       TextView<1-102> (open: false, index: 2)
                       TextView<1-104> (open: false, index: 3)
                """
                }

                assertChildHierarchy(root) {
                    """
                <LinearLayout>
                    <TextView id=101 text='101' />
                    <TextView id=103 text='103' />
                    <TextView id=102 text='102' />
                    <TextView id=104 text='104' />
                </LinearLayout>
                """
                }

                val el2101 = activity.findViewById(101)
                val el2102 = activity.findViewById(102)
                val el2103 = activity.findViewById(103)
                val el2104 = activity.findViewById(104)

                assert(el101 === el2101)
                assert(el102 === el2102)
                assert(el103 === el2103)
                assert(el104 === el2104)


                current = listOf(102, 103)

                cc.recompose(component)

                assertSlotTable(cc) {
                    """
                 <ROOT><0-0> (open: false, index: 0)
                   Reordering<0-null> (open: false, index: 0)
                     LinearLayout<0-null> (open: false, index: 0)
                       TextView<1-102> (open: false, index: 0)
                       TextView<1-103> (open: false, index: 1)
                """
                }

                assertChildHierarchy(root) {
                    """
                <LinearLayout>
                    <TextView id=102 text='102' />
                    <TextView id=103 text='103' />
                </LinearLayout>
                """
                }

                val el3101 = activity.findViewById(101)
                val el3102 = activity.findViewById(102)
                val el3103 = activity.findViewById(103)
                val el3104 = activity.findViewById(104)

                assert(el3101 === null)
                assert(el3102 === el102)
                assert(el3103 === el103)
                assert(el3104 === null)
            }
        }
    }
}

class Counter {
    private var counts = mutableMapOf<String, Int>()
    fun inc(key: String) = counts.getOrPut(key, { 0 }).let { counts[key] = it + 1 }
    fun reset() {
        counts = mutableMapOf()
    }

    operator fun get(key: String) = counts.getOrDefault(key, 0)
}

// Only use during testing
fun useTreeSlots(body: () -> Unit) {
    val wasUsingNew = CompositionContext.usingNew
    CompositionContext.useOld()
    try {
        body()
    } finally {
        if (wasUsingNew) CompositionContext.useNew()
    }

}

