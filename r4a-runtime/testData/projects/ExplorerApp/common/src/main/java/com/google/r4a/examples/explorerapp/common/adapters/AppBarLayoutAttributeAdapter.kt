package com.google.r4a.examples.explorerapp.common.adapters

import android.support.design.widget.AppBarLayout
import android.view.View
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

    registerIntLayoutHandler(android.support.design.R.styleable.AppBarLayout_Layout_layout_scrollFlags) {
        when (this) {
            is AppBarLayout.LayoutParams -> scrollFlags = it
            else -> error("scrollFlags not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registered = true
}

fun View.setLayoutAppBarScrollFlags(flags: Int) = layoutBuilder.set(android.support.design.R.styleable.AppBarLayout_Layout_layout_scrollFlags, flags)
