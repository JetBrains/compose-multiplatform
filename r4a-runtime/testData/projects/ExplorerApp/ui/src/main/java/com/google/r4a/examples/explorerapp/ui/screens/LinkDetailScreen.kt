package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.AppBarLayout
import android.support.design.widget.*
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import android.support.v7.widget.Toolbar
import android.widget.LinearLayout
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.LoadingRow
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.TextView
import com.google.r4a.examples.explorerapp.common.components.HomogeneousList
import com.google.r4a.examples.explorerapp.ui.R

class LinkDetailScreen : Component() {
    private val repository get() = CompositionContext.current.getAmbient(RedditRepository.Ambient, this)

    private val pageSize = 10

    private var linkId: String? = null
        set(value) {
            field = value
            _model = null
        }
    // TODO(lmr): for some reason the property setter version of this broke IR
    fun setId(id: String) {
        linkId = id
    }
    private var minitialLink: Link? = null
    fun setInitialLink(link: Link?) {
        minitialLink = link
    }

    private var _model: LinkModel? = null
    private val model: LinkModel
        get() {
            // NOTE(lmr): this is a good example of derived state. This could be cleaned up with a good memoize
            // property delegate potentially
            var result = _model
            if (result == null) {
                result = repository.linkDetails(linkId!!, pageSize)
                _model = result
                subscribe(result.link)
                subscribe(result.comments)
                subscribe(result.networkState)
                subscribe(result.initialLoad)
            }
            return result
        }

    private val listParams = CoordinatorLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT).apply {
        setBehavior(AppBarLayout.ScrollingViewBehavior())
    }

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

    private val tabTitles = listOf("Comments", "Link")

    override fun compose() {
        val model = model // TODO(lmr): remove when private access works
        val listParams = listParams // TODO(lmr): remove when private access works
        val link = model.link.getValue() ?: minitialLink
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
                                val isLoading = model.networkState.getValue() == AsyncState.LOADING
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

                                    onLoadAround={ pos -> model.loadAround(pos) }
                                    data=model.comments.getValue()
                                > node ->
                                    <CommentRow
                                        node
                                        onClick={
                                            when (node) {
                                                is RedditMore -> model.loadMore(node)
                                                is Comment -> model.toggleCollapsedState(node)
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
    }
}


