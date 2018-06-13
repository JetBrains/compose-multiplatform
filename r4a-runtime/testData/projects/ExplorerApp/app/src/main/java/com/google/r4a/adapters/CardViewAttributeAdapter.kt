package com.google.r4a.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.CardView
import com.google.r4a.adapters.LocalUtils.displayMetrics
import com.google.r4a.adapters.LocalUtils.stringToFloatPx
import com.google.r4a.adapters.LocalUtils.stringToIntPx

object CardViewAttributeAdapter: AttributeAdapter() {

    fun setCardBackgroundColor(view: CardView, color: String) {
        view.setCardBackgroundColor(Color.parseColor(color))
    }

    fun setRadius(view: CardView, radius: Dimension) {
        view.setRadius(radius.toFloatPixels(displayMetrics(view)))
    }

    fun setCardElevation(view: CardView, elevation: Dimension) {
        view.setCardElevation(elevation.toFloatPixels(displayMetrics(view)))
    }

    fun setMaxCardElevation(view: CardView, elevation: Dimension) {
        view.setMaxCardElevation(elevation.toFloatPixels(displayMetrics(view)))
    }

    fun setContentPadding(view: CardView, padding: Int) {
        view.setContentPadding(padding, padding, padding, padding)
    }

    fun setContentPadding(view: CardView, padding: Dimension) {
        setContentPadding(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingHorizontal(view: CardView, padding: Int) {
        view.setContentPadding(padding, view.getContentPaddingTop(), padding, view.getContentPaddingBottom())
    }

    fun setContentPaddingHorizontal(view: CardView, padding: Dimension) {
        setContentPaddingHorizontal(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingVertical(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), padding, view.getContentPaddingRight(), padding)
    }

    fun setContentPaddingVertical(view: CardView, padding: Dimension) {
        setContentPaddingVertical(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingLeft(view: CardView, padding: Int) {
        view.setContentPadding(padding, view.getContentPaddingTop(), view.getContentPaddingRight(), view.getContentPaddingBottom())
    }

    fun setContentPaddingLeft(view: CardView, padding: Dimension) {
        setContentPaddingLeft(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingTop(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), padding, view.getContentPaddingRight(), view.getContentPaddingBottom())
    }

    fun setContentPaddingTop(view: CardView, padding: Dimension) {
        setContentPaddingTop(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingRight(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), view.getContentPaddingTop(), padding, view.getContentPaddingBottom())
    }

    fun setContentPaddingRight(view: CardView, padding: Dimension) {
        setContentPaddingRight(view, padding.toIntPixels(displayMetrics(view)))
    }

    fun setContentPaddingBottom(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), view.getContentPaddingTop(), view.getContentPaddingRight(), padding)
    }

    fun setContentPaddingBottom(view: CardView, padding: Dimension) {
        setContentPaddingBottom(view, padding.toIntPixels(displayMetrics(view)))
    }
}