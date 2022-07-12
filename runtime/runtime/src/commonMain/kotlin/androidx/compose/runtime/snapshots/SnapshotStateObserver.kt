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

import androidx.compose.runtime.DerivedState
import androidx.compose.runtime.State
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.collection.IdentityArrayMap
import androidx.compose.runtime.collection.IdentityArraySet
import androidx.compose.runtime.collection.IdentityScopeMap
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.observeDerivedStateRecalculations
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Helper class to efficiently observe snapshot state reads. See [observeReads] for more details.
 *
 * NOTE: This class is not thread-safe, so implementations should not reuse observer between
 * different threads to avoid race conditions.
 */
@Suppress("NotCloseable") // we can't implement AutoCloseable from commonMain
class SnapshotStateObserver(private val onChangedExecutor: (callback: () -> Unit) -> Unit) {
    private val applyObserver: (Set<Any>, Snapshot) -> Unit = { applied, _ ->
        var hasValues = false

        forEachScopeMap { scopeMap ->
            hasValues = scopeMap.recordInvalidation(applied) || hasValues
        }
        if (hasValues) {
            onChangedExecutor {
                forEachScopeMap { scopeMap ->
                    scopeMap.notifyInvalidatedScopes()
                }
            }
        }
    }

    /**
     * The observer used by this [SnapshotStateObserver] during [observeReads].
     */
    private val readObserver: (Any) -> Unit = { state ->
        if (!isPaused) {
            synchronized(observedScopeMaps) {
                currentMap!!.recordRead(state)
            }
        }
    }

    /**
     * List of all [ObservedScopeMap]s. When [observeReads] is called, there will be a
     * [ObservedScopeMap] associated with its [ObservedScopeMap.onChanged] callback in this list.
     * The list only grows.
     */
    private val observedScopeMaps = mutableVectorOf<ObservedScopeMap>()

    /**
     * Helper for synchronized iteration over [observedScopeMaps]. All observed reads should
     * happen on the same thread, but snapshots can be applied on a different thread, requiring
     * synchronization.
     */
    private inline fun forEachScopeMap(block: (ObservedScopeMap) -> Unit) {
        synchronized(observedScopeMaps) {
            observedScopeMaps.forEach(block)
        }
    }

    /**
     * Method to call when unsubscribing from the apply observer.
     */
    private var applyUnsubscribe: ObserverHandle? = null

    /**
     * `true` when [withNoObservations] is called and read observations should not
     * be considered invalidations for the current scope.
     */
    private var isPaused = false

    /**
     * The [ObservedScopeMap] that should be added to when a model is read during [observeReads].
     */
    private var currentMap: ObservedScopeMap? = null

