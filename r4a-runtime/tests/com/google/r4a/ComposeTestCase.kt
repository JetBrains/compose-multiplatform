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
abstract class ComposeTestCase : TestCase() {

    fun CompositionContext.run(fn: () -> Unit) {
        class Foo : Component() {
            override fun compose() {
                fn()
            }
        }
        setInstance(Foo())
    }

    fun CompositionContext.recomposeFromRoot() {
        if (this !is CompositionContextImpl) error("expected CompositionContextImpl")
        recompose(ROOT_SLOT.instance as Component)
    }

    fun CompositionContext.treeAsString(): String {
        if (this !is CompositionContextImpl) error("expected CompositionContextImpl")
        val sb = StringBuilder()
        ROOT_SLOT.treeAsString(0, sb)
        return sb.toString()
    }

    internal fun Slot.treeAsString(indent: Int, sb: StringBuilder) {
        val whiteSpace = " ".repeat(indent)

        sb.appendln("$whiteSpace$this")

        val child = child
        if (child != null) {
            child.treeAsString(indent + 2, sb)
        }

        val nextSibling = nextSibling
        if (nextSibling != null) {
            nextSibling.treeAsString(indent, sb)
        }
    }

    fun compose(composable: (CompositionContext) -> Unit) = ComposeTest(Root(composable))

    fun compose(component: Component) = ComposeTest(component)

    fun withRootView(fn: (ViewGroup) -> Unit) {
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.create().get()
        val root = activity.findViewById(ROOT_ID) as ViewGroup
        fn(root)
    }

    fun withContext(fn: (CompositionContext) -> Unit) = withRootView { root ->
        val result = CompositionContextImpl()
        result.ROOT_CONTAINER.view = root
        result.context = root.context
        val prev = CompositionContext.current
        CompositionContext.current = result
        fn(result)
        CompositionContext.current = prev

    }

    class ComposeTest(val component: Component) {
        fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit) {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.findViewById(ROOT_ID) as ViewGroup
            val cc = CompositionContext.create(root.context, root, component)
            val prev = CompositionContext.current
            CompositionContext.current = cc
            cc.setInstance(component)
            cc.compose()
            cc.end()
            fn(cc, component, root, activity)
            CompositionContext.current = prev
        }
    }

    companion object {
        val ROOT_ID = 18284847
    }

    private class Root(var composable: (CompositionContext) -> Unit) : Component() {
        override fun compose() = composable(CompositionContext.current)
    }

    private class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply { id = ROOT_ID })
        }
    }
}