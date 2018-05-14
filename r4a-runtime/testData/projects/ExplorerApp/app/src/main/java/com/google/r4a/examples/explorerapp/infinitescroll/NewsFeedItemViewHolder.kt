package com.google.r4a.examples.explorerapp.infinitescroll

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout

class NewsFeedItemViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    val component = NewsFeedStoryComponent()
}