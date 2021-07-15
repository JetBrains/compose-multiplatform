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

@file:OptIn(InternalComposeApi::class)
package androidx.compose.runtime

import androidx.compose.runtime.collection.IdentityArrayMap
import androidx.compose.runtime.collection.IdentityArraySet
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import androidx.compose.runtime.collection.IdentityScopeMap
import androidx.compose.runtime.snapshots.fastForEach

/**
 * A composition object is usually constructed for you, and returned from an API that
 * is used to initially compose a UI. For instance, [setContent] returns a Composition.
 *
 * The [dispose] method should be used when you would like to dispose of the UI and
 * the Composition.
 */
interface Composition {
    /**
     * Returns true if any pending invalidations have been scheduled. An invalidation is schedule
     * if [RecomposeScope.invalidate] has been called on any composition scopes create for the
     * composition.
     *
     * Modifying [MutableState.value] of a value produced by [mutableStateOf] will
     * automatically call [RecomposeScope.invalidate] for any scope that read [State.value] of
     * the mutable state instance during composition.
     *
     * @see RecomposeScope
     * @see mutableStateOf
     */
    val hasInvalidations: Boolean

    /**
     * True if [dispose] has been called.
     */
    val isDisposed: Boolean

    /**
     * Clear the hierarchy that was created from the composition and release resources allocated
     * for composition. After calling [dispose] the composition will no longer be recomposed and
     * calling [setContent] will throw an [IllegalStateException]. Calling [dispose] is
     * idempotent, all calls after the first are a no-op.
     */
    fun dispose()

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the content of the composition.
     * @exception IllegalStateException thrown in the composition has been [dispose]d.
     */
    fun setContent(content: @Composable () -> Unit)
}

/**
 * A controlled composition is a [Composition] that can be directly controlled by the caller.
 *
 * This is the interface used by the [Recomposer] to control how and when a composition is
 * invalidated and subsequently recomposed.
 *
 * Normally a composition is controlled by the [Recomposer] but it is often more efficient for
 * tests to take direct control over a composition by calling [ControlledComposition] instead of
 * [Composition].
 *
 * @see ControlledComposition
 */
interface ControlledComposition : Composition {
    /**
     * True if the composition is actively compositing such as when actively in a call to
     * [composeContent] or [recompose].
     */
    val isComposing: Boolean

    /**
     * True after [composeContent] or [recompose] has been called and [applyChanges] is expected
     * as the next call. An exception will be throw in [composeContent] or [recompose] is called
     * while there are pending from the previous composition pending to be applied.
     */
    val hasPendingChanges: Boolean

    /**
     * Called by the parent composition in response to calling [setContent]. After this method
     * the changes should be calculated but not yet applied. DO NOT call this method directly if
     * this is interface is controlled by a [Recomposer], either use [setContent] or
     * [Recomposer.composeInitial] instead.
     *
     * @param content A composable function that describes the tree.
     */
    fun composeContent(content: @Composable () -> Unit)

    /**
     * Record the values that were modified after the last call to [recompose] or from the
     * initial call to [composeContent]. This should be called before [recompose] is called to
     * record which parts of the composition need to be recomposed.
     *
     * @param values the set of values that have changed since the last composition.
     */
    fun recordModificationsOf(values: Set<Any>)

    /**
     * Returns true if any of the object instances in [values] is observed by this composition.
     * This allows detecting if values changed by a previous composition will potentially affect
     * this composition.
     */
    fun observesAnyOf(values: Set<Any>): Boolean

    /**
     * Execute [block] with [isComposing] set temporarily to `true`. This allows treating
     * invalidations reported during [prepareCompose] as if they happened while composing to avoid
     * double invalidations when propagating changes from a parent composition while before
     * composing the child composition.
     */
    fun prepareCompose(block: () -> Unit)

    /**
     * Record that [value] has been read. This is used primarily by the [Recomposer] to inform the
     * composer when the a [MutableState] instance has been read implying it should be observed
     * for changes.
     *
     * @param value the instance from which a property was read
     */
    fun recordReadOf(value: Any)

    /**
     * Record that [value] has been modified. This is used primarily by the [Recomposer] to inform
     * the composer when the a [MutableState] instance been change by a composable function.
     */
    fun recordWriteOf(value: Any)

    /**
     * Recompose the composition to calculate any changes necessary to the composition state and
     * the tree maintained by the applier. No changes have been made yet. Changes calculated will
     * be applied when [applyChanges] is called.
     *
     * @return returns `true` if any changes are pending and [applyChanges] should be called.
     */
    fun recompose(): Boolean

    /**
     * Apply the changes calculated during [setContent] or [recompose]. If an exception is thrown
     * by [applyChanges] the composition is irreparably damaged and should be [dispose]d.
     */
    fun applyChanges()

    /**
     * Invalidate all invalidation scopes. This is called, for example, by [Recomposer] when the
     * Recomposer becomes active after a previous period of inactivity, potentially missing more
     * granular invalidations.
     */
    fun invalidateAll()

    /**
     * Throws an exception if the internal state of the composer has been corrupted and is no
     * longer consistent. Used in testing the composer itself.
     */
    @InternalComposeApi
    fun verifyConsistent()
}

/**
 * The [CoroutineContext] that should be used to perform concurrent recompositions of this
 * [ControlledComposition] when used in an environment supporting concurrent composition.
 *
 * See [Recomposer.runRecomposeConcurrentlyAndApplyChanges] as an example of configuring
 * such an environment.
 */
// Implementation note: as/if this method graduates it should become a real method of
// ControlledComposition with a default implementation.
@ExperimentalComposeApi
val ControlledComposition.recomposeCoroutineContext: CoroutineContext
    @ExperimentalComposeApi
    get() = (this as? CompositionImpl)?.recomposeContext ?: EmptyCoroutineContext

/**
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionContext] can be provided to make the composition behave as a sub-composition of
 * the parent or a [Recomposer] can be provided.
 *
 * It is important to call [Composition.dispose] this composer is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 *
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
fun Composition(
    applier: Applier<*>,
    parent: CompositionContext
): Composition =
    CompositionImpl(
        parent,
        applier
    )

/**
 * This method is a way to initiate a composition. Optionally, a [parent]
 * [CompositionContext] can be provided to make the composition behave as a sub-composition of
 * the parent or a [Recomposer] can be provided.
 *
 * A controlled composition allows direct control of the composition instead of it being
 * controlled by the [Recomposer] passed ot the root composition.
 *
 * It is important to call [Composition.dispose] this composer is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 *
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
@TestOnly
fun ControlledComposition(
    applier: Applier<*>,
    parent: CompositionContext
): ControlledComposition =
    CompositionImpl(
        parent,
        applier
    )

/**
 * Create a [Composition] using [applier] to manage the composition, as a child of [parent].
 *
 * When used in a configuration that supports concurrent recomposition, hint to the environment
 * that [recomposeCoroutineContext] should be used to perform recomposition. Recompositions will
 * be launched into the
 */
@ExperimentalComposeApi
fun Composition(
    applier: Applier<*>,
    parent: CompositionContext,
    recomposeCoroutineContext: CoroutineContext
): Composition = CompositionImpl(
    parent,
    applier,
    recomposeContext = recomposeCoroutineContext
)

@TestOnly
@ExperimentalComposeApi
fun ControlledComposition(
    applier: Applier<*>,
    parent: CompositionContext,
    recomposeCoroutineContext: CoroutineContext
): ControlledComposition = CompositionImpl(
    parent,
    applier,
    recomposeContext = recomposeCoroutineContext
)

private val PendingApplyNoModifications = Any()

/**
 * The implementation of the [Composition] interface.
 *
 * @param parent An optional reference to the parent composition.
 * @param applier The applier to use to manage the tree built by the composer.
 * @param recomposeContext The coroutine context to use to recompose this composition. If left
 * `null` the controlling recomposer's default context is used.
 */
