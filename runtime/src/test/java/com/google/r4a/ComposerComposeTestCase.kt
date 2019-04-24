package com.google.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
abstract class ComposerComposeTestCase : TestCase() {
    fun compose(composable: (ViewComposition) -> Unit) = ComposeTest(Root(composable))

    class ComposeTest(val component: Component) {
        fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit) {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.findViewById(ROOT_ID) as ViewGroup
            val cc = R4a.createCompositionContext(root.context, root, component, null)
            cc.runWithCurrent {
                cc.recompose()
                fn(cc, component, root, activity)
            }
        }
    }

    private class Root(var composable: (ViewComposition) -> Unit) : Component() {
        override fun compose() = composable(composer)
    }

    private class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply { id = ROOT_ID })
        }
    }

    companion object {
        val ROOT_ID = 18284847
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
