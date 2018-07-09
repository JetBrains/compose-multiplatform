package com.google.r4a.examples.explorerapp.common.adapters

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.v7.widget.CardView
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.r4a.adapters.Dimension
import com.google.r4a.adapters.setMinimumHeight

internal val View.metrics: DisplayMetrics get() = resources.displayMetrics

fun CardView.setCardBackgroundColor(color: String) {
    setCardBackgroundColor(Color.parseColor(color))
}

fun CardView.setRadius(radius: Dimension) {
    setRadius(radius.toFloatPixels(metrics))
}

fun CardView.setCardElevation(elevation: Dimension) {
    cardElevation = elevation.toFloatPixels(metrics)
}

fun CardView.setMaxCardElevation(elevation: Dimension) {
    maxCardElevation = elevation.toFloatPixels(metrics)
}

fun CardView.setContentPadding(padding: Int) = setContentPadding(padding, padding, padding, padding)

fun CardView.setContentPadding(padding: Dimension) = setContentPadding(padding.toIntPixels(metrics))

fun CardView.setContentPaddingHorizontal(padding: Int) = setContentPadding(padding, contentPaddingTop, padding, contentPaddingBottom)

fun CardView.setContentPaddingHorizontal(padding: Dimension) = setContentPaddingHorizontal(padding.toIntPixels(metrics))

fun CardView.setContentPaddingVertical(padding: Int) = setContentPadding(contentPaddingLeft, padding, contentPaddingRight, padding)

fun CardView.setContentPaddingVertical(padding: Dimension) = setContentPaddingVertical(padding.toIntPixels(metrics))

fun CardView.setContentPaddingLeft(padding: Int) = setContentPadding(padding, contentPaddingTop, contentPaddingRight, contentPaddingBottom)

fun CardView.setContentPaddingLeft(padding: Dimension) = setContentPaddingLeft(padding.toIntPixels(metrics))

fun CardView.setContentPaddingTop(padding: Int) = setContentPadding(contentPaddingLeft, padding, contentPaddingRight, contentPaddingBottom)

fun CardView.setContentPaddingTop(padding: Dimension) = setContentPaddingTop(padding.toIntPixels(metrics))

fun CardView.setContentPaddingRight(padding: Int) = setContentPadding(contentPaddingLeft, contentPaddingTop, padding, contentPaddingBottom)

fun CardView.setContentPaddingRight(padding: Dimension) = setContentPaddingRight(padding.toIntPixels(metrics))

fun CardView.setContentPaddingBottom(padding: Int) = setContentPadding(contentPaddingLeft, contentPaddingTop, contentPaddingRight, padding)

fun CardView.setContentPaddingBottom(padding: Dimension) = setContentPaddingBottom(padding.toIntPixels(metrics))

private val typedValue = TypedValue()

private inline fun <T> withResId(context: Context, resId: Int, block: (TypedValue) -> T): T {
    if (context.theme.resolveAttribute(resId, typedValue, true)) {
        return block(typedValue)
    } else {
//        try {
            context.resources.getValue(resId, typedValue, true)
//        } catch (e: Resources.NotFoundException) {
//            // TODO(lmr): figure out the right thing to do here
//            return
//        }
        return block(typedValue)
    }
}

/**
 * We want to be able to resolve resource attributes pretty close to how AAPT does it... Someone should be able
 * to pass in R.attr.foo as well as R.drawable.foo. Even harder is that we may want to also allow color ints and color
 * resource ints: R.color.foo and Color.BLUE. This logic isn't complete yet, but I'm working on it.
 */
fun getDrawableFromResId(context: Context, resId: Int): Drawable {
    return withResId(context, resId) {
        when (it.type) {
            // apparently this means drawable???
            TypedValue.TYPE_STRING -> {
                return context.resources.getDrawable(typedValue.resourceId, context.theme)
            }
            else -> error("unrecognized type: ${it.type}")
        }
    }
}

// TODO(lmr): should we have 0 "reset" it? should we rename this to foregroundResource so we support colors??
fun CardView.setForeground(resId: Int) {
    foreground = getDrawableFromResId(context, resId)
}

fun View.setForeground(resId: Int) {
    foreground = getDrawableFromResId(context, resId)
}

// TODO(lmr): is this equivalent to setBackgroundResource or are we handling attributes special here?
fun CardView.setBackground(resId: Int) {
    background = getDrawableFromResId(context, resId)
}


// TODO(lmr): why are these max/minimum instead of max/min?
fun ImageView.setMaxHeight(maxHeight: Dimension) = setMaxHeight(maxHeight.toIntPixels(metrics))
fun ImageView.setMinimumHeight(minimumHeight: Dimension) = setMinimumHeight(minimumHeight.toIntPixels(metrics))
fun ImageView.setMaxWidth(maxWidth: Dimension) = setMaxWidth(maxWidth.toIntPixels(metrics))
fun ImageView.setMinimumWidth(minimumWidth: Dimension) = setMinimumWidth(minimumWidth.toIntPixels(metrics))