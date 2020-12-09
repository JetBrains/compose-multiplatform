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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.collection.IdentityScopeMap

@ExperimentalComposeApi
@Suppress("DEPRECATION_ERROR")
class SnapshotStateObserver(private val onChangedExecutor: (callback: () -> Unit) -> Unit) {
    private val applyObserver: SnapshotApplyObserver = { applied, _ ->
        var hasValues = false

        synchronized(applyMaps) {
            applyMaps.forEach { applyMap ->
                val invalidated = applyMap.invalidated
                val map = applyMap.map
                for (value in applied) {
                    map.forEachScopeOf(value) { scope ->
                        invalidated += scope
                        hasValues = true
                    }
                }
                if (invalidated.isNotEmpty()) {
                    map.removeValueIf { scope -> scope in invalidated }
                }
            }
        }
        if (hasValues) {
            onChangedExecutor {
                callOnChanged()
            }
        }
    }

    /**
     * The [SnapshotReadObserver] used by this [SnapshotStateObserver] during [observeReads].
     */
    private val readObserver: SnapshotReadObserver = { state ->
        if (!isPaused) {
            currentMap!!.addValue(state)
        }
    }

    /**
     * List of all [ApplyMap]s. When [observeReads] is called, there will be a [ApplyMap]
     * associated with its `onChanged` callback in this list. The list only grows.
     */
    private val applyMaps = mutableVectorOf<ApplyMap<*>>()

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
     * The [ApplyMap] that should be added to when a model is read during [observeReads].
     */
    private var currentMap: ApplyMap<*>? = null

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
     * The [target] and [onChanged] are associated with any values that are read so that when
     * those values change, [onChanged] can be called with the [target] parameter.
     *
     * Observation for [target] will be paused when a new [observeReads] call is made or when
     * [pauseObservingReads] is called.
     *
     * Any previous observation with the given [target] and [onChanged] will be cleared when
     * the [onChanged] is called for [target]. The [onChanged] should trigger a new [observeReads]
     * call to resubscribe to changes. They may also be cleared using [removeObservationsFor]
     * or [clear].
     */
    fun <T : Any> observeReads(target: T, onChanged: (T) -> Unit, block: () -> Unit) {
        val oldMap = currentMap
        val oldPaused = isPaused
        val applyMap = synchronized(applyMaps) { ensureMap(onChanged) }
        val oldScope = applyMap.currentScope

        applyMap.currentScope = target
        currentMap = applyMap
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
        applyMap.currentScope = oldScope
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
            applyMaps.forEach { commitMap ->
                commitMap.map.removeValueIf { scope ->
                    scope === target
                }
            }
        }
    }

    /**
     * Remove observations using [predicate] to identify target scopes to be removed. This is
     * used when a scope is no longer in the hierarchy and should not receive any callbacks.
     */
    fun removeObservationsFor(predicate: (scope: Any) -> Boolean) {
        synchronized(applyMaps) {
            applyMaps.forEach { applyMap ->
                applyMap.map.removeValueIf(predicate)
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
     * This method is only used for testing. It notifies that [changes] have been made on
     * [snapshot].
     */
    @TestOnly
    fun notifyChanges(changes: Set<Any>, snapshot: Snapshot) {
        applyObserver(changes, snapshot)
    }

    /**
     * Remove all observations.
     */
    fun clear() {
        synchronized(applyMaps) {
            applyMaps.forEach { applyMap ->
                applyMap.map.clear()
            }
        }
    }

    /**
     * Calls the `onChanged` callback for the given targets.
     */
    private fun callOnChanged() {
        applyMaps.forEach { applyMap ->
            val targets = applyMap.invalidated
            if (targets.isNotEmpty()) {
                applyMap.callOnChanged(targets)
                targets.clear()
            }
        }
    }

    /**
     * Returns the [ApplyMap] within [applyMaps] associated with [onChanged] or a newly-
     * inserted one if it doesn't exist.
     *
     * Must be called inside a synchronized block.
     */
    private fun <T : Any> ensureMap(onChanged: (T) -> Unit): ApplyMap<T> {
        val index = applyMaps.indexOfFirst { it.onChanged === onChanged }
        if (index == -1) {
            val commitMap = ApplyMap(onChanged)
            applyMaps += commitMap
            return commitMap
        }
        @Suppress("UNCHECKED_CAST")
        return applyMaps[index] as ApplyMap<T>
    }

    /**
     * Used to tie an [onChanged] to its target by type. This works around some difficulties in
     * unchecked casts with kotlin.
     */
    @Suppress("UNCHECKED_CAST")
    private class ApplyMap<T : Any>(val onChanged: (T) -> Unit) {
        /**
         * Map (key = model, value = scope). These are the models that have been
         * read during the target's [SnapshotStateObserver.observeReads].
         */
        val map = IdentityScopeMap<T>()

        /**
         * Scopes that were invalidated. This and cleared during the [applyObserver] call.
         */
        val invalidated = hashSetOf<Any>()

        /**
         * Current scope that adds to [map] will use.
         */
        var currentScope: T? = null

        /**
         * Adds [value]/[currentScope] to the [map].
         */
        fun addValue(value: Any) {
            map.add(value, currentScope!!)
        }

        /**
         * Calls the `onCommit` callback for targets affected by the given committed values.
         */
        fun callOnChanged(targets: Collection<Any>) {
            targets.forEach { target ->
                onChanged(target as T)
            }
        }
    }
}
