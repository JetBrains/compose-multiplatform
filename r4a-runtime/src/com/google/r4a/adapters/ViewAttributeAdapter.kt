package com.google.r4a.adapters

import android.graphics.Color
import android.view.View
import com.google.r4a.adapters.Utils.displayMetrics
import com.google.r4a.adapters.Utils.stringToFloatPx
import com.google.r4a.adapters.Utils.stringToIntPx
import com.google.r4a.annotations.ColorString
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.DimensionString

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ViewAttributeAdapter: AttributeAdapter() {

    // NOTE: these attributes are added to every view/component so that we can have a "key" attribute
    // that the users define to preserve state across composes. In the long run, we may decide to
    // use namespaces for these attributes, but for now, the grammar does not allow that and this
    // is a simple stop-gap solution.
    fun setKey(view: View, key: Any) {}

    @DimensionString
    @ConflictsWith("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal")
    fun setPadding(view: View, padding: Int) {
        view.setPadding(padding, padding, padding, padding)
    }

    @DimensionString
    @ConflictsWith("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal")
    fun setPadding(view: View, padding: String) {
        setPadding(view, stringToIntPx(padding, displayMetrics(view)))
    }

    @DimensionString
    @ConflictsWith("paddingLeft", "paddingRight")
    fun setPaddingHorizontal(view: View, padding: Int) {
        view.setPadding(padding, view.paddingTop, padding, view.paddingBottom)
    }

    @DimensionString
    @ConflictsWith("paddingLeft", "paddingRight")
    fun setPaddingHorizontal(view: View, padding: String) {
        setPaddingHorizontal(view, stringToIntPx(padding, displayMetrics(view)))
    }

    @DimensionString
    @ConflictsWith("paddingTop", "paddingBottom")
    fun setPaddingVertical(view: View, padding: Int) {
        view.setPadding(view.paddingLeft, padding, view.paddingRight, padding)
    }

    @DimensionString
    @ConflictsWith("paddingTop", "paddingBottom")
    fun setPaddingVertical(view: View, padding: String) {
        setPaddingVertical(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingLeft(view: View, padding: Int) {
        view.setPadding(padding, view.paddingTop, view.paddingRight, view.paddingBottom)
    }

    @DimensionString
    fun setPaddingLeft(view: View, padding: String) {
        setPaddingLeft(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingTop(view: View, padding: Int) {
        view.setPadding(view.paddingLeft, padding, view.paddingRight, view.paddingBottom)
    }

    @DimensionString
    fun setPaddingTop(view: View, padding: String) {
        setPaddingTop(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingRight(view: View, padding: Int) {
        view.setPadding(view.paddingLeft, view.paddingTop, padding, view.paddingBottom)
    }

    @DimensionString
    fun setPaddingRight(view: View, padding: String) {
        setPaddingRight(view, stringToIntPx(padding, displayMetrics(view)))
    }

    fun setPaddingBottom(view: View, padding: Int) {
        view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, padding)
    }

    @DimensionString
    fun setPaddingBottom(view: View, padding: String) {
        setPaddingBottom(view, stringToIntPx(padding, displayMetrics(view)))
    }

    @ColorString
    fun setBackgroundColor(view: View, color: String) {
        view.setBackgroundColor(Color.parseColor(color))
    }

}