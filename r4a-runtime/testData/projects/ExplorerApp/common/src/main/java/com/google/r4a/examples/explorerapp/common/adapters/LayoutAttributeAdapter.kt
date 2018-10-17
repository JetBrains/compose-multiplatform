package com.google.r4a.examples.explorerapp.common.adapters

import android.support.annotation.DimenRes
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.DrawerLayout
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.r4a.adapters.Dimension
import com.google.r4a.adapters.ViewAdapter
import com.google.r4a.adapters.getOrAddAdapter

internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

class LayoutBuilder: ViewAdapter {
    companion object {
        val id = tagKey("LayoutBuilder")
        val intHandlers = HashMap<Int, (ViewGroup.LayoutParams, Int) -> Unit>()
        val floatHandlers = HashMap<Int, (ViewGroup.LayoutParams, Float) -> Unit>()
        val anyHandlers = HashMap<Int, (ViewGroup.LayoutParams, Any) -> Unit>()
        fun registerFloat(attr: Int, setter: (ViewGroup.LayoutParams, Float) -> Unit) {
            floatHandlers[attr] = setter
        }
        fun registerInt(attr: Int, setter: (ViewGroup.LayoutParams, Int) -> Unit) {
            intHandlers[attr] = setter
        }
        fun register(attr: Int, setter: (ViewGroup.LayoutParams, Any) -> Unit) {
            anyHandlers[attr] = setter
        }

        private val genDefaultLayoutParams by lazy {
            val method = ViewGroup::class.java.getDeclaredMethod("generateDefaultLayoutParams")
            method.isAccessible = true
            method
        }
    }

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

    override val id: Int = LayoutBuilder.id

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

    fun buildAndSet(view: View, parent: ViewGroup) {
        if (!dirty) return
        dirty = false
        val prev = builtLayoutParams

        val lp = prev ?: genDefaultLayoutParams.invoke(parent) as? ViewGroup.LayoutParams ?: error("couldn't create default layout params")

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

        if (prev == null || intAttrs.isNotEmpty() || floatAttrs.isNotEmpty() || anyAttrs.isNotEmpty()) {
            // params have not been set yet, or they've been updated
            view.layoutParams = lp
        }

        intAttrs.clear()
        floatAttrs.clear()
        anyAttrs.clear()

        builtLayoutParams = lp

    }
}

private var registered = false

