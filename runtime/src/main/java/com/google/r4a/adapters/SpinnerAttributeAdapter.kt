@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AbsSpinner
import android.widget.Spinner

private val AbsSpinner.composeAdapter: ArrayAdapter<Any>
    get() {
        @Suppress("UNCHECKED_CAST")
        var adapter = adapter as? ArrayAdapter<Any>
        if (adapter == null) {
            adapter = ArrayAdapter<Any>()
            setAdapter(adapter)
        }
        return adapter
    }

fun AbsSpinner.setData(data: Collection<Any>) {
    composeAdapter.apply {
        items = data.toMutableList()
        notifyDataSetChanged()
    }
}

fun AbsSpinner.setComposeItem(composeItem: Function1<Any, Unit>) {
    composeAdapter.composable = composeItem
}

fun Spinner.setDropDownHorizontalOffset(dropDownHorizontalOffset: Dimension) =
    setDropDownHorizontalOffset(dropDownHorizontalOffset.toIntPixels(metrics))
fun Spinner.setDropDownVerticalOffset(dropDownVerticalOffset: Dimension) =
    setDropDownVerticalOffset(dropDownVerticalOffset.toIntPixels(metrics))
fun Spinner.setDropDownWidth(dropDownWidth: Dimension) =
    setDropDownWidth(dropDownWidth.toIntPixels(metrics))