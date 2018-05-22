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

    fun setRadius(view: CardView, radius: String) {
        view.setRadius(stringToFloatPx(radius, displayMetrics(view)))
    }

    fun setCardElevation(view: CardView, elevation: String) {
        view.setCardElevation(stringToFloatPx(elevation, displayMetrics(view)))
    }

    fun setMaxCardElevation(view: CardView, elevation: String) {
        view.setMaxCardElevation(stringToFloatPx(elevation, displayMetrics(view)))
    }

    fun setContentPadding(view: CardView, padding: Int) {
        view.setContentPadding(padding, padding, padding, padding)
    }

    fun setContentPadding(view: CardView, padding: String) {
        setContentPadding(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingHorizontal(view: CardView, padding: Int) {
        view.setContentPadding(padding, view.getContentPaddingTop(), padding, view.getContentPaddingBottom())
    }

    fun setContentPaddingHorizontal(view: CardView, padding: String) {
        setContentPaddingHorizontal(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingVertical(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), padding, view.getContentPaddingRight(), padding)
    }

    fun setContentPaddingVertical(view: CardView, padding: String) {
        setContentPaddingVertical(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingLeft(view: CardView, padding: Int) {
        view.setContentPadding(padding, view.getContentPaddingTop(), view.getContentPaddingRight(), view.getContentPaddingBottom())
    }

    fun setContentPaddingLeft(view: CardView, padding: String) {
        setContentPaddingLeft(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingTop(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), padding, view.getContentPaddingRight(), view.getContentPaddingBottom())
    }

    fun setContentPaddingTop(view: CardView, padding: String) {
        setContentPaddingTop(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingRight(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), view.getContentPaddingTop(), padding, view.getContentPaddingBottom())
    }

    fun setContentPaddingRight(view: CardView, padding: String) {
        setContentPaddingRight(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setContentPaddingBottom(view: CardView, padding: Int) {
        view.setContentPadding(view.getContentPaddingLeft(), view.getContentPaddingTop(), view.getContentPaddingRight(), padding)
    }

    fun setContentPaddingBottom(view: CardView, padding: String) {
        setContentPaddingBottom(view, stringToIntPx(padding, displayMetrics(view)))
    }
}