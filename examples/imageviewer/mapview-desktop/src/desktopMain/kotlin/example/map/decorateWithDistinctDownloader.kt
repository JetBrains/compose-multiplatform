package example.map

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch

private sealed interface Message<K, T> {
    class StartDownload<K, T>(val key: K, val deferred: CompletableDeferred<T>) : Message<K, T>
    class DownloadComplete<K, T>(val key: K, val result: T) : Message<K, T>
    class DownloadFail<K, T>(val key: K, val exception: Throwable) : Message<K, T>
}

@OptIn(ObsoleteCoroutinesApi::class)
fun <K, T> ContentRepository<K, T>.decorateWithDistinctDownloader(
    scope: CoroutineScope
): ContentRepository<K, T> {
    val origin = this
    val actor = scope.actor<Message<K, T>> {
        val mapKeyToRequests: MutableMap<K, MutableList<CompletableDeferred<T>>> = mutableMapOf()
        while (true) {
            when (val message = receive()) {
                is Message.StartDownload<K, T> -> {
                    val requestsWithSameKey = mapKeyToRequests.getOrPut(message.key) {
                        val newHandlers = mutableListOf<CompletableDeferred<T>>()
                        scope.launch {
                            try {
                                val result = origin.loadContent(message.key)
                                channel.send(
                                    Message.DownloadComplete(message.key, result)
                                )
                            } catch (t: Throwable) {
                                channel.send(Message.DownloadFail(message.key, t))
                            }
                        }
                        newHandlers
                    }
                    requestsWithSameKey.add(message.deferred)
                }

                is Message.DownloadComplete<K, T> -> {
                    mapKeyToRequests.remove(message.key)?.forEach {
                        it.complete(message.result)
                    }
                }

                is Message.DownloadFail<K, T> -> {
                    val exceptionInfo =
                        "decorateWithDistinctDownloader, fail to load tile ${message.key}"
                    val exception = Exception(exceptionInfo, message.exception)
                    mapKeyToRequests.remove(message.key)?.forEach {
                        it.completeExceptionally(exception)
                    }
                }
            }
        }
    }

    return object : ContentRepository<K, T> {
        override suspend fun loadContent(key: K): T {
            return CompletableDeferred<T>()
                .also { actor.send(Message.StartDownload(key, it)) }
                .await()
        }
    }
}
