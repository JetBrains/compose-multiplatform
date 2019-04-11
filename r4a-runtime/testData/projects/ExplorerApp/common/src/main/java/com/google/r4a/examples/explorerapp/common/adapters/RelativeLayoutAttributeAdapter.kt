package com.google.r4a.examples.explorerapp.common.adapters

import android.view.View
import android.widget.RelativeLayout
import androidx.ui.androidview.adapters.LayoutBuilder
import androidx.ui.androidview.adapters.getOrAddLayoutBuilderAdapter
import androidx.ui.androidview.adapters.registerIntLayoutHandler


private var registered = false
private val View.layoutBuilder: LayoutBuilder
    get() {
        if (!registered) {
            registerHandlers()
        }
        return getOrAddLayoutBuilderAdapter()
    }

private fun registerHandlers() {

    fun registerRelativeLayoutRule(attrId: Int, rule: Int, attrName: String) {
        registerIntLayoutHandler(attrId) {
            when (this) {
                is RelativeLayout.LayoutParams -> {
                    if (it != 0) {
                        addRule(rule)
                    } else {
                        removeRule(rule)
                    }
                }
                else -> error("$attrName not possible to be set on ${this::class.java.simpleName}")
            }
        }
    }

    fun registerRelativeLayoutIdRule(attrId: Int, rule: Int, attrName: String) {
        registerIntLayoutHandler(attrId) {
            when (this) {
                is RelativeLayout.LayoutParams -> {
                    if (it != 0) {
                        addRule(rule, it)
                    } else {
                        removeRule(rule)
                    }
                }
                else -> error("$attrName not possible to be set on ${this::class.java.simpleName}")
            }
        }
    }

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentTop,
        RelativeLayout.ALIGN_PARENT_TOP,
        "layoutAlignParentTop"
    )

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentBottom,
        RelativeLayout.ALIGN_PARENT_BOTTOM,
        "layoutAlignParentBottom"
    )

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentRight,
        RelativeLayout.ALIGN_PARENT_RIGHT,
        "layoutAlignParentRight"
    )

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentLeft,
        RelativeLayout.ALIGN_PARENT_LEFT,
        "layoutAlignParentLeft"
    )

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentEnd,
        RelativeLayout.ALIGN_PARENT_END,
        "layoutAlignParentEnd"
    )

    registerRelativeLayoutRule(
        android.R.attr.layout_alignParentStart,
        RelativeLayout.ALIGN_PARENT_START,
        "layoutAlignParentStart"
    )

    registerRelativeLayoutIdRule(
        android.R.attr.layout_below,
        RelativeLayout.BELOW,
        "layoutBelow"
    )
    registered = true
}

fun View.setLayoutAlignParentTop(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentTop, if (value) 1 else 0)
fun View.setLayoutAlignParentBottom(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentBottom, if (value) 1 else 0)
fun View.setLayoutAlignParentLeft(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentLeft, if (value) 1 else 0)
fun View.setLayoutAlignParentRight(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentRight, if (value) 1 else 0)
fun View.setLayoutAlignParentStart(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentStart, if (value) 1 else 0)
fun View.setLayoutAlignParentEnd(value: Boolean) = layoutBuilder.set(android.R.attr.layout_alignParentEnd, if (value) 1 else 0)
fun View.setLayoutBelow(resId: Int) = layoutBuilder.set(android.R.attr.layout_below, resId)
