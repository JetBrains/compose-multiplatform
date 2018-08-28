package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.*
import android.support.v7.widget.Toolbar
import android.widget.LinearLayout
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import android.widget.TextView


class LinkListScreen : Component() {
    private val tlParams = AppBarLayout.LayoutParams(
            AppBarLayout.LayoutParams.MATCH_PARENT,
            AppBarLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
    }

    private val pagerParams = CoordinatorLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
    ).apply {
        setBehavior(AppBarLayout.ScrollingViewBehavior())
    }

    // TODO(lmr): change this to be the user's saved list of subreddits when a user is logged in
    private val subreddits = listOf("science", "androiddev", "javascript", "reactjs")

    override fun compose() {
        val subreddits = subreddits // TODO(lmr): remove when private access works
        <CoordinatorLayout
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
        >
            <Tabs
                tabBackgroundColor=Colors.WHITE
                tabLayoutParams=tlParams
                pagerLayoutParams=pagerParams
                offscreenPageLimit=2
                titles=subreddits
            > tabs, content ->
                <RedditAppBar>
                    <tabs />
                </RedditAppBar>
                // once we have generics, it's possible we could have the object get passed directly into
                // the children function
                <content
                    children={ tabIndex ->
                        val subreddit = subreddits[tabIndex]
                        <SubredditLinkList subreddit />
                    }
                />
            </Tabs>
        </CoordinatorLayout>
    }
}

