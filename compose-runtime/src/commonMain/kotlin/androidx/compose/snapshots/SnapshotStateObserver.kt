/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.snapshots

import androidx.compose.ExperimentalComposeApi
import androidx.compose.ObserverMap

@ExperimentalComposeApi
@Suppress("DEPRECATION_ERROR")
class SnapshotStateObserver(private val onChangedExecutor: (callback: () -> Unit) -> Unit) {
    private val applyObserver: SnapshotApplyObserver = { applied, _ ->
        var hasValues = false

        // This array is in the same order as applyMaps
        val targetsArray = synchronized(applyMaps) {
            Array(applyMaps.size) { index ->
                applyMaps[index].map[applied].apply {
                    if (isNotEmpty())
                        hasValues = true
                }
            }
        }
        if (hasValues) {
            onChangedExecutor {
                callOnChanged(targetsArray)
            }
        }
    }

    /**
     * The [SnapshotReadObserver] used by this [SnapshotStateObserver] during [observeReads].
     */
    private val readObserver: SnapshotReadObserver = { state ->
        if (!isPaused) {
            synchronized(applyMaps) {
                currentMap!!.add(state, currentTarget!!)
            }
        }
    }

    /**
     * List of all [ApplyMap]s. When [observeReads] is called, there will be a [ApplyMap]
     * associated with its `onChanged` callback in this list. The list only grows.
     */
    private val applyMaps = mutableListOf<ApplyMap<*>>()

    /**
     * Method to call when unsubscribing from the apply observer.
     */
    private var applyUnsubscribe: (() -> Unit)? = null

    /**
     * `true` when an [observeReads] is in progress and [readObserver] is active and `false` when
     * [readObserver] is no longer observing changes.
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
     * Remove all hooks used to track changes.
     */
    fun dispose() {
        require(!isObserving) { "Cannot dispose an observer while observing" }
        if (applyUnsubscribe != null) {
            enableStateUpdatesObserving(false)
        }
    }

    /**
     * Executes [block], observing state object reads during its execution.
     *
     * The [target] is stored as a weak reference to be passed to [onChanged] when a change to the
     * state object has been detected.
     *
     * Observation for [target] will be paused when a new [observeReads] call is made or when
     * [pauseObservingReads] is called.
     *
     * Any previous observation with the given [target] and [onChanged] will be cleared and only
     * the new observation on [block] will be stored. It is important that the same instance of
     * [onChanged] is used between calls or previous references will not be cleared.
     *
     * The [onChanged] will be called when a state object that was accessed during [block] has been
     * applied, and it will be called with [onChangedExecutor].
     */
    fun <T : Any> observeReads(target: T, onChanged: (T) -> Unit, block: () -> Unit) {
        val oldMap = currentMap
        val oldTarget = currentTarget
        val oldPaused = isPaused

        currentMap = synchronized(applyMaps) {
            ensureMap(onChanged).apply { removeValue(target) }
        }
        currentTarget = target
        isPaused = false
        if (!isObserving) {
            isObserving = true
            try {
                Snapshot.observe(readObserver, null, block)
            } finally {
                isObserving = false
            }
        } else {
            block()
        }
        currentMap = oldMap
        currentTarget = oldTarget
        isPaused = oldPaused
    }

    /**
     * Stops observing state object reads while executing [block]. State object reads may be
     * restarted by calling [observeReads] inside [block].
     */
    fun pauseObservingReads(block: () -> Unit) {
        val oldPaused = isPaused
        isPaused = true
        try {
            block()
        } finally {
            isPaused = oldPaused
        }
    }

    /**
     * Clears all model read observations for a given [target]. This clears values for all
     * `onCommit` methods passed in [observeReads].
     */
    fun clear(target: Any) {
        synchronized(applyMaps) {
            applyMaps.fastForEach { commitMap ->
                commitMap.map.removeValue(target)
            }
        }
    }

    /**
     * Starts or stops watching for model commits based on [enabled].
     */
    fun enableStateUpdatesObserving(enabled: Boolean) {
        require(enabled == (applyUnsubscribe == null)) {
            "Called twice with the same enabled value: $enabled"
        }
        applyUnsubscribe = if (enabled) {
            Snapshot.registerApplyObserver(applyObserver)
        } else {
            applyUnsubscribe?.invoke()
            null
        }
    }

    /**
     * Calls the `onChanged` callback for the given targets.
     */
    private fun callOnChanged(targetsArray: Array<List<Any>>) {
        for (i in 0..targetsArray.lastIndex) {
            val targets = targetsArray[i]
            if (targets.isNotEmpty()) {
                val onChangeCaller = synchronized(applyMaps) { applyMaps[i] }
                onChangeCaller.callOnChanged(targets)
            }
        }
    }

    /**
     * Returns the [ObserverMap] within [applyMaps] associated with [onChanged] or a newly-
     * inserted one if it doesn't exist.
     *
     * Must be called inside a synchronized block.
     */
    private fun <T : Any> ensureMap(onChanged: (T) -> Unit): ObserverMap<Any, Any> {
        val index = applyMaps.indexOfFirst { it.onChanged === onChanged }
        if (index == -1) {
            val commitMap = ApplyMap(onChanged)
            applyMaps.add(commitMap)
            return commitMap.map
        }
        return applyMaps[index].map
    }

    /**
     * Used to tie an [onChanged] to its target by type. This works around some difficulties in
     * unchecked casts with kotlin.
     */
    @Suppress("UNCHECKED_CAST")
    private class ApplyMap<T : Any>(val onChanged: (T) -> Unit) {
        /**
         * ObserverMap (key = model, value = target). These are the models that have been
         * read during the target's [SnapshotStateObserver.observeReads].
         */
        val map = ObserverMap<Any, Any>()

        /**
         * Calls the `onCommit` callback for targets affected by the given committed values.
         */
        fun callOnChanged(targets: List<Any>) {
            targets.forEach { target ->
                onChanged(target as T)
            }
        }
    }
}
