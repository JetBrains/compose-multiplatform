package com.google.r4a

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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


    fun assertChildHierarchy(root: ViewGroup, getHierarchy: () -> String) {
        val realHierarchy = printChildHierarchy(root)

        TestCase.assertEquals(
            normalizeString(getHierarchy()),
            realHierarchy.trim()
        )
    }

    fun normalizeString(str: String): String {
        val lines = str.split('\n').dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        if (lines.isEmpty()) return ""
        val toRemove = lines.first().takeWhile { it == ' ' }.length
        return lines.joinToString("\n") { it.substring(Math.min(toRemove, it.length)) }
    }


    fun printHierarchy(root: View): String {
        val sb = StringBuilder()
        printView(root, 0, sb)
        return sb.toString()
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