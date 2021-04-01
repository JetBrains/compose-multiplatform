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

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A composition object is usually constructed for you, and returned from an API that
 * is used to initially compose a UI. For instance, [setContent] returns a Composition.
 *
 * The [dispose] method should be used when you would like to dispose of the UI and
 * the Composition.
 */
interface Composition {
    /**
     * Returns true if any pending invalidations have been scheduled.
     */
    val hasInvalidations: Boolean

    /**
     * True if [dispose] has been called.
     */
    val isDisposed: Boolean

    /**
     * Clear the hierarchy that was created from the composition.
     */
    fun dispose()

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the tree.
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
 * @param parent The parent composition reference, if applicable. Default is null.
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
 * This method is the way to initiate a composition. Optionally, a [parent]
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
 * @param parent The parent composition reference, if applicable. Default is null.
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
 * @param parent An optional reference to the parent composition.
 * @param applier The applier to use to manage the tree built by the composer.
 * @param onDispose A callback to be triggered when [dispose] is called.
 */
internal class CompositionImpl(
    private val parent: CompositionContext,
    applier: Applier<*>,
    private val onDispose: (() -> Unit)? = null,
    recomposeContext: CoroutineContext? = null
) : ControlledComposition {

    /**
     * `null` if a composition isn't pending to apply.
     * `Set<Any>` or `Array<Set<Any>>` if there are modifications to record
     * [PendingApplyNoModifications] if a composition is pending to apply, no modifications.
     * any set contents will be sent to [ComposerImpl.recordModificationsOf] after applying changes
     * before releasing [lock]
     */
    private val pendingModifications = AtomicReference<Any?>(null)

    // Held when making changes to self or composer
    private val lock = Any()

    private val composer: ComposerImpl = ComposerImpl(applier, parent, this).also {
        parent.registerComposer(it)
    }

    private val _recomposeContext: CoroutineContext? = recomposeContext

    val recomposeContext: CoroutineContext
        get() = _recomposeContext ?: parent.recomposeCoroutineContext

    /**
     * Return true if this is a root (non-sub-) composition.
     */
    val isRoot: Boolean = parent is Recomposer

    private var disposed = false

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
            is Set<*> -> composer.recordModificationsOf(toRecord as Set<Any>)
            is Array<*> -> for (changed in toRecord as Array<Set<Any>>) {
                composer.recordModificationsOf(changed)
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
            is Set<*> -> composer.recordModificationsOf(toRecord as Set<Any>)
            is Array<*> -> for (changed in toRecord as Array<Set<Any>>) {
                composer.recordModificationsOf(changed)
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
            composer.composeContent(content)
        }
    }

    override fun dispose() {
        synchronized(lock) {
            if (!disposed) {
                disposed = true
                composable = {}
                composer.dispose()
                parent.unregisterComposition(this)
                onDispose?.invoke()
            }
        }
    }

    override val hasInvalidations get() = synchronized(lock) { composer.hasInvalidations }

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

    override fun recordReadOf(value: Any) {
        // Not acquiring lock since this happens during composition with it already held
        composer.recordReadOf(value)
    }

    override fun recordWriteOf(value: Any) {
        // Not acquiring lock since this happens during composition with it already held
        composer.recordWriteOf(value)
    }

    override fun recompose(): Boolean = synchronized(lock) {
        drainPendingModificationsForCompositionLocked()
        composer.recompose().also { shouldDrain ->
            // Apply would normally do this for us; do it now if apply shouldn't happen.
            if (!shouldDrain) drainPendingModificationsLocked()
        }
    }

    override fun applyChanges() {
        synchronized(lock) {
            composer.applyChanges()
            drainPendingModificationsLocked()
        }
    }

    override fun invalidateAll() {
        synchronized(lock) {
            composer.invalidateAll()
        }
    }

    override fun verifyConsistent() {
        synchronized(lock) {
            composer.verifyConsistent()
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
