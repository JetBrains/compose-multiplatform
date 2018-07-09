package com.google.r4a.examples.explorerapp.common.components

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout

class XViewHolder(parent: ViewGroup, val viewType: Int): RecyclerView.ViewHolder(LinearLayout(parent.context).apply {
    layoutParams = LAYOUT_PARAMS
}) {
    companion object {
        private val LAYOUT_PARAMS = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}