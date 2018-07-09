package com.google.r4a.examples.explorerapp.common.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v7.widget.Toolbar
import android.widget.Spinner

import com.google.r4a.CompositionContext
import com.google.r4a.adapters.Dimension

fun Toolbar.setTitleTextColor(color: String) {
    setTitleTextColor(Color.parseColor(color))
}

fun Toolbar.setLogo(resId: Int) {
    logo = getDrawableFromResId(context, resId)
}

fun Toolbar.setLogoColor(color: String) {
    logo.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)
}

fun Toolbar.setNavigationIconColor(color: String) {
    navigationIcon?.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)
}

fun Toolbar.setOverflowIcon(resId: Int) {
    overflowIcon = getDrawableFromResId(context, resId)
}


fun Toolbar.setMenu(resId: Int) {
    inflateMenu(resId)
}

fun Toolbar.setContentInsetLeft(inset: Dimension) = setContentInsetsAbsolute(inset.toIntPixels(metrics), contentInsetRight)
fun Toolbar.setContentInsetRight(inset: Dimension) = setContentInsetsAbsolute(contentInsetLeft, inset.toIntPixels(metrics))
fun Toolbar.setContentInsetStart(inset: Dimension) = setContentInsetsRelative(inset.toIntPixels(metrics), contentInsetEnd)
fun Toolbar.setContentInsetEnd(inset: Dimension) = setContentInsetsRelative(contentInsetStart, inset.toIntPixels(metrics))

//@RequiresApi(Build.VERSION_CODES.N)
fun Toolbar.setTitleMarginBottom(margin: Dimension) = setTitleMarginBottom(margin.toIntPixels(metrics))

//@RequiresApi(Build.VERSION_CODES.N)
fun Toolbar.setTitleMarginTop(margin: Dimension) = setTitleMarginTop(margin.toIntPixels(metrics))

//@RequiresApi(Build.VERSION_CODES.N)
fun Toolbar.setTitleMarginEnd(margin: Dimension) = setTitleMarginEnd(margin.toIntPixels(metrics))

//@RequiresApi(Build.VERSION_CODES.N)
fun Toolbar.setTitleMarginStart(margin: Dimension) = setTitleMarginStart(margin.toIntPixels(metrics))


fun Toolbar.setIsActionBar(isActionBar: Boolean) {
    // TODO(lmr): Using ambients in a setter like this is not great, since we are don't really have any way of
    // ensuring that it's getting the right ones based on it's scope. If we change ambients to also pass though
    // android Context objects, I think we can do this in a more reliable way that feels better.
    val activity = CompositionContext.current.getAmbient(Ambients.Activity) ?: return
    if (isActionBar) {
        activity.setSupportActionBar(this)
    }
}

//fun Spinner.setColorThing(x: Int) {
//    text
//}

// currentContentInset dimensions
