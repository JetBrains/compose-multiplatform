@file:Suppress("unused", "LocalVariableName", "UnnecessaryVariable")

package com.google.r4a

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
    fun testNativeViewWithAttributes() = compose { cc ->

        // <TextView id={456} text="some text" />
        var el = cc.start(123) as? TextView
        if (el == null) {
            el = TextView(cc.context)
            cc.setInstance(el)
        }
        val attr_0 = 456
        if (cc.updateAttribute(attr_0)) {
            el.id = attr_0
        }

        val attr_1 = "some text"
        if (cc.updateAttribute(attr_1)) {
            el.text = attr_1
        }
        cc.end()
    }.then { _, _, root, activity ->
        assertEquals(1, root.childCount)

        val tv = activity.findViewById(456) as TextView
        assertEquals("some text", tv.text)

        assertEquals(tv, root.getChildAt(0))
    }

    @Test
    fun testSlotKeyChangeCausesRecreate() {
        var i = 1

        compose { cc ->
            // this should cause the textview to get recreated on every compose
            i++

            // <TextView id={456} text="some text" />
            var el = cc.start(i) as? TextView
            if (el == null) {
                el = TextView(cc.context)
                cc.setInstance(el)
            }
            val attr_0 = 456
            if (cc.updateAttribute(attr_0)) {
                el.id = attr_0
            }

            val attr_1 = "some text"
            if (cc.updateAttribute(attr_1)) {
                el.text = attr_1
            }
            cc.end()
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
        compose { cc ->
            // this should cause the textview to get recreated on every compose

            // <LinearLayout id={345}>
            var el1 = cc.start(100) as? LinearLayout
            if (el1 == null) {
                el1 = LinearLayout(cc.context)
                cc.setInstance(el1)
            }
            val el1_attr_0 = 345
            if (cc.updateAttribute(el1_attr_0)) {
                el1.id = el1_attr_0
            }

            // <TextView id={456} text="some text" />
            var el2 = cc.start(101) as? TextView
            if (el2 == null) {
                el2 = TextView(cc.context)
                cc.setInstance(el2)
            }
            val el2_attr_0 = 456
            if (cc.updateAttribute(el2_attr_0)) {
                el2.id = el2_attr_0
            }

            val el2_attr_1 = "some text"
            if (cc.updateAttribute(el2_attr_1)) {
                el2.text = el2_attr_1
            }
            cc.end()

            // <TextView id={567} text="some text" />
            var el3 = cc.start(102) as? TextView
            if (el3 == null) {
                el3 = TextView(cc.context)
                cc.setInstance(el3)
            }
            val el3_attr_0 = 567
            if (cc.updateAttribute(el3_attr_0)) {
                el3.id = el3_attr_0
            }

            val el3_attr_1 = "some text"
            if (cc.updateAttribute(el3_attr_1)) {
                el3.text = el3_attr_1
            }
            cc.end()

            // </LinearLayout>
            cc.end()
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
        compose { cc ->
            // this should cause the textview to get recreated on every compose

            // <LinearLayout id={345}>
            var el1 = cc.start(100) as? LinearLayout
            if (el1 == null) {
                el1 = LinearLayout(cc.context)
                cc.setInstance(el1)
            }
            val el1_attr_0 = 345
            if (cc.updateAttribute(el1_attr_0)) {
                el1.id = el1_attr_0
            }

            for (i in items) {
                // <TextView id={456} text="some text" />
                var el2 = cc.start(101, i) as? TextView
                if (el2 == null) {
                    el2 = TextView(cc.context)
                    cc.setInstance(el2)
                }

                val el2_attr_0 = "some text $i"
                if (cc.updateAttribute(el2_attr_0)) {
                    el2.text = el2_attr_0
                }
                cc.end()
            }

            // </LinearLayout>
            cc.end()
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

                // <TextView id={id} onClickListener={{ recomposeSync() }} />
                val cc = CompositionContext.current
                var el = cc.start(24) as? TextView
                if (el == null) {
                    el = TextView(cc.context)
                    cc.setInstance(el)
                }
                val attr0 = id
                if (cc.updateAttribute(attr0)) {
                    el.id = attr0
                }
                val attr1 = View.OnClickListener { recompose() }
                if (cc.updateAttribute(attr1)) {
                    el.setOnClickListener(attr1)
                }
                cc.end()
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


                val cc = CompositionContext.current
                // <LinearLayout>
                var el0 = cc.start(897) as? LinearLayout
                if (el0 == null) {
                    el0 = LinearLayout(cc.context)
                    cc.setInstance(el0)
                }
                val el0attr1 = View.OnClickListener { recompose() }
                if (cc.updateAttribute(el0attr1)) {
                    el0.setOnClickListener(el0attr1)
                }
                val el0attr2 = 99
                if (cc.updateAttribute(el0attr2)) {
                    el0.id = el0attr2
                }

                for (id in 100..102) {
                    // <B key={id} id={id} />
                    var el1 = cc.start(878983, id) as? B
                    if (el1 == null) {
                        el1 = B()
                        cc.setInstance(el1)
                    }
                    val el1attr1 = id
                    if (cc.updateAttribute(el1attr1)) {
                        el1.id = el1attr1
                    }
                    cc.compose()
                    cc.end()
                }
                // </LinearLayout>
                cc.end()
            }
        }


        compose { cc ->
            // <A />
            var el1 = cc.start(123) as? A
            if (el1 == null) {
                el1 = A()
                cc.setInstance(el1!!)
            }
            cc.compose()
            cc.end()
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

                // <TextView id={id} onClickListener={{ recomposeSync() }} />
                val cc = CompositionContext.current
                var el = cc.start(24) as? TextView
                if (el == null) {
                    el = TextView(cc.context)
                    cc.setInstance(el)
                }
                val attr0 = id
                if (cc.updateAttribute(attr0)) {
                    el.id = attr0
                }
                val attr1 = View.OnClickListener { recomposeSync() }
                if (cc.updateAttribute(attr1)) {
                    el.setOnClickListener(attr1)
                }
                cc.end()
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


                val cc = CompositionContext.current
                // <LinearLayout>
                var el0 = cc.start(897) as? LinearLayout
                if (el0 == null) {
                    el0 = LinearLayout(cc.context)
                    cc.setInstance(el0)
                }

                for (id in 100..102) {
                    // <B key={id} id={id} />
                    var el1 = cc.start(878983, id) as? B
                    if (el1 == null) {
                        el1 = B()
                        cc.setInstance(el1)
                    }
                    val attr1 = id
                    if (cc.updateAttribute(attr1)) {
                        el1.id = attr1
                    }
                    cc.compose()
                    cc.end()
                }
                // </LinearLayout>
                cc.end()
            }
        }


        compose { cc ->
            // <A />
            var el1 = cc.start(123) as? A
            if (el1 == null) {
                el1 = A()
                cc.setInstance(el1!!)
            }
            cc.compose()
            cc.end()
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
    fun testPreservesTree() = compose { cc ->
        var el = cc.start(123) as? TextView
        if (el == null) {
            el = TextView(cc.context)
            cc.setInstance(el)
        }
        val attr_0 = "some text"
        if (cc.updateAttribute(attr_0)) {
            el.text = attr_0
        }
        cc.end()
    }.then { cc, component, _, _ ->
        val before = cc.treeAsString()
        cc.recompose(component)
        val after = cc.treeAsString()
        assertEquals(before, after)
    }



    @Test
    fun testCorrectViewTree() = compose { cc ->
        var el0 = cc.start(123) as? LinearLayout
        if (el0 == null) {
            el0 = LinearLayout(cc.context)
            cc.setInstance(el0)
        }

        var el0x0 = cc.start(123) as? LinearLayout
        if (el0x0 == null) {
            el0x0 = LinearLayout(cc.context)
            cc.setInstance(el0x0)
        }
        cc.end()

        var el0x1 = cc.start(123) as? LinearLayout
        if (el0x1 == null) {
            el0x1 = LinearLayout(cc.context)
            cc.setInstance(el0x1)
        }
        cc.end()

        cc.end()

        var el1 = cc.start(123) as? LinearLayout
        if (el1 == null) {
            el1 = LinearLayout(cc.context)
            cc.setInstance(el1)
        }
        cc.end()

    }.then { cc, component, root, activity ->
        assertEquals(2, root.childCount)
        val el0 = root.getChildAt(0) as ViewGroup
        val el1 = root.getChildAt(1) as ViewGroup
        assertEquals(2, el0.childCount)
        assertEquals(0, el1.childCount)
        val el0x0 = el0.getChildAt(0) as ViewGroup
        val el0x1 = el0.getChildAt(1) as ViewGroup
        assertEquals(0, el0x0.childCount)
        assertEquals(0, el0x1.childCount)
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
