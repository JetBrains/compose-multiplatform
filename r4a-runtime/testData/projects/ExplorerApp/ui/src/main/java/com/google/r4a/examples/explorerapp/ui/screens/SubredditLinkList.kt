package com.google.r4a.examples.explorerapp.ui.screens

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.LoadingRow
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.r4a.examples.explorerapp.common.components.HomogeneousPagedList

class SubredditLinkList : Component() {
    private val repository get() = CompositionContext.current.getAmbient(RedditRepository.Ambient)

    private val pageSize = 10

    private val sortOptions = listOf(
            RedditFilterType.HOT,
            RedditFilterType.NEW,
            RedditFilterType.TOP
    )
    private var selectedSortIndex = 0
        set(value) {
            field = value
            _model = null
        }
    var subreddit: String = ""
        set(value) {
            field = value
            _model = null
        }

    private var _model: SubredditModel? = null
    private val model: SubredditModel
        get() {
            var result = _model
            if (result == null) {

                val repository = repository
                result = repository.linksOfSubreddit(subreddit, sortOptions[selectedSortIndex], pageSize)
                _model = result
                subscribe(result.links)
                subscribe(result.networkState)
                subscribe(result.refreshState)
            }
            return result
        }

    private val listParams = FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
    )

    override fun compose() {
        val model = model
        val isLoading = (model.networkState.value ?: AsyncState.LOADING) == AsyncState.LOADING

        <HomogeneousPagedList
            comparator=Link.COMPARATOR
            layoutParams=listParams
            paddingTop=(48.dp + 56.dp)
            headerCount=1
            composeHeader={
                <FrameLayout
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    paddingTop=12.dp
                    paddingBottom=0.dp
                    paddingHorizontal=16.dp
                >
                    <TextView
                        layoutWidth=WRAP_CONTENT
                        layoutHeight=WRAP_CONTENT
                        layoutGravity=(Gravity.START or Gravity.CENTER_VERTICAL)
                        text="Filter:"
                        textSize=15.sp
                        textColor=Colors.TEXT_MUTED
                    />
                    <Spinner
                        layoutWidth=WRAP_CONTENT
                        layoutHeight=WRAP_CONTENT
                        layoutGravity=(Gravity.END or Gravity.CENTER_VERTICAL)
                        data=(sortOptions.map { it.displayText })
                        // TODO(lmr): textColor?

                        controlledSelectedIndex=selectedSortIndex
                        onSelectedIndexChange={
                            selectedSortIndex = it
                            recomposeSync()
                        }
                    />
                </FrameLayout>
            }

            loadingRowCount=(if (isLoading) 1 else 0)
            composeLoadingRow={ _ -> <LoadingRow /> }
            backgroundColor=Colors.LIGHT_GRAY
            data=model.links.getValue()
        > link ->
            <PostListItem link />
        </HomogeneousPagedList>
    }

}