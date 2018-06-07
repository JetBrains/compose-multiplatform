package com.google.r4a

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import junit.framework.TestCase
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class AmbientTests : ComposeTestCase() {
    @Test
    fun testAmbient() {
        val X = Ambient.of<Int>()
        var xval1 = 123
        var xval2 = 987
        val idTv1 = 999
        val idTv2 = 998

        class Bar : Component() {
            var id: Int = 0
            override fun compose() {
                val cc = CompositionContext.current

                // <X.Consumer> ambient ->
                var el0 = cc.start(9834) as? Ambient<Int>.Consumer
                if (el0 == null) {
                    el0 = X.Consumer()
                    cc.setInstance(el0)
                }
                val el0attr1 = { ambient: Int? ->
                    // <TextView text="ambient: $ambient" id={id} />
                    var el1 = cc.start(9734) as? TextView
                    if (el1 == null) {
                        el1 = TextView(cc.context)
                        cc.setInstance(el1)
                    }
                    val el1attr0 = "ambient: $ambient"
                    if (cc.updateAttribute(el1attr0)) {
                        el1.text = el1attr0
                    }

                    val el1attr1 = id
                    if (cc.updateAttribute(el1attr1)) {
                        el1.id = el1attr1
                    }
                    cc.end()
                }
                if (cc.updateAttribute(el0attr1)) {
                    el0.children = el0attr1
                }
                cc.compose()
                // </X.Consumer>
                cc.end()
            }
        }

        class Foo : Component() {
            var id: Int = 0
            override fun compose() {
                val cc = CompositionContext.current

                // <Bar id={id} />
                var el24 = cc.start(984) as? Bar
                if (el24 == null) {
                    el24 = Bar()
                    cc.setInstance(el24)
                }
                val el24attr0 = id
                if (cc.updateAttribute(el24attr0)) {
                    el24.id = el24attr0
                }

                cc.compose()

                cc.end()
            }
        }
        compose { cc ->

            // <X.Provider value={123}>
            var el22 = cc.start(123) as? Ambient<Int>.Provider
            val el0attr0 = xval1
            val el0attr1 = {
                // <Foo id={999} />
                var el23 = cc.start(234) as? Foo
                if (el23 == null) {
                    el23 = Foo()
                    cc.setInstance(el23)
                }
                val el23attr0 = idTv1
                if (cc.updateAttribute(el23attr0)) {
                    el23.id = el23attr0
                }

                cc.compose()

                cc.end()
            }
            if (el22 == null) {
                el22 = X.Provider(el0attr0, el0attr1)
                cc.setInstance(el22)
            }
            if (cc.updateAttribute(el0attr0)) {
                el22.value = el0attr0
            }
            if (cc.updateAttribute(el0attr1)) {
                el22.children = el0attr1
            }

            cc.compose()
            // </X.Provider>
            cc.end()


            // <X.Provider value={123}>
            var el32 = cc.start(123) as? Ambient<Int>.Provider
            val el32attr0 = xval2
            val el32attr1 = {
                // <Foo id={998} />
                var el33 = cc.start(234) as? Foo
                if (el33 == null) {
                    el33 = Foo()
                    cc.setInstance(el33)
                }
                val el33attr0 = idTv2
                if (cc.updateAttribute(el33attr0)) {
                    el33.id = el33attr0
                }

                cc.compose()

                cc.end()
            }
            if (el32 == null) {
                el32 = X.Provider(el32attr0, el32attr1)
                cc.setInstance(el32)
            }
            if (cc.updateAttribute(el32attr0)) {
                el32.value = el32attr0
            }
            if (cc.updateAttribute(el32attr1)) {
                el32.children = el32attr1
            }

            cc.compose()
            // </X.Provider>
            cc.end()


        }.then { cc, component, root, activity ->

            val tv1 = activity.findViewById(idTv1) as TextView
            val tv2 = activity.findViewById(idTv2) as TextView

            assertEquals("ambient: $xval1", tv1.text)
            assertEquals("ambient: $xval2", tv2.text)

            RuntimeEnvironment.getMasterScheduler().pause()

            xval1 = 456
            xval2 = 355

            cc.recompose(component)

            RuntimeEnvironment.getMasterScheduler().unPause()

            assertEquals("ambient: $xval1", tv1.text)
            assertEquals("ambient: $xval2", tv2.text)

            // TODO: once we have pruning/memoization, we should assert test:
            // - no intermediate renders (e.g. Bar shouldn't recompose)
            // - no double renders

            // - overriding providers
        }
    }

    @Test
    fun testDefaultNull() {
        val X = Ambient.of<Int?>()
        val idTv1 = 999
        compose { cc ->
            // <X.Consumer> ambient ->
            var el0 = cc.start(9834) as? Ambient<Int?>.Consumer
            if (el0 == null) {
                el0 = X.Consumer()
                cc.setInstance(el0)
            }
            val el0attr1 = { ambient: Int? ->
                // <TextView text="ambient: $ambient" id={id} />
                var el1 = cc.start(9734) as? TextView
                if (el1 == null) {
                    el1 = TextView(cc.context)
                    cc.setInstance(el1)
                }
                val el1attr0 = "ambient: $ambient"
                if (cc.updateAttribute(el1attr0)) {
                    el1.text = el1attr0
                }

                val el1attr1 = idTv1
                if (cc.updateAttribute(el1attr1)) {
                    el1.id = el1attr1
                }
                cc.end()
            }
            if (cc.updateAttribute(el0attr1)) {
                el0.children = el0attr1
            }
            cc.compose()
            // </X.Consumer>
            cc.end()

        }.then { cc, component, root, activity ->
            val tv1 = activity.findViewById(idTv1) as TextView
            assertEquals("If no defaultFactory provided, null is used", "ambient: null", tv1.text)
        }
    }

    @Test
    fun testDefaultFactory() {
        val X = Ambient.of<Int>(defaultFactory = { 345 })
        val idTv1 = 999
        compose { cc ->
            // <X.Consumer> ambient ->
            var el0 = cc.start(9834) as? Ambient<Int>.Consumer
            if (el0 == null) {
                el0 = X.Consumer()
                cc.setInstance(el0)
            }
            val el0attr1 = { ambient: Int? ->
                // <TextView text="ambient: $ambient" id={id} />
                var el1 = cc.start(9734) as? TextView
                if (el1 == null) {
                    el1 = TextView(cc.context)
                    cc.setInstance(el1)
                }
                val el1attr0 = "ambient: $ambient"
                if (cc.updateAttribute(el1attr0)) {
                    el1.text = el1attr0
                }

                val el1attr1 = idTv1
                if (cc.updateAttribute(el1attr1)) {
                    el1.id = el1attr1
                }
                cc.end()
            }
            if (cc.updateAttribute(el0attr1)) {
                el0.children = el0attr1
            }
            cc.compose()
            // </X.Consumer>
            cc.end()

        }.then { cc, component, root, activity ->
            val tv1 = activity.findViewById(idTv1) as TextView
            assertEquals("If defaultFactory is provided, it should be called", "ambient: 345", tv1.text)
        }
    }


    @Test
    fun testDelegate() {
        val X = Ambient.of<Int>(defaultFactory = { 345 })
        val idTv1 = 999

        class Foo : Component() {
            val x by X
            override fun compose() {
                val cc = CompositionContext.current

                // <TextView text="ambient: $ambient" id={999} />
                var el1 = cc.start(9734) as? TextView
                if (el1 == null) {
                    el1 = TextView(cc.context)
                    cc.setInstance(el1)
                }
                val el1attr0 = "ambient: $x"
                if (cc.updateAttribute(el1attr0)) {
                    el1.text = el1attr0
                }

                val el1attr1 = idTv1
                if (cc.updateAttribute(el1attr1)) {
                    el1.id = el1attr1
                }
                cc.end()
            }
        }
        compose { cc ->
            // <X.Provider>
            var el0 = cc.start(9834) as? Ambient<Int>.Provider
            val el0attr0 = 234
            val el0attr1 = {
                // <Foo />
                var el45 = cc.start(9734) as? Foo
                if (el45 == null) {
                    el45 = Foo()
                    cc.setInstance(el45)
                }
                cc.compose()
                cc.end()
            }
            if (el0 == null) {
                el0 = X.Provider(el0attr0, el0attr1)
                cc.setInstance(el0)
            }
            if (cc.updateAttribute(el0attr0)) {
                el0.value = el0attr0
            }
            if (cc.updateAttribute(el0attr1)) {
                el0.children = el0attr1
            }
            cc.compose()
            // </X.Provider>
            cc.end()

        }.then { _, _, _, activity ->
            val tv1 = activity.findViewById(idTv1) as TextView
            assertEquals("If used under a provider, the provided value should be used", "ambient: 234", tv1.text)
        }


        compose { cc ->
            // <Foo />
            var el46 = cc.start(9734) as? Foo
            if (el46 == null) {
                el46 = Foo()
                cc.setInstance(el46)
            }
            cc.compose()
            cc.end()

        }.then { _, _, _, activity ->
            val tv1 = activity.findViewById(idTv1) as TextView
            assertEquals("If no provider, the defaultFactory should be called", "ambient: 345", tv1.text)
        }
    }
}