package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.AppBarLayout
import android.support.design.widget.*
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import android.support.v7.widget.Toolbar
import android.widget.LinearLayout
import com.google.r4a.*
import androidx.ui.androidview.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.LoadingRow
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import com.google.r4a.examples.explorerapp.common.components.HomogeneousList
import com.google.r4a.examples.explorerapp.ui.R

private val listParams = CoordinatorLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT).apply {
    behavior = AppBarLayout.ScrollingViewBehavior()
}

private val tlParams = AppBarLayout.LayoutParams(
        AppBarLayout.LayoutParams.MATCH_PARENT,
        AppBarLayout.LayoutParams.WRAP_CONTENT
).apply {
    scrollFlags = SCROLL_FLAG_SNAP
}

private val pagerParams = CoordinatorLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
).apply {
    behavior = AppBarLayout.ScrollingViewBehavior()
}

private val tabTitles = listOf("Comments", "Link")

@Composable
fun LinkDetailScreen(@Pivotal linkId: String, pageSize: Int = 10, initialLink: Link? = null) {
    <Observe>
        val repository = +ambient(RedditRepository.Ambient)
        val linkModel = +model { repository.linkDetails(linkId, pageSize) }

        val link = +subscribe(linkModel.link) ?: initialLink
        val comments = +subscribe(linkModel.comments)
        val networkState = +subscribe(linkModel.networkState)

        <CoordinatorLayout
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
        >
            <Tabs
                tabBackgroundColor=Colors.WHITE
                tabLayoutParams=tlParams
                pagerLayoutParams=pagerParams
                titles=tabTitles
            > tabs, content ->
                <RedditAppBar>
                    <tabs />
                </RedditAppBar>
                // once we have generics, it's possible we could have the object get passed directly into
                // the children function
                <content
                    children={ tabIndex ->
                        when (tabIndex) {
                            0 /* List of Comments */-> {
                                val isLoading = networkState == AsyncState.LOADING
                                <HomogeneousList
                                    comparator=HierarchicalThing.COMPARATOR
                                    layoutParams=listParams
                                    paddingTop=(48.dp + 56.dp)
                                    backgroundColor=Colors.LIGHT_GRAY

                                    headerCount=(if (link != null) 1 else 0)
                                    composeHeader={
                                        <LinkHeader link=link!! />
                                    }

                                    loadingRowCount=(if (isLoading) 1 else 0)
                                    composeLoadingRow={
                                        <LoadingRow />
                                    }

                                    onLoadAround={ pos -> linkModel.loadAround(pos) }
                                    data=comments
                                > node ->
                                    <CommentRow
                                        node
                                        onClick={
                                            when (node) {
                                                is RedditMore -> linkModel.loadMore(node)
                                                is Comment -> linkModel.toggleCollapsedState(node)
                                            }
                                        }
                                    />
                                </HomogeneousList>
                                Unit
                            }
                            1 /* WebView */ -> {
                                val url = link?.url
                                if (url != null) {
                                    <WebView
                                        url=url
    //                                                    onReceivedTitle={}
                                    />
                                }
                                Unit
                            }
                            else -> Unit
                        }
                    }
                />
            </Tabs>
        </CoordinatorLayout>
    </Observe>
}