internal class CompositionImpl(
    /**
     * The parent composition from [rememberCompositionContext], for sub-compositions, or the an
     * instance of [Recomposer] for root compositions.
     */
    private val parent: CompositionContext,

    /**
     * The applier to use to update the tree managed by the composition.
     */
    private val applier: Applier<*>,

    recomposeContext: CoroutineContext? = null
) : ControlledComposition {
    /**
     * `null` if a composition isn't pending to apply.
     * `Set<Any>` or `Array<Set<Any>>` if there are modifications to record
     * [PendingApplyNoModifications] if a composition is pending to apply, no modifications.
     * any set contents will be sent to [recordModificationsOf] after applying changes
     * before releasing [lock]
     */
    private val pendingModifications = AtomicReference<Any?>(null)

    // Held when making changes to self or composer
    private val lock = Any()

    /**
     * A set of remember observers that were potentially abandoned between [composeContent] or
     * [recompose] and [applyChanges]. When inserting new content any newly remembered
     * [RememberObserver]s are added to this set and then removed as [RememberObserver.onRemembered]
     * is dispatched. If any are left in this when exiting [applyChanges] they have been
     * abandoned and are sent an [RememberObserver.onAbandoned] notification.
     */
    private val abandonSet = HashSet<RememberObserver>()

    /**
     * The slot table is used to store the composition information required for recomposition.
     */
    private val slotTable = SlotTable()

    /**
     * A map of observable objects to the [RecomposeScope]s that observe the object. If the key
     * object is modified the associated scopes should be invalidated.
     */
    private val observations = IdentityScopeMap<RecomposeScopeImpl>()

    /**
     * A map of object read during derived states to the corresponding derived state.
     */
    private val derivedStates = IdentityScopeMap<DerivedState<*>>()

    /**
     * A list of changes calculated by [Composer] to be applied to the [Applier] and the
     * [SlotTable] to reflect the result of composition. This is a list of lambdas that need to
     * be invoked in order to produce the desired effects.
     */
    private val changes = mutableListOf<Change>()

    /**
     * When an observable object is modified during composition any recompose scopes that are
     * observing that object are invalidated immediately. Since they have already been processed
     * there is no need to process them again, so this set maintains a set of the recompose
     * scopes that were already dismissed by composition and should be ignored in the next call
     * to [recordModificationsOf].
     */
    private val observationsProcessed = IdentityScopeMap<RecomposeScopeImpl>()

    /**
     * A map of the invalid [RecomposeScope]s. If this map is non-empty the current state of
     * the composition does not reflect the current state of the objects it observes and should
     * be recomposed by calling [recompose]. Tbe value is a map of values that invalidated the
     * scope. The scope is checked with these instances to ensure the value has changed. This is
     * used to only invalidate the scope if a [derivedStateOf] object changes.
     */
    private var invalidations = IdentityArrayMap<RecomposeScopeImpl, IdentityArraySet<Any>?>()

    /**
     * As [RecomposeScope]s are removed the corresponding entries in the observations set must be
     * removed as well. This process is expensive so should only be done if it is certain the
     * [observations] set contains [RecomposeScope] that is no longer needed. [pendingInvalidScopes]
     * is set to true whenever a [RecomposeScope] is removed from the [slotTable].
     */
    internal var pendingInvalidScopes = false

    /**
     * The [Composer] to use to create and update the tree managed by this composition.
     */
    private val composer: ComposerImpl =
        ComposerImpl(
            applier = applier,
            parentContext = parent,
            slotTable = slotTable,
            abandonSet = abandonSet,
            changes = changes,
            composition = this
        ).also {
            parent.registerComposer(it)
        }

    /**
     * The [CoroutineContext] override, if there is one, for this composition.
     */
    private val _recomposeContext: CoroutineContext? = recomposeContext

    /**
     * the [CoroutineContext] to use to [recompose] this composition.
     */
    val recomposeContext: CoroutineContext
        get() = _recomposeContext ?: parent.recomposeCoroutineContext

    /**
     * Return true if this is a root (non-sub-) composition.
     */
    val isRoot: Boolean = parent is Recomposer

    /**
     * True if [dispose] has been called.
     */
    private var disposed = false

    /**
     * True if a sub-composition of this composition is current composing.
     */
    private val areChildrenComposing get() = composer.areChildrenComposing

    /**
     * The [Composable] function used to define the tree managed by this composition. This is set
     * by [setContent].
     */
    var composable: @Composable () -> Unit = {}

    override val isComposing: Boolean
        get() = composer.isComposing

    override val isDisposed: Boolean get() = disposed

    override val hasPendingChanges: Boolean
        get() = synchronized(lock) { composer.hasPendingChanges }

    override fun setContent(content: @Composable () -> Unit) {
        check(!disposed) { "The composition is disposed" }
        this.composable = content
        parent.composeInitial(this, composable)
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsForCompositionLocked() {
        // Recording modifications may race for lock. If there are pending modifications
        // and we won the lock race, drain them before composing.
        when (val toRecord = pendingModifications.getAndSet(PendingApplyNoModifications)) {
            null -> {
                // Do nothing, just start composing.
            }
            PendingApplyNoModifications -> error("pending composition has not been applied")
            is Set<*> -> addPendingInvalidationsLocked(toRecord as Set<Any>)
            is Array<*> -> for (changed in toRecord as Array<Set<Any>>) {
                addPendingInvalidationsLocked(changed)
            }
            else -> error("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsLocked() {
        when (val toRecord = pendingModifications.getAndSet(null)) {
            PendingApplyNoModifications -> {
                // No work to do
            }
            is Set<*> -> addPendingInvalidationsLocked(toRecord as Set<Any>)
            is Array<*> -> for (changed in toRecord as Array<Set<Any>>) {
                addPendingInvalidationsLocked(changed)
            }
            null -> error(
                "calling recordModificationsOf and applyChanges concurrently is not supported"
            )
            else -> error(
                "corrupt pendingModifications drain: $pendingModifications"
            )
        }
    }

    override fun composeContent(content: @Composable () -> Unit) {
        // TODO: This should raise a signal to any currently running recompose calls
        // to halt and return
        synchronized(lock) {
            drainPendingModificationsForCompositionLocked()
            composer.composeContent(takeInvalidations(), content)
        }
    }

    override fun dispose() {
        synchronized(lock) {
            if (!disposed) {
                disposed = true
                composable = {}
                if (slotTable.groupsSize > 0) {
                    val manager = RememberEventDispatcher(abandonSet)
                    slotTable.write { writer ->
                        writer.removeCurrentGroup(manager)
                    }
                    applier.clear()
                    manager.dispatchRememberObservers()
                }
                composer.dispose()
                parent.unregisterComposition(this)
            }
        }
    }

    override val hasInvalidations get() = synchronized(lock) { invalidations.size > 0 }

    /**
     * To bootstrap multithreading handling, recording modifications is now deferred between
     * recomposition with changes to apply and the application of those changes.
     * [pendingModifications] will contain a queue of changes to apply once all current changes
     * have been successfully processed. Draining this queue is the responsibility of [recompose]
     * if it would return `false` (changes do not need to be applied) or [applyChanges].
     */
    @Suppress("UNCHECKED_CAST")
    override fun recordModificationsOf(values: Set<Any>) {
        while (true) {
            val old = pendingModifications.get()
            val new: Any = when (old) {
                null, PendingApplyNoModifications -> values
                is Set<*> -> arrayOf(old, values)
                is Array<*> -> (old as Array<Set<Any>>) + values
                else -> error("corrupt pendingModifications: $pendingModifications")
            }
            if (pendingModifications.compareAndSet(old, new)) {
                if (old == null) {
                    synchronized(lock) {
                        drainPendingModificationsLocked()
                    }
                }
                break
            }
        }
    }

    override fun observesAnyOf(values: Set<Any>): Boolean {
        for (value in values) {
            if (value in observations || value in derivedStates) return true
        }
        return false
    }

    override fun prepareCompose(block: () -> Unit) = composer.prepareCompose(block)

    private fun addPendingInvalidationsLocked(values: Set<Any>) {
        var invalidated: HashSet<RecomposeScopeImpl>? = null

        fun invalidate(value: Any) {
            observations.forEachScopeOf(value) { scope ->
                if (
                    !observationsProcessed.remove(value, scope) &&
                    scope.invalidateForResult(value) != InvalidationResult.IGNORED
                ) {
                    val set = invalidated
                        ?: HashSet<RecomposeScopeImpl>().also {
                            invalidated = it
                        }
                    set.add(scope)
                }
            }
        }

        for (value in values) {
            if (value is RecomposeScopeImpl) {
                value.invalidateForResult(null)
            } else {
                invalidate(value)
                derivedStates.forEachScopeOf(value) {
                    invalidate(it)
                }
            }
        }
        invalidated?.let {
            observations.removeValueIf { scope -> scope in it }
        }
    }

    override fun recordReadOf(value: Any) {
        // Not acquiring lock since this happens during composition with it already held
        if (!areChildrenComposing) {
            composer.currentRecomposeScope?.let {
                it.used = true
                observations.add(value, it)

                // Record derived state dependency mapping
                if (value is DerivedState<*>) {
                    value.dependencies.forEach { dependency ->
                        derivedStates.add(dependency, value)
                    }
                }

                it.recordRead(value)
            }
        }
    }

    private fun invalidateScopeOfLocked(value: Any) {
        // Invalidate any recompose scopes that read this value.
        observations.forEachScopeOf(value) { scope ->
            if (scope.invalidateForResult(value) == InvalidationResult.IMMINENT) {
                // If we process this during recordWriteOf, ignore it when recording modifications
                observationsProcessed.add(value, scope)
            }
        }
    }

    override fun recordWriteOf(value: Any) = synchronized(lock) {
        invalidateScopeOfLocked(value)

        // If writing to dependency of a derived value and the value is changed, invalidate the
        // scopes that read the derived value.
        derivedStates.forEachScopeOf(value) {
            invalidateScopeOfLocked(it)
        }
    }

    override fun recompose(): Boolean = synchronized(lock) {
        drainPendingModificationsForCompositionLocked()
        composer.recompose(takeInvalidations()).also { shouldDrain ->
            // Apply would normally do this for us; do it now if apply shouldn't happen.
            if (!shouldDrain) drainPendingModificationsLocked()
        }
    }

    override fun applyChanges() {
        synchronized(lock) {
            val manager = RememberEventDispatcher(abandonSet)
            try {
                applier.onBeginChanges()

                // Apply all changes
                slotTable.write { slots ->
                    val applier = applier
                    changes.fastForEach { change ->
                        change(applier, slots, manager)
                    }
                    changes.clear()
                }

                applier.onEndChanges()

                // Side effects run after lifecycle observers so that any remembered objects
                // that implement RememberObserver receive onRemembered before a side effect
                // that captured it and operates on it can run.
                manager.dispatchRememberObservers()
                manager.dispatchSideEffects()

                if (pendingInvalidScopes) {
                    pendingInvalidScopes = false
                    observations.removeValueIf { scope -> !scope.valid }
                    derivedStates.removeValueIf { derivedValue -> derivedValue !in observations }
                }
            } finally {
                manager.dispatchAbandons()
            }
            drainPendingModificationsLocked()
        }
    }

    override fun invalidateAll() {
        synchronized(lock) {
            slotTable.slots.forEach { (it as? RecomposeScopeImpl)?.invalidate() }
        }
    }

    override fun verifyConsistent() {
        synchronized(lock) {
            if (!isComposing) {
                slotTable.verifyWellFormed()
                validateRecomposeScopeAnchors(slotTable)
            }
        }
    }

    fun invalidate(scope: RecomposeScopeImpl, instance: Any?): InvalidationResult {
        if (scope.defaultsInScope) {
            scope.defaultsInvalid = true
        }
        val anchor = scope.anchor
        if (anchor == null || !slotTable.ownsAnchor(anchor) || !anchor.valid)
            return InvalidationResult.IGNORED // The scope has not yet entered the composition
        val location = anchor.toIndexFor(slotTable)
        if (location < 0)
            return InvalidationResult.IGNORED // The scope was removed from the composition
        if (isComposing && composer.tryImminentInvalidation(scope, instance)) {
            // The invalidation was redirected to the composer.
            return InvalidationResult.IMMINENT
        }

        // invalidations[scope] containing an explicit null means it was invalidated
        // unconditionally.
        if (instance == null) {
            invalidations[scope] = null
        } else {
            invalidations.addValue(scope, instance)
        }

        parent.invalidate(this)
        return if (isComposing) InvalidationResult.DEFERRED else InvalidationResult.SCHEDULED
    }

    internal fun removeObservation(instance: Any, scope: RecomposeScopeImpl) {
        observations.remove(instance, scope)
    }

    /**
     * This takes ownership of the current invalidations and sets up a new array map to hold the
     * new invalidations.
     */
    private fun takeInvalidations(): IdentityArrayMap<RecomposeScopeImpl, IdentityArraySet<Any>?> {
        val invalidations = invalidations
        this.invalidations = IdentityArrayMap()
        return invalidations
    }

    /**
     * Helper for [verifyConsistent] to ensure the anchor match there respective invalidation
     * scopes.
     */
    private fun validateRecomposeScopeAnchors(slotTable: SlotTable) {
        val scopes = slotTable.slots.mapNotNull { it as? RecomposeScopeImpl }
        scopes.fastForEach { scope ->
            scope.anchor?.let { anchor ->
                check(scope in slotTable.slotsOf(anchor.toIndexFor(slotTable))) {
                    val dataIndex = slotTable.slots.indexOf(scope)
                    "Misaligned anchor $anchor in scope $scope encountered, scope found at " +
                        "$dataIndex"
                }
            }
        }
    }

    /**
     * Helper for collecting remember observers for later strictly ordered dispatch.
     *
     * This includes support for the deprecated [RememberObserver] which should be
     * removed with it.
     */
    private class RememberEventDispatcher(
        private val abandoning: MutableSet<RememberObserver>
    ) : RememberManager {
        private val remembering = mutableListOf<RememberObserver>()
        private val forgetting = mutableListOf<RememberObserver>()
        private val sideEffects = mutableListOf<() -> Unit>()

        override fun remembering(instance: RememberObserver) {
            forgetting.lastIndexOf(instance).let { index ->
                if (index >= 0) {
                    forgetting.removeAt(index)
                    abandoning.remove(instance)
                } else {
                    remembering.add(instance)
                }
            }
        }

        override fun forgetting(instance: RememberObserver) {
            remembering.lastIndexOf(instance).let { index ->
                if (index >= 0) {
                    remembering.removeAt(index)
                    abandoning.remove(instance)
                } else {
                    forgetting.add(instance)
                }
            }
        }

        override fun sideEffect(effect: () -> Unit) {
            sideEffects += effect
        }

        fun dispatchRememberObservers() {
            // Send forgets
            if (forgetting.isNotEmpty()) {
                for (i in forgetting.size - 1 downTo 0) {
                    val instance = forgetting[i]
                    if (instance !in abandoning) {
                        instance.onForgotten()
                    }
                }
            }

            // Send remembers
            if (remembering.isNotEmpty()) {
                remembering.fastForEach { instance ->
                    abandoning.remove(instance)
                    instance.onRemembered()
                }
            }
        }

        fun dispatchSideEffects() {
            if (sideEffects.isNotEmpty()) {
                sideEffects.fastForEach { sideEffect ->
                    sideEffect()
                }
                sideEffects.clear()
            }
        }

        fun dispatchAbandons() {
            if (abandoning.isNotEmpty()) {
                val iterator = abandoning.iterator()
                while (iterator.hasNext()) {
                    val instance = iterator.next()
                    iterator.remove()
                    instance.onAbandoned()
                }
            }
        }
    }
}

/**
 * Apply Code Changes will invoke the two functions before and after a code swap.
 *
 * This forces the whole view hierarchy to be redrawn to invoke any code change that was
 * introduce in the code swap.
 *
 * All these are private as within JVMTI / JNI accessibility is mostly a formality.
 */
private class HotReloader {
    companion object {
        // Called before Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun saveStateAndDispose(context: Any): Any {
            return Recomposer.saveStateAndDisposeForHotReload()
        }

        // Called after Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun loadStateAndCompose(token: Any) {
            Recomposer.loadStateAndComposeForHotReload(token)
        }

        @TestOnly
        internal fun simulateHotReload(context: Any) {
            loadStateAndCompose(saveStateAndDispose(context))
        }
    }
}

/**
 * @suppress
 */
@TestOnly
fun simulateHotReload(context: Any) = HotReloader.simulateHotReload(context)

private fun <K : Any, V : Any> IdentityArrayMap<K, IdentityArraySet<V>?>.addValue(
    key: K,
    value: V
) {
    if (key in this) {
        this[key]?.add(value)
    } else {
        this[key] = IdentityArraySet<V>().also { it.add(value) }
    }
}