fun registerSetters() {
    registered = true
    LayoutBuilder.registerInt(android.R.attr.layout_width) { p, v -> p.width = v }
    LayoutBuilder.registerInt(android.R.attr.layout_height) { p, v -> p.height = v }
    LayoutBuilder.registerFloat(android.R.attr.layout_weight) { p, v -> when (p) {
        is LinearLayout.LayoutParams -> {
            p.weight = v
        }
        else -> error("weight not possible to be set on ${p::class.java.simpleName}")
    }}
    LayoutBuilder.registerInt(android.R.attr.layout_gravity) { p, v -> when (p) {
        is LinearLayout.LayoutParams -> {
            p.gravity = v
        }
        is FrameLayout.LayoutParams -> {
            p.gravity = v
        }
        is DrawerLayout.LayoutParams -> {
            p.gravity = v
        }
        else -> error("gravity not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_margin) { p, v -> when (p) {
        is ViewGroup.MarginLayoutParams -> {
            p.setMargins(v, v, v, v)
        }
        else -> error("margin not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_marginTop) { p, v -> when (p) {
        is ViewGroup.MarginLayoutParams -> {
            p.topMargin = v
        }
        else -> error("marginTop not possible to be set on ${p::class.java.simpleName}")
    }}
    LayoutBuilder.registerInt(android.R.attr.layout_marginLeft) { p, v -> when (p) {
        is ViewGroup.MarginLayoutParams -> {
            p.leftMargin = v
        }
        else -> error("marginLeft not possible to be set on ${p::class.java.simpleName}")
    }}
    LayoutBuilder.registerInt(android.R.attr.layout_marginBottom) { p, v -> when (p) {
        is ViewGroup.MarginLayoutParams -> {
            p.bottomMargin = v
        }
        else -> error("marginBottom not possible to be set on ${p::class.java.simpleName}")
    }}
    LayoutBuilder.registerInt(android.R.attr.layout_marginRight) { p, v -> when (p) {
        is ViewGroup.MarginLayoutParams -> {
            p.rightMargin = v
        }
        else -> error("marginRight not possible to be set on ${p::class.java.simpleName}")
    }}
    LayoutBuilder.registerInt(android.support.design.R.styleable.AppBarLayout_Layout_layout_scrollFlags) { p, v -> when (p) {
        is AppBarLayout.LayoutParams -> {
            p.scrollFlags = v
        }
        else -> error("scrollFlags not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_alignParentBottom) { p, v -> when (p) {
        is RelativeLayout.LayoutParams -> {
            if (v != 0) {
                p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            } else {
                p.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
        }
        else -> error("scrollFlags not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_alignParentEnd) { p, v -> when (p) {
        is RelativeLayout.LayoutParams -> {
            if (v != 0) {
                p.addRule(RelativeLayout.ALIGN_PARENT_END)
            } else {
                p.removeRule(RelativeLayout.ALIGN_PARENT_END)
            }
        }
        else -> error("scrollFlags not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_alignParentRight) { p, v -> when (p) {
        is RelativeLayout.LayoutParams -> {
            if (v != 0) {
                p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            } else {
                p.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }
        }
        else -> error("scrollFlags not possible to be set on ${p::class.java.simpleName}")
    }}

    LayoutBuilder.registerInt(android.R.attr.layout_below) { p, v -> when (p) {
        is RelativeLayout.LayoutParams -> {
            if (v != 0) {
                p.addRule(RelativeLayout.BELOW, v)
            } else {
                p.removeRule(RelativeLayout.BELOW)
            }
        }
        else -> error("scrollFlags not possible to be set on ${p::class.java.simpleName}")
    }}
}

private fun View.getLayoutBuilder(): LayoutBuilder {
    if (!registered) {
        registerSetters()
    }
    return getOrAddAdapter(LayoutBuilder.id) { LayoutBuilder() }
}

// TODO(jdemeulenaere): Remove this and use extension method from ViewAttributeAdapter.kt once this file is moved in
// same project.
internal fun @receiver:DimenRes Int.assertDimensionRes(): Int {
    assert((this ushr 28) == 0x7f)
    return this
}

private fun View.setPixelLayoutWidth(width: Int) = getLayoutBuilder().set(android.R.attr.layout_width, width)
fun View.setLayoutWidth(width: Int) {
    if (width == -1 || width == -2) {
        // It is either MATCH_PARENT, FILL_PARENT or WRAP_CONTENT
        setPixelLayoutWidth(width)
    } else {
        // It is a dimension resource ID.
        setPixelLayoutWidth(resources.getDimensionPixelSize(width.assertDimensionRes()))
    }
}
fun View.setLayoutWidth(dim: Dimension) = setPixelLayoutWidth(dim.toIntPixels(metrics))

private fun View.setPixelLayoutHeight(height: Int) = getLayoutBuilder().set(android.R.attr.layout_height, height)
fun View.setLayoutHeight(height: Int) {
    if (height == -1 || height == -2) {
        setPixelLayoutHeight(height)
    } else {
        setPixelLayoutHeight(resources.getDimensionPixelSize(height.assertDimensionRes()))
    }
}
fun View.setLayoutHeight(dim: Dimension) = setPixelLayoutHeight(dim.toIntPixels(metrics))

fun View.setLayoutGravity(gravity: Int) = getLayoutBuilder().set(android.R.attr.layout_gravity, gravity)

private fun View.setPixelMarginTop(pixels: Int) = getLayoutBuilder().set(android.R.attr.layout_marginTop, pixels)
private fun View.setPixelMarginLeft(pixels: Int) = getLayoutBuilder().set(android.R.attr.layout_marginLeft, pixels)
private fun View.setPixelMarginBottom(pixels: Int) = getLayoutBuilder().set(android.R.attr.layout_marginBottom, pixels)
private fun View.setPixelMarginRight(pixels: Int) = getLayoutBuilder().set(android.R.attr.layout_marginRight, pixels)

fun View.setMarginTop(@DimenRes resId: Int) = setPixelMarginTop(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginLeft(@DimenRes resId: Int) = setPixelMarginLeft(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginBottom(@DimenRes resId: Int) = setPixelMarginBottom(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginRight(@DimenRes resId: Int) = setPixelMarginRight(resources.getDimensionPixelSize(resId.assertDimensionRes()))

fun View.setMarginTop(dim: Dimension) = setPixelMarginTop(dim.toIntPixels(metrics))
fun View.setMarginLeft(dim: Dimension) = setPixelMarginLeft(dim.toIntPixels(metrics))
fun View.setMarginBottom(dim: Dimension) = setPixelMarginBottom(dim.toIntPixels(metrics))
fun View.setMarginRight(dim: Dimension) = setPixelMarginRight(dim.toIntPixels(metrics))

private fun View.setPixelMarginHorizontal(pixels: Int) {
    setMarginLeft(pixels)
    setMarginRight(pixels)
}

private fun View.setPixelMarginVertical(pixels: Int) {
    setMarginTop(pixels)
    setMarginBottom(pixels)
}

fun View.setMarginHorizontal(@DimenRes resId: Int) = setPixelMarginHorizontal(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginVertical(@DimenRes resId: Int) = setPixelMarginVertical(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginHorizontal(dim: Dimension) = setPixelMarginHorizontal(dim.toIntPixels(metrics))
fun View.setMarginVertical(dim: Dimension) = setPixelMarginVertical(dim.toIntPixels(metrics))

fun View.setLayoutAppBarScrollFlags(flags: Int) = getLayoutBuilder().set(android.support.design.R.styleable.AppBarLayout_Layout_layout_scrollFlags, flags)
fun View.setLayoutAlignParentBottom(value: Boolean) = getLayoutBuilder().set(android.R.attr.layout_alignParentBottom, if (value) 1 else 0)
fun View.setLayoutAlignParentRight(value: Boolean) = getLayoutBuilder().set(android.R.attr.layout_alignParentRight, if (value) 1 else 0)
fun View.setLayoutBelow(resId: Int) = getLayoutBuilder().set(android.R.attr.layout_alignParentBottom, resId)
fun View.setLayoutWeight(weight: Float) = getLayoutBuilder().set(android.R.attr.layout_weight, weight)
