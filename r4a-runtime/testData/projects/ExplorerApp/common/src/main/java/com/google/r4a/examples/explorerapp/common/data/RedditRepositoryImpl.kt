package com.google.r4a.examples.explorerapp.common.data

import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import com.google.r4a.examples.explorerapp.common.api.RedditApi
import java.util.concurrent.Executor

class RedditRepositoryImpl(
        private val redditApi: RedditApi,
        private val networkExecutor: Executor) : RedditRepository {

    override fun linksOfSubreddit(subreddit: String, filter: RedditFilterType, pageSize: Int): SubredditModel {
        val factory = SubRedditDataSourceFactory(redditApi, subreddit, filter, networkExecutor)

        val liveLinks = LivePagedListBuilder(factory, pageSize)
                // provide custom executor for network requests, otherwise it will default to
                // Arch Components' IO pool which is also used for disk access
                .setFetchExecutor(networkExecutor)
                .build()

        return SubredditModel(
                links = liveLinks,
                networkState = Transformations.switchMap(factory.liveSource) { it.networkState },
                refreshState = Transformations.switchMap(factory.liveSource) { it.initialLoad },
                retry = {
                    factory.liveSource.value?.retryAllFailed()
                },
                refresh = {
                    factory.liveSource.value?.invalidate()
                }
        )
    }

//    override fun linkDetails(linkId: String, pageSize: Int): LinkModel {
//        val factory = CommentDataSourceFactory(redditApi, linkId, networkExecutor)
//
//        val liveComments = LivePagedListBuilder(factory, pageSize)
//                // provide custom executor for network requests, otherwise it will default to
//                // Arch Components' IO pool which is also used for disk access
//                .setFetchExecutor(networkExecutor)
//                .build()
//
//        return LinkModel(
//                link = Transformations.switchMap(factory.liveSource) { it.liveLink },
//                comments = liveComments,
//                networkState = Transformations.switchMap(factory.liveSource) { it.networkState },
//                refreshState = Transformations.switchMap(factory.liveSource) { it.initialLoad },
//                retry = {
//                    factory.liveSource.value?.retryAllFailed()
//                },
//                refresh = {
//                    factory.liveSource.value?.invalidate()
//                }
//        )
//    }

    override fun linkDetails(linkId: String, pageSize: Int): LinkModel {
//        val factory = CommentDataSourceFactory(redditApi, linkId, networkExecutor)
//
//        val liveComments = LivePagedListBuilder(factory, pageSize)
//                // provide custom executor for network requests, otherwise it will default to
//                // Arch Components' IO pool which is also used for disk access
//                .setFetchExecutor(networkExecutor)
//                .build()

        return LinkModel(
                api = redditApi,
                executor = networkExecutor,
                linkId = linkId,
                pageSize = pageSize
//                link = Transformations.switchMap(factory.liveSource) { it.liveLink },
//                comments = liveComments,
//                networkState = Transformations.switchMap(factory.liveSource) { it.networkState },
//                refreshState = Transformations.switchMap(factory.liveSource) { it.initialLoad },
//                retry = {
//                    factory.liveSource.value?.retryAllFailed()
//                },
//                refresh = {
//                    factory.liveSource.value?.invalidate()
//                }
        )
    }
}