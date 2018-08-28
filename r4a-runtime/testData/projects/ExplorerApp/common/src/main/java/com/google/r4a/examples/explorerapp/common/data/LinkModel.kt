package com.google.r4a.examples.explorerapp.common.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.paging.DataSource
import android.arch.paging.PageKeyedDataSource
import com.google.r4a.examples.explorerapp.common.api.RedditApi
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.Executor

/**
 * This is the viewmodel used for the LinkDetailScreen. It handles the loading of
 * a link and its comments.
 */
class LinkModel(
        private val linkId: Id,
        private val api: RedditApi,
        private val executor: Executor,
        private val pageSize: Int
) {
    private val mutableLink = MutableLiveData<Link>()
    private var mutableComments = MutableLiveData<List<HierarchicalThing>>().apply { value = listOf() }
    private val mutableNetworkState = MutableLiveData<AsyncState>()
    private val mutableInitialLoad = MutableLiveData<AsyncState>()

    val link: LiveData<Link> = mutableLink
    val comments: LiveData<List<HierarchicalThing>> = mutableComments
    val networkState: LiveData<AsyncState> = mutableNetworkState
    val initialLoad: LiveData<AsyncState> = mutableInitialLoad
    private var retry: (() -> Unit)? = null

    private var highestLoadedPosition = 0
    private var isLoadingMore = false
    private val fetchAheadDistance = pageSize

    /**
     * Call this to indicate when a comment at a certain position is being "used" by the
     * UI. This will be used to handle infinite loading / paging of the comment list at
     * the appropriate time
     */
    fun loadAround(position: Int) {
        if (highestLoadedPosition > position) return
        highestLoadedPosition = position
        if (isLoadingMore) return
        val total = comments.value!!.size
        if (total - position < fetchAheadDistance) {
            attemptLoadNextPage()
        }
    }

    /**
     * Given a MoeNode in the comment list, this will load in those comments and replace
     * it in the list at the appropriate place.
     */
    fun loadMore(moreNode: RedditMore) {
        executor.execute {
            loadNextPage(moreNode)
        }
    }

    /**
     * Comments can be "collapsed" for easier viewing of noisy threads. Call this to toggle
     * the collapsed state of a comment.
     */
    fun toggleCollapsedState(comment: Comment) {
        val comments = this.comments.value ?: return
        val index = comments.indexOf(comment)
        if (index == -1) return
        val next = comments.toMutableList()
        if (comment.isCollapsed) {
            comment.collapsedChildren?.let {
                next.addAll(index + 1, it)
                comment.collapsedChildren = listOf()
            }
        } else {
            // collapse all children to this comment
            val depth = comment.depth
            val i = index + 1
            val children = mutableListOf<HierarchicalThing>()
            while (i < next.size) {
                val el = next[i]
                if (el.depth <= depth) break
                next.removeAt(i)
                children.add(el)
            }
            comment.collapsedChildren = children
        }
        comment.isCollapsed = !comment.isCollapsed
        mutableComments.postValue(next)
    }


    private fun attemptLoadNextPage() {
        val moreNode = comments.value?.lastOrNull() as? RedditMore ?: return
        if (moreNode.depth != 0) return
        isLoadingMore = true
        executor.execute {
            loadNextPage(moreNode)
        }
    }

    private fun replace(moreNode: RedditMore, toReplaceList: List<HierarchicalThing>) {
        val current = comments.value!!
        val index = current.lastIndexOf(moreNode)
        if (index == -1) error("Couldn't find node to replace!")
        val nextList = mutableListOf<HierarchicalThing>()
        nextList.addAll(current.subList(0, index))
        nextList.addAll(toReplaceList)
        if (index < current.size - 1) {
            nextList.addAll(current.subList(index + 1, current.size - 1))
        }
        mutableComments.postValue(nextList)
    }

    private fun loadNextPage(moreNode: RedditMore) {
        mutableNetworkState.postValue(AsyncState.LOADING)
        api.getMoreChildren(
                api_type = "json",
                linkId = "t3_$linkId",
                children = moreNode.children.joinToString()
        ).enqueue(
                object : retrofit2.Callback<MoreChildrenResponse> {
                    override fun onFailure(call: Call<MoreChildrenResponse>, t: Throwable) {
                        retry = {
                            loadNextPage(moreNode)
                        }
                        mutableNetworkState.postValue(AsyncState.FAILED)
                        isLoadingMore = false
                    }

                    override fun onResponse(
                            call: Call<MoreChildrenResponse>,
                            response: Response<MoreChildrenResponse>) {
                        val data = response.body()
                        if (response.isSuccessful && data != null) {
                            retry = null
                            replace(moreNode, extractResponse(data))
                            mutableNetworkState.postValue(AsyncState.DONE)
                        } else {
                            retry = {
                                loadNextPage(moreNode)
                            }
                            mutableNetworkState.postValue(AsyncState.FAILED)
                            isLoadingMore = false
                        }
                    }
                }
        )


    }

    private fun attemptLoadInitial() {
        val request = api.getLink(
                link = linkId,
                depth = 3, // NOTE: hardcoded for now
                limit = pageSize * 3
        )
        mutableNetworkState.postValue(AsyncState.LOADING)
        mutableInitialLoad.postValue(AsyncState.LOADING)

        // triggered by a refresh, we better execute sync
        val body: LinkResponse
        try {
            val response = request.execute()
            body = response.body()!!
        } catch (exception: Exception) {
            retry = {
                attemptLoadInitial()
            }
            mutableNetworkState.postValue(AsyncState.FAILED)
            mutableInitialLoad.postValue(AsyncState.FAILED)
            return
        }

        retry = null
        mutableNetworkState.postValue(AsyncState.DONE)
        mutableInitialLoad.postValue(AsyncState.DONE)
        val comments = extractResponse(body)
        mutableComments.postValue(comments)
    }

    private fun extractResponse(data: MoreChildrenResponse): List<HierarchicalThing> {
        val nodes = mutableListOf<HierarchicalThing>()
        data.json.data?.things?.forEach {
            when (it) {
                is Comment -> nodes.add(it)
                is RedditMore -> if (it.children.isNotEmpty()) nodes.add(it)
                else -> error("unrecognized object")
            }
        }

        return nodes
    }

    private fun extractResponse(data: LinkResponse): List<HierarchicalThing> {
        val nodes = mutableListOf<HierarchicalThing>()

        val link = (data.link as RedditListing).children[0] as Link

        // since this is the initial response, lets go ahead and
        mutableLink.postValue(link)

        val commentListing = data.comments as RedditListing

        var extract: (RedditObject, Int) -> Unit = { a, b ->}
        extract = { obj: RedditObject, depth: Int ->
            when (obj) {
                is Comment -> {
                    // TODO(lmr): check to see if this is needed or not...
                    obj.depth = depth
                    nodes.add(obj)
                    // this response has nested comments actually embedded in the "replies" field, so we have to
                    // recurse down and flatten the list
                    val childCommentListing = (obj.replies as? RedditListing)
                    childCommentListing?.children?.forEach { extract(it, depth + 1) }
                }
                is RedditMore -> if (obj.children.isNotEmpty()) {
//                    obj.id = "more_${obj.id}"
                    obj.depth = depth
                    nodes.add(obj)
                }
                else -> error("unrecognized RedditObject type")
            }
        }

        commentListing.children.forEach { extract(it, 0) }

        return nodes
    }

    // Do the initial load as soon as the object is created
    init { executor.execute { attemptLoadInitial() } }
}
