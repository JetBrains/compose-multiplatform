package com.google.r4a.examples.explorerapp.ui.screens

import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.google.r4a.*
import androidx.ui.androidview.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.LoadingRow
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.r4a.examples.explorerapp.common.components.HomogeneousPagedList

private val sortOptions = listOf(
        RedditFilterType.HOT,
        RedditFilterType.NEW,
        RedditFilterType.TOP
)

private val listParams = FrameLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
)

@Composable
fun SubredditLinkList(subreddit: String, pageSize: Int = 10) {
    // TODO(lmr): for some reason this observe tag was necessary here, despite our automatic insertion. not sure why.
    <Observe>
        val selectedSortIndex = +state { 0 }
        val repository = +ambient(RedditRepository.Ambient)
        val model = +modelFor(subreddit, selectedSortIndex.value) {
            repository.linksOfSubreddit(subreddit, sortOptions[selectedSortIndex.value], pageSize)
        }
        val links = +subscribe(model.links)
        val networkState = +subscribe(model.networkState) ?: AsyncState.LOADING

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

                        controlledSelectedIndex=selectedSortIndex.value
                        onSelectedIndexChange={
                            if (it != selectedSortIndex.value) {
                                selectedSortIndex.value = it
                            }
                        }
                    />
                </FrameLayout>
            }

            loadingRowCount=(if (networkState == AsyncState.LOADING) 1 else 0)
            composeLoadingRow={ <LoadingRow /> }
            backgroundColor=Colors.LIGHT_GRAY
            data=links
        > link ->
            <PostListItem link />
        </HomogeneousPagedList>
    </Observe>
}