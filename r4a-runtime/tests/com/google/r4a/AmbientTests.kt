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
                with(CompositionContext.current) {

                    // <X.Consumer> ambient ->
                    consumeAmbient(X) { ambient ->
                        // <TextView text="ambient: $ambient" id={id} />
                        emitView(9734, ::TextView) {
                            set("ambient: $ambient") { text = it }
                            set(id) { id = it }
                        }
                    }
                }
            }
        }

        class Foo : Component() {
            var id: Int = 0
            override fun compose() {
                with(CompositionContext.current) {
                    emitComponent(984, ::Bar) {
                        set(id) { id = it }
                    }
                }
            }
        }
        compose { cc ->
            with(cc) {
                // <X.Provider value={xval1}>
                provideAmbient(X, xval1) {
                    // <Foo id={999} />
                    emitComponent(234, ::Foo) {
                        set(999) { id = it }
                    }
                }
                // <X.Provider value={xval2}>
                provideAmbient(X, xval2) {
                    // <Foo id={998} />
                    emitComponent(234, ::Foo) {
                        set(998) { id = it }
                    }
                }
            }
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
            with(cc) {
                // <X.Consumer> ambient ->
                consumeAmbient(X) { ambient ->
                    // <TextView text="ambient: $ambient" id={id} />
                    emitView(9734, ::TextView) {
                        set("ambient: $ambient") { text = it }
                        set(idTv1) { id = it }
                    }
                }
            }
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
            with(cc) {
                consumeAmbient(X) { ambient ->
                    // <TextView text="ambient: $ambient" id={id} />
                    emitView(9734, ::TextView) {
                        set("ambient: $ambient") { text = it }
                        set(idTv1) { id = it }
                    }
                }
            }
        }.then { cc, component, root, activity ->
            val tv1 = activity.findViewById(idTv1) as TextView
            assertEquals("If defaultFactory is provided, it should be called", "ambient: 345", tv1.text)
        }
    }
}