package com.google.r4a.adapters

import android.view.View
import android.view.ViewGroup

private val LayoutBuilderId = tagKey("LayoutBuilder")
private val intHandlers = HashMap<Int, ViewGroup.LayoutParams.(Int) -> Unit>()
private val floatHandlers = HashMap<Int, ViewGroup.LayoutParams.(Float) -> Unit>()
private val anyHandlers = HashMap<Int, ViewGroup.LayoutParams.(Any) -> Unit>()

private val genDefaultLayoutParams by lazy {
    val method = ViewGroup::class.java.getDeclaredMethod("generateDefaultLayoutParams")
    method.isAccessible = true
    method
}

fun View.getOrAddLayoutBuilderAdapter() = getOrAddAdapter(LayoutBuilderId) { LayoutBuilder() }

fun registerFloatLayoutHandler(attr: Int, setter: ViewGroup.LayoutParams.(Float) -> Unit) {
    floatHandlers[attr] = setter
}

fun registerIntLayoutHandler(attr: Int, setter: ViewGroup.LayoutParams.(Int) -> Unit) {
    intHandlers[attr] = setter
}

fun registerLayoutHandler(attr: Int, setter: ViewGroup.LayoutParams.(Any?) -> Unit) {
    anyHandlers[attr] = setter
}

class LayoutBuilder : ViewAdapter {
    private val intAttrs = HashMap<Int, Int>()
    private val floatAttrs = HashMap<Int, Float>()
    private val anyAttrs = HashMap<Int, Any>()
    private var builtLayoutParams: ViewGroup.LayoutParams? = null
    private var dirty = false

    fun set(attr: Int, value: Int) {
        intAttrs[attr] = value
        dirty = true
    }

    fun set(attr: Int, value: Float) {
        floatAttrs[attr] = value
        dirty = true
    }

    fun set(attr: Int, value: Any) {
        anyAttrs[attr] = value
        dirty = true
    }

    override val id: Int = LayoutBuilderId

    override fun didInsert(view: View, parent: ViewGroup) {
        // do nothing
    }

    override fun didUpdate(view: View, parent: ViewGroup) {
        buildAndSet(view, parent)
    }

    override fun willInsert(view: View, parent: ViewGroup) {
        // on first pass we want to make sure and set the layout params *before* the view gets added
        // to the parent
        buildAndSet(view, parent)
    }

    private fun buildAndSet(view: View, parent: ViewGroup) {
        if (!dirty) return
        dirty = false
        val prev = builtLayoutParams

        val lp = prev ?: genDefaultLayoutParams.invoke(parent) as? ViewGroup.LayoutParams
            ?: error("couldn't create default layout params")

        val intHandlers = intHandlers
        for ((attr, value) in intAttrs) {
            val handler = intHandlers[attr]
            if (handler != null) {
                handler(lp, value)
            }
        }

        val floatHandlers = floatHandlers
        for ((attr, value) in floatAttrs) {
            val handler = floatHandlers[attr]
            if (handler != null) {
                handler(lp, value)
            }
        }

        val anyHandlers = anyHandlers
        for ((attr, value) in anyAttrs) {
            val handler = anyHandlers[attr]
            if (handler != null) {
                handler(lp, value)
            }
        }

        if (prev == null || intAttrs.isNotEmpty() || floatAttrs.isNotEmpty() ||
            anyAttrs.isNotEmpty()) {
            // params have not been set yet, or they've been updated
            view.layoutParams = lp
        }

        intAttrs.clear()
        floatAttrs.clear()
        anyAttrs.clear()

        builtLayoutParams = lp
    }
}