package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.Toolbar
//import android.widget.Toolbar
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.r4a.Children
import com.google.r4a.Composable

class RedditAppBar : Component() {
    // TODO(lmr): for some reason this cannot be moved into the constructor
    @Children
    lateinit var children: @Composable() () -> Unit
    override fun compose() {
        <AppBarLayout
            layoutWidth=MATCH_PARENT
            layoutHeight=WRAP_CONTENT
            fitsSystemWindows=true
            elevation=4.dp
        >
            <Toolbar
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                // TODO(lmr): need to figure out how to get this to work...
                layoutAppBarScrollFlags=AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
//                layoutAppBarScrollFlags={AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS}
                title="Reddit"
                titleTextColor=Colors.TEXT_DARK
                backgroundColor=Colors.WHITE
                logo=R.drawable.reddit_mark_on_white
                titleMarginStart=32.dp
                menu=R.menu.toolbar_menu
            />
            <children />
        </AppBarLayout>
    }
}