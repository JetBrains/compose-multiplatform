package com.google.r4a.examples.explorerapp.infinitescroll

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.*
import com.google.r4a.Component

/** This component is the scrollable box (RecyclerView) with all the newsfeed stories inside it **/
class NewsFeed: Component() {

    private val adapter = PhotoManagementRecyclerAdapter()
    private val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    private val layoutManager = LinearLayoutManager(null).apply { setOrientation(LinearLayoutManager.VERTICAL) }

    override fun compose() {
        <RecyclerView
            layoutManager={layoutManager}
            adapter={adapter}
            layoutParams={layoutParams}
        />
    }
}