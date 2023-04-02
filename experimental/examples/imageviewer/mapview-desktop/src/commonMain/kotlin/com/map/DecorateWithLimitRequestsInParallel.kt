package com.map

import com.map.collection.CollectionAddRemove
import com.map.collection.createStack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Декоратор для ограничения количества параллельных запросов в сеть.
 * Запросы которые пришли недавно - обрабатываются в первую очередь (внутри для этого используется Stack)
 */
fun <K, T> ContentRepository<K, T>.decorateWithLimitRequestsInParallel(
    scope: CoroutineScope,
    maxParallelRequests: Int = 10,
    waitBufferCapacity: Int = 50,
    delayBeforeRequestMs: Long = 50 // Если карта быстро изменяется, то загружать сразу нет смысла
): ContentRepository<K, T> {
    val origin = this

    // Потокобезопасное immutable состояние
    data class State(
        val stack: CollectionAddRemove<ElementWait<K, T>> = createStack(waitBufferCapacity),
        val currentRequests: Int = 0
    )

    val store = scope.createStoreWithSideEffect(
        init = State(),
        effectHandler = { store, effect: NetworkSideEffect<K, T> ->
            when (effect) {
                is NetworkSideEffect.Load<K, T> -> {
                    effect.waitElements.forEach { element ->
                        scope.launch {
                            try {
                                val result = origin.loadContent(element.key)
                                element.deferred.complete(result)
                            } catch (t: Throwable) {
                                val message = "caught exception in decorateWithLimitRequestsInParallel"
                                element.deferred.completeExceptionally(Exception(message, t))
                            } finally {
                                store.send(Intent.ElementComplete())
                            }
                        }
                    }
                }
                is NetworkSideEffect.Delay<K, T> -> {
                    scope.launch {
                        delay(delayBeforeRequestMs)
                        store.send(Intent.AfterDelay())
                    }
                }
            }
        }
    ) { state, intent: Intent<K, T> ->
        // Модификация состояния происходит только в этой функции и исполняется в одном потоке
        when (intent) {
            is Intent.NewElement -> {
                val (fifo, removed) = state.stack.add(intent.wait)
                removed?.let {
                    scope.launch {
                        // cancel element callback, because hi will wait too long
                        it.deferred.completeExceptionally(Exception("cancelled in decorateWithLimitRequestsInParallel"))
                    }
                }
                state.copy(stack = fifo).addSideEffect(NetworkSideEffect.Delay())
            }
            is Intent.AfterDelay -> {
                if (state.stack.isNotEmpty()) {
                    var fifo = state.stack
                    val elementsToLoad: MutableList<ElementWait<K, T>> = mutableListOf()
                    while (state.currentRequests + elementsToLoad.size < maxParallelRequests && fifo.isNotEmpty()) {
                        val result = fifo.remove()
                        result.removed?.let {
                            elementsToLoad.add(it)
                        }
                        fifo = result.collection
                    }
                    state.copy(
                        stack = fifo,
                        currentRequests = state.currentRequests + elementsToLoad.size
                    ).addSideEffect(NetworkSideEffect.Load(elementsToLoad))
                } else {
                    state.noSideEffects()
                }
            }
            is Intent.ElementComplete -> {
                state.copy(
                    currentRequests = state.currentRequests - 1
                ).run {
                    if (state.stack.isNotEmpty()) {
                        addSideEffect(NetworkSideEffect.Delay())
                    } else {
                        noSideEffects()
                    }
                }
            }
        }
    }

    return object : ContentRepository<K, T> {
        override suspend fun loadContent(key: K): T {
            return CompletableDeferred<T>()
                .also { store.send(Intent.NewElement(ElementWait(key, it))) }
                .await()
        }
    }
}

private class ElementWait<K, T>(val key: K, val deferred: CompletableDeferred<T>)
private sealed interface Intent<K, T> {
    class ElementComplete<K, T> : Intent<K, T>
    class NewElement<K, T>(val wait: ElementWait<K, T>) : Intent<K, T>
    class AfterDelay<K, T> : Intent<K, T>
}

private sealed interface NetworkSideEffect<K, T> {
    class Load<K, T>(val waitElements: List<ElementWait<K, T>>) : NetworkSideEffect<K, T>
    class Delay<K, T> : NetworkSideEffect<K, T>
}
