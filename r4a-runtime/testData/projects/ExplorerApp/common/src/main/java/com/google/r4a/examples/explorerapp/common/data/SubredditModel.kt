package com.google.r4a.examples.explorerapp.common.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.paging.DataSource
import android.arch.paging.PageKeyedDataSource
import android.arch.paging.PagedList
import com.google.r4a.examples.explorerapp.common.api.RedditApi
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.Executor


data class SubredditModel(
        val links: LiveData<PagedList<Link>>,
        val networkState: LiveData<AsyncState>,
        val refreshState: LiveData<AsyncState>,
        val refresh: () -> Unit,
        val retry: () -> Unit
)

class SubRedditDataSourceFactory(
        private val redditApi: RedditApi,
        private val subredditName: String,
        private val filter: RedditFilterType,
        private val retryExecutor: Executor) : DataSource.Factory<String, Link>() {
    val liveSource = MutableLiveData<PageKeyedSubredditDataSource>()
    override fun create(): DataSource<String, Link> {
        val source = PageKeyedSubredditDataSource(redditApi, subredditName, filter, retryExecutor)
        liveSource.postValue(source)
        return source
    }
}

/**
 * A data source that uses the before/after keys returned in page requests.
 */
class PageKeyedSubredditDataSource(
        private val redditApi: RedditApi,
        private val subredditName: String,
        private val filter: RedditFilterType,
        private val retryExecutor: Executor) : PageKeyedDataSource<String, Link>() {

    // keep a function reference for the retry event
    private var retry: (() -> Unit)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success value before calling loadAfter.
     */
    val networkState = MutableLiveData<AsyncState>()

    val initialLoad = MutableLiveData<AsyncState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, Link>) {
        // ignored, since we only ever append to our initial load
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, Link>) {
        networkState.postValue(AsyncState.LOADING)
        redditApi.getSubredditLinksAfter(
                subreddit = subredditName,
                filter = filter,
                after = params.key,
                limit = params.requestedLoadSize).enqueue(
                object : retrofit2.Callback<ListResponse> {
                    override fun onFailure(call: Call<ListResponse>, t: Throwable) {
                        retry = {
                            loadAfter(params, callback)
                        }
                        networkState.postValue(AsyncState.FAILED)
                    }

                    override fun onResponse(
                            call: Call<ListResponse>,
                            response: Response<ListResponse>) {
                        if (response.isSuccessful) {
                            val data = response.body()
                            val links = data?.links ?: emptyList()
                            retry = null
                            callback.onResult(links, data?.after)
                            networkState.postValue(AsyncState.DONE)
                        } else {
                            retry = {
                                loadAfter(params, callback)
                            }
                            networkState.postValue(AsyncState.FAILED)
                        }
                    }
                }
        )
    }

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, Link>) {
        val request = redditApi.getSubredditLinks(
                subreddit = subredditName,
                filter = filter,
                limit = params.requestedLoadSize
        )
        networkState.postValue(AsyncState.LOADING)
        initialLoad.postValue(AsyncState.LOADING)

        // triggered by a refresh, we better execute sync
        try {
            val response = request.execute()
            val data = response.body()
            val items = data?.links ?: emptyList()

            retry = null
            networkState.postValue(AsyncState.DONE)
            initialLoad.postValue(AsyncState.DONE)
            callback.onResult(items, data?.before, data?.after)
        } catch (ioException: IOException) {
            retry = {
                loadInitial(params, callback)
            }
            val error = AsyncState.FAILED
            networkState.postValue(error)
            initialLoad.postValue(error)
        }
    }
}
