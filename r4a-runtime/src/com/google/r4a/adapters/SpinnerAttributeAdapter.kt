@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AbsSpinner
import android.widget.Spinner

private fun AbsSpinner.getR4aAdapter(): ArrayAdapter<Any> {
    var adapter = adapter as? ArrayAdapter<Any>
    if (adapter == null) {
        adapter = ArrayAdapter<Any>()
        setAdapter(adapter)
    }
    return adapter
}

fun AbsSpinner.setData(data: Collection<Any>) {
    val adapter = getR4aAdapter()
    adapter.items = data.toMutableList()
    adapter.notifyDataSetChanged()
}

fun AbsSpinner.setComposeItem(composeItem: Function1<Any, Unit>) {
    getR4aAdapter().composable = composeItem
}

fun Spinner.setDropDownHorizontalOffset(dropDownHorizontalOffset: Dimension) = setDropDownHorizontalOffset(dropDownHorizontalOffset.toIntPixels(metrics))
fun Spinner.setDropDownVerticalOffset(dropDownVerticalOffset: Dimension) = setDropDownVerticalOffset(dropDownVerticalOffset.toIntPixels(metrics))
fun Spinner.setDropDownWidth(dropDownWidth: Dimension) = setDropDownWidth(dropDownWidth.toIntPixels(metrics))