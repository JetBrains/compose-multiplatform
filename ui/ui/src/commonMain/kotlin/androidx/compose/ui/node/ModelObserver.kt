/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")

package androidx.compose.ui.node

import androidx.compose.runtime.ObserverMap
import androidx.compose.runtime.frames.FrameCommitObserver
import androidx.compose.runtime.frames.FrameReadObserver
import androidx.compose.runtime.frames.observeAllReads
import androidx.compose.ui.util.fastForEach

/**
 * Allows for easy model read observation. To begin observe a change, you must pass a
 * non-lambda `onCommit` listener to the [observeReads] method.
 *
 * When a state change has been committed, the `onCommit` listener will be called
 * with the `targetObject` as the argument. There are no order guarantees for
 * `onCommit` listener calls. Commit callbacks are made on the thread that model changes
 * are committed, so the [commitExecutor] allows the developer to control the thread on which the
 * `onCommit`calls are made. An example use would be to have the executor shift to the
 * the UI thread for the `onCommit` callbacks to be made.
 *
 * A different ModelObserver should be used with each thread that [observeReads] is called on.
 *
 * @param commitExecutor The executor on which all `onCommit` calls will be made.
 */
@Deprecated("Frames have been replaced by snapshots",
    ReplaceWith(
        "SnapshotStateObserver",
        "androidx.compose.runtime.snapshots"
    )
)
class ModelObserver(private val commitExecutor: (command: () -> Unit) -> Unit) {
    private val commitObserver: FrameCommitObserver = { committed, _ ->
        var hasValues = false
        // This array is in the same order as commitMaps
        @Suppress("DEPRECATION_ERROR")
        val targetsArray = synchronized(commitMaps) {
            Array(commitMaps.size) { index ->
                commitMaps[index].map.get(committed).apply {
                    if (isNotEmpty())
                        hasValues = true
                }
            }
        }
        if (hasValues) {
            commitExecutor {
                callOnCommit(targetsArray)
            }
        }
    }

    /**
     * The [FrameReadObserver] used by this [ModelObserver] during [observeReads].
     */
    private val readObserver: FrameReadObserver = { model ->
        if (!isPaused) {
            @Suppress("DEPRECATION_ERROR")
            synchronized(commitMaps) {
                currentMap!!.add(model, currentTarget!!)
            }
        }
    }

    /**
     * List of all [CommitMap]s. When [observeReads] is called, there will be
     * a [CommitMap] associated with its `onCommit` callback in this list. The list
     * only grows.
     */
    private val commitMaps = mutableListOf<CommitMap<*>>()

    /**
     * Method to call when unsubscribing from the commit observer.
     */
    private var commitUnsubscribe: (() -> Unit)? = null

    /**
     * `true` when an [observeReads] is in progress and [readObserver] is active and
     * `false` when [readObserver] is no longer observing changes.
     */
    private var isObserving = false

    /**
     * `true` when [pauseObservingReads] is called and read observations should no
     * longer be considered invalidations for the `onCommit` callback.
     */
    private var isPaused = false

    /**
     * The [ObserverMap] that should be added to when a model is read during [observeReads].
     */
    private var currentMap: ObserverMap<Any, Any>? = null

    /**
     * The target associated with the active [observeReads] call.
     */
    private var currentTarget: Any? = null

    /**
     * Test-only access to the internal commit listener. This is used for benchmarking
     * the commit notification callback.
     *
     * @suppress
     */
    val frameCommitObserver: FrameCommitObserver
        @InternalCoreApi
        get() = commitObserver

    /**
     * Executes [block], observing model reads during its execution.
     * The [target] is stored as a weak reference to be passed to [onCommit] when a change to the
     * model has been detected.
     *
     * Observation for [target] will be paused when a new [observeReads] call is made or when
     * [pauseObservingReads] is called.
     *
     * Any previous observation with the given [target] and [onCommit] will be
     * cleared and only the new observation on [block] will be stored. It is important that
     * the same instance of [onCommit] is used between calls or previous references will
     * not be cleared.
     *
     * The [onCommit] will be called when a model that was accessed during [block] has been
     * committed, and it will be called with [commitExecutor].
     */
    fun <T : Any> observeReads(target: T, onCommit: (T) -> Unit, block: () -> Unit) {
        val oldMap = currentMap
        val oldTarget = currentTarget
        val oldPaused = isPaused

        currentMap = @Suppress("DEPRECATION_ERROR") synchronized(commitMaps) {
            ensureMap(onCommit).apply { removeValue(target) }
        }
        currentTarget = target
        isPaused = false
        if (!isObserving) {
            isObserving = true
            observeAllReads(readObserver, block)
            isObserving = false
        } else {
            block()
        }
        currentMap = oldMap
        currentTarget = oldTarget
        isPaused = oldPaused
    }

    /**
     * Stops observing model reads while executing [block]. Model reads may be restarted
     * by calling [observeReads] inside [block].
     */
    fun pauseObservingReads(block: () -> Unit) {
        val oldPaused = isPaused
        isPaused = true
        block()
        isPaused = oldPaused
    }

    /**
     * Clears all model read observations for a given [target]. This clears values for all
     * `onCommit` methods passed in [observeReads].
     */
    fun clear(target: Any) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(commitMaps) {
            commitMaps.fastForEach { commitMap ->
                commitMap.map.removeValue(target)
            }
        }
    }

    /**
     * Starts or stops watching for model commits based on [enabled].
     */
    fun enableModelUpdatesObserving(enabled: Boolean): Unit = error("deprecated")

    /**
     * Calls the `onCommit` callback for the given targets.
     */
    private fun callOnCommit(targetsArray: Array<List<Any>>) {
        for (i in 0..targetsArray.lastIndex) {
            val targets = targetsArray[i]
            if (targets.isNotEmpty()) {
                @Suppress("DEPRECATION_ERROR")
                val commitCaller = synchronized(commitMaps) { commitMaps[i] }
                commitCaller.callOnCommit(targets)
            }
        }
    }

    /**
     * Returns the [ObserverMap] within [commitMaps] associated with [onCommit] or a newly-
     * inserted one if it doesn't exist.
     */
    private fun <T : Any> ensureMap(onCommit: (T) -> Unit): ObserverMap<Any, Any> {
        val index = commitMaps.indexOfFirst { it.onCommit === onCommit }
        if (index == -1) {
            val commitMap = CommitMap(onCommit)
            commitMaps.add(commitMap)
            return commitMap.map
        }
        return commitMaps[index].map
    }

    /**
     * Used to tie an `onCommit` to its target by type. This works around some difficulties in
     * unchecked casts with kotlin.
     */
    @Suppress("UNCHECKED_CAST")
    private class CommitMap<T : Any>(val onCommit: (T) -> Unit) {
        /**
         * ObserverMap (key = model, value = target). These are the models that have been
         * read during the target's [ModelObserver.observeReads].
         */
        val map = ObserverMap<Any, Any>()

        /**
         * Calls the `onCommit` callback for targets affected by the given committed values.
         */
        fun callOnCommit(targets: List<Any>) {
            targets.forEach { target ->
                onCommit(target as T)
            }
        }
    }
}
