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

import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.collection.IdentityScopeMap
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.synchronized

@Suppress("NotCloseable") // we can't implement AutoCloseable from commonMain
class SnapshotStateObserver(private val onChangedExecutor: (callback: () -> Unit) -> Unit) {
    private val applyObserver: (Set<Any>, Snapshot) -> Unit = { applied, _ ->
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
            }
        }
        if (hasValues) {
            onChangedExecutor {
                callOnChanged()
            }
        }
    }

    /**
     * The observer used by this [SnapshotStateObserver] during [observeReads].
     */
    private val readObserver: (Any) -> Unit = { state ->
        if (!isPaused) {
            synchronized(applyMaps) {
                currentMap!!.addValue(state)
            }
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
    private var applyUnsubscribe: ObserverHandle? = null

    /**
     * `true` when [withNoObservations] is called and read observations should no
     * longer be considered invalidations for the `onCommit` callback.
     */
    private var isPaused = false

    /**
     * The [ApplyMap] that should be added to when a model is read during [observeReads].
     */
    private var currentMap: ApplyMap<*>? = null

    /**
     * Executes [block], observing state object reads during its execution.
     *
     * The [scope] and [onValueChangedForScope] are associated with any values that are read so
     * that when those values change, [onValueChangedForScope] can be called with the [scope]
     * parameter.
     *
     * Observation for [scope] will be paused when a new [observeReads] call is made or when
     * [withNoObservations] is called.
     *
     * Any previous observation with the given [scope] and [onValueChangedForScope] will be
     * cleared when the [onValueChangedForScope] is called for [scope]. The
     * [onValueChangedForScope] should trigger a new [observeReads] call to resubscribe to
     * changes. They may also be cleared using [clearIf] or [clear].
     */
    fun <T : Any> observeReads(scope: T, onValueChangedForScope: (T) -> Unit, block: () -> Unit) {
        val oldMap = currentMap
        val oldPaused = isPaused
        val applyMap = synchronized(applyMaps) {
            ensureMap(onValueChangedForScope).also {
                it.map.removeScope(scope)
            }
        }
        val oldScope = applyMap.currentScope

        applyMap.currentScope = scope
        currentMap = applyMap
        isPaused = false

        Snapshot.observe(readObserver, null, block)

        currentMap = oldMap
        applyMap.currentScope = oldScope
        isPaused = oldPaused
    }

    /**
     * Stops observing state object reads while executing [block]. State object reads may be
     * restarted by calling [observeReads] inside [block].
     */
    @Deprecated(
        "Replace with Snapshot.withoutReadObservation()",
        ReplaceWith(
            "Snapshot.withoutReadObservation(block)",
            "androidx.compose.runtime.snapshots.Snapshot"
        )
    )
    fun withNoObservations(block: () -> Unit) {
        val oldPaused = isPaused
        isPaused = true
        try {
            block()
        } finally {
            isPaused = oldPaused
        }
    }

    /**
     * Clears all model read observations for a given [scope]. This clears values for all
     * `onCommit` methods passed in [observeReads].
     */
    fun clear(scope: Any) {
        synchronized(applyMaps) {
            applyMaps.forEach { commitMap ->
                commitMap.map.removeValueIf {
                    it === scope
                }
            }
        }
    }

    /**
     * Remove observations using [predicate] to identify scope scopes to be removed. This is
     * used when a scope is no longer in the hierarchy and should not receive any callbacks.
     */
    fun clearIf(predicate: (scope: Any) -> Boolean) {
        synchronized(applyMaps) {
            applyMaps.forEach { applyMap ->
                applyMap.map.removeValueIf(predicate)
            }
        }
    }

    /**
     * Starts watching for state commits.
     */
    fun start() {
        applyUnsubscribe = Snapshot.registerApplyObserver(applyObserver)
    }

    /**
     * Stops watching for state commits.
     */
    fun stop() {
        applyUnsubscribe?.dispose()
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
     * Calls the `onChanged` callback for the given scopes.
     */
    private fun callOnChanged() {
        applyMaps.forEach { applyMap ->
            val scopes = applyMap.invalidated
            if (scopes.isNotEmpty()) {
                applyMap.callOnChanged(scopes)
                scopes.clear()
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
     * Used to tie an [onChanged] to its scope by type. This works around some difficulties in
     * unchecked casts with kotlin.
     */
    @Suppress("UNCHECKED_CAST")
    private class ApplyMap<T : Any>(val onChanged: (T) -> Unit) {
        /**
         * Map (key = model, value = scope). These are the models that have been
         * read during the scope's [SnapshotStateObserver.observeReads].
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
         * Calls the `onCommit` callback for scopes affected by the given committed values.
         */
        fun callOnChanged(scopes: Collection<Any>) {
            scopes.forEach { scope ->
                onChanged(scope as T)
            }
        }
    }
}