    /**
     * Executes [block], observing state object reads during its execution.
     *
     * The [scope] and [onValueChangedForScope] are associated with any values that are read so
     * that when those values change, [onValueChangedForScope] will be called with the [scope]
     * parameter.
     *
     * Observation can be paused with [Snapshot.withoutReadObservation].
     *
     * @param scope value associated with the observed scope.
     * @param onValueChangedForScope is called with the [scope] when value read within [block]
     * has been changed. For repeated observations, it is more performant to pass the same instance
     * of the callback, as [observedScopeMaps] grows with each new callback instance.
     * @param block to observe reads within.
     */
    fun <T : Any> observeReads(scope: T, onValueChangedForScope: (T) -> Unit, block: () -> Unit) {
        val scopeMap = synchronized(observedScopeMaps) {
            ensureMap(onValueChangedForScope).also {
                it.clearScopeObservations(scope)
            }
        }

        val oldPaused = isPaused
        val oldMap = currentMap
        val oldScope = scopeMap.currentScope

        try {
            isPaused = false
            currentMap = scopeMap
            scopeMap.currentScope = scope

            observeDerivedStateRecalculations(
                start = scopeMap.derivedStateEnterObserver,
                done = scopeMap.derivedStateExitObserver
            ) {
                Snapshot.observe(readObserver, null, block)
            }
        } finally {
            scopeMap.currentScope = oldScope
            currentMap = oldMap
            isPaused = oldPaused
        }
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
     * Clears all state read observations for a given [scope]. This clears values for all
     * `onValueChangedForScope` callbacks passed in [observeReads].
     */
    fun clear(scope: Any) {
        forEachScopeMap {
            it.clearScopeObservations(scope)
        }
    }

    /**
     * Remove observations using [predicate] to identify scopes to be removed. This is
     * used when a scope is no longer in the hierarchy and should not receive any callbacks.
     */
    fun clearIf(predicate: (scope: Any) -> Boolean) {
        forEachScopeMap { scopeMap ->
            scopeMap.removeScopeIf(predicate)
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
        forEachScopeMap { scopeMap ->
            scopeMap.clear()
        }
    }

    /**
     * Returns the [ObservedScopeMap] within [observedScopeMaps] associated with [onChanged] or a newly-
     * inserted one if it doesn't exist.
     *
     * Must be called inside a synchronized block.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ensureMap(onChanged: (T) -> Unit): ObservedScopeMap {
        val scopeMap = observedScopeMaps.firstOrNull { it.onChanged === onChanged }
        if (scopeMap == null) {
            val map = ObservedScopeMap(onChanged as ((Any) -> Unit))
            observedScopeMaps += map
            return map
        }
        return scopeMap
    }

    /**
     * Connects observed values to scopes for each [onChanged] callback.
     */
    @Suppress("UNCHECKED_CAST")
    private class ObservedScopeMap(val onChanged: (Any) -> Unit) {
        /**
         * Currently observed scope.
         */
        var currentScope: Any? = null

        /**
         * Start observer for derived state recalculation
         */
        val derivedStateEnterObserver: (State<*>) -> Unit = { deriveStateScopeCount++ }

        /**
         * Exit observer for derived state recalculation
         */
        val derivedStateExitObserver: (State<*>) -> Unit = { deriveStateScopeCount-- }

        /**
         * Counter for skipping reads inside derived states. If count is > 0, read happens inside
         * a derived state.
         * Reads for derived states are captured separately through [DerivedState.dependencies].
         */
        private var deriveStateScopeCount = 0

        /**
         * Values that have been read during the scope's [SnapshotStateObserver.observeReads].
         */
        private val valueToScopes = IdentityScopeMap<Any>()

        /**
         * Reverse index (scope -> values) for faster scope invalidation.
         */
        private val scopeToValues: IdentityArrayMap<Any, IdentityArraySet<Any>> =
            IdentityArrayMap()

        /**
         * Scopes that were invalidated during previous apply step.
         */
        private val invalidated = hashSetOf<Any>()

        /**
         * Invalidation index from state objects to derived states reading them.
         */
        private val dependencyToDerivedStates = IdentityScopeMap<DerivedState<*>>()

        /**
         * Last derived state value recorded during read.
         */
        private val recordedDerivedStateValues = HashMap<DerivedState<*>, Any?>()

        /**
         * Record that [value] was read in [currentScope].
         */
        fun recordRead(value: Any) {
            if (deriveStateScopeCount > 0) {
                // Reads coming from derivedStateOf block
                return
            }

            val scope = currentScope!!
            valueToScopes.add(value, scope)

            val recordedValues = scopeToValues[scope]
                ?: IdentityArraySet<Any>().also { scopeToValues[scope] = it }
            recordedValues.add(value)

            if (value is DerivedState<*>) {
                val dependencies = value.dependencies
                for (dependency in dependencies) {
                    // skip over dependency array
                    if (dependency == null) break
                    dependencyToDerivedStates.add(dependency, value)
                }
                recordedDerivedStateValues[value] = value.currentValue
            }
        }

        /**
         * Clear observations for [scope].
         */
        fun clearScopeObservations(scope: Any) {
            val recordedValues = scopeToValues[scope] ?: return
            recordedValues.fastForEach {
                removeObservation(scope, it)
            }
            // clearing the scope usually means that we are about to start observation again
            // so it doesn't make sense to reallocate the set
            recordedValues.clear()
        }

        /**
         * Remove observations in scopes matching [predicate].
         */
        fun removeScopeIf(predicate: (scope: Any) -> Boolean) {
            scopeToValues.removeIf { scope, valueSet ->
                val willRemove = predicate(scope)
                if (willRemove) {
                    valueSet.fastForEach {
                        removeObservation(scope, it)
                    }
                }
                willRemove
            }
        }

        private fun removeObservation(scope: Any, value: Any) {
            valueToScopes.remove(value, scope)
            if (value is DerivedState<*> && value !in valueToScopes) {
                dependencyToDerivedStates.removeScope(value)
                recordedDerivedStateValues.remove(value)
            }
        }

        /**
         * Clear all observations.
         */
        fun clear() {
            valueToScopes.clear()
            scopeToValues.clear()
            dependencyToDerivedStates.clear()
            recordedDerivedStateValues.clear()
        }

        /**
         * Record scope invalidation for given set of values.
         * @return whether any scopes observe changed values
         */
        fun recordInvalidation(changes: Set<Any>): Boolean {
            var hasValues = false
            for (value in changes) {
                if (value in dependencyToDerivedStates) {
                    // Find derived state that is invalidated by this change
                    dependencyToDerivedStates.forEachScopeOf(value) { derivedState ->
                        derivedState as DerivedState<Any?>
                        val previousValue = recordedDerivedStateValues[derivedState]
                        val policy = derivedState.policy ?: structuralEqualityPolicy()

                        // Invalidate only if currentValue is different than observed on read
                        if (!policy.equivalent(derivedState.currentValue, previousValue)) {
                            valueToScopes.forEachScopeOf(derivedState) { scope ->
                                invalidated += scope
                                hasValues = true
                            }
                        }
                    }
                }

                valueToScopes.forEachScopeOf(value) { scope ->
                    invalidated += scope
                    hasValues = true
                }
            }
            return hasValues
        }

        /**
         * Call [onChanged] for previously invalidated scopes.
         */
        fun notifyInvalidatedScopes() {
            invalidated.forEach(onChanged)
            invalidated.clear()
        }
    }
}
