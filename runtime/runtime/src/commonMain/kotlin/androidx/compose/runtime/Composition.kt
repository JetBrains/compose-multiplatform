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
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionContext] can be provided to make the composition behave as a sub-composition of
 * the parent or a [Recomposer] can be provided.
 *
 * It is important to call [Composition.dispose] whenever this [key] is no longer needed in
 * order to release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param key The object this composition will be tied to. Only one [Composition] will be created
 * for a given [key]. If the same [key] is passed in subsequent calls, the same [Composition]
 * instance will be returned.
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent composition reference, if applicable. Default is null.
 * @param onCreated A function which will be executed only when the Composition is created.
 *
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
@ExperimentalComposeApi
fun Composition(
    key: Any,
    applier: Applier<*>,
    parent: CompositionContext,
    onCreated: () -> Unit = {}
): Composition = Compositions.findOrCreate(key) {
    CompositionImpl(
        parent,
        applier,
        onDispose = { Compositions.onDisposed(key) }
    ).also {
        onCreated()
    }
}

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
@ExperimentalComposeApi
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
 * @param parent An optional reference to the parent composition.
 * @param applier The applier to use to manage the tree built by the composer.
 * @param onDispose A callback to be triggered when [dispose] is called.
 */
private class CompositionImpl(
    private val parent: CompositionContext,
    applier: Applier<*>,
    private val onDispose: (() -> Unit)? = null
) : ControlledComposition {
    private val composer: ComposerImpl = ComposerImpl(applier, parent, this).also {
        parent.registerComposer(it)
    }

    /**
     * Return true if this is a root (non-sub-) composition.
     */
    val isRoot: Boolean = parent is Recomposer

    private var disposed = false

    var composable: @Composable () -> Unit = {}

    override val isComposing: Boolean
        get() = composer.isComposing

    override val isDisposed: Boolean = disposed

    override val hasPendingChanges: Boolean
        get() = composer.hasPendingChanges

    override fun setContent(content: @Composable () -> Unit) {
        check(!disposed) { "The composition is disposed" }
        this.composable = content
        parent.composeInitial(this, composable)
    }

    override fun composeContent(content: @Composable () -> Unit) {
        composer.composeContent(content)
    }

    @OptIn(ExperimentalComposeApi::class)
    override fun dispose() {
        if (!disposed) {
            disposed = true
            composable = {}
            composer.dispose()
            parent.unregisterComposition(this)
            onDispose?.invoke()
        }
    }

    override val hasInvalidations get() = composer.hasInvalidations

    override fun recordModificationsOf(values: Set<Any>) {
        composer.recordModificationsOf(values)
    }

    override fun recordReadOf(value: Any) {
        composer.recordReadOf(value)
    }

    override fun recordWriteOf(value: Any) {
        composer.recordWriteOf(value)
    }

    override fun recompose(): Boolean = composer.recompose()

    override fun applyChanges() {
        composer.applyChanges()
    }

    override fun invalidateAll() {
        composer.invalidateAll()
    }

    override fun verifyConsistent() {
        composer.verifyConsistent()
    }
}

/**
 * Keeps all the active compositions.
 * This object is thread-safe.
 */
private object Compositions {
    private val holdersMap = WeakHashMap<Any, CompositionImpl>()

    fun findOrCreate(root: Any, create: () -> CompositionImpl): CompositionImpl =
        synchronized(holdersMap) {
            holdersMap[root] ?: create().also { holdersMap[root] = it }
        }

    fun onDisposed(root: Any) {
        synchronized(holdersMap) {
            holdersMap.remove(root)
        }
    }

    fun clear() {
        synchronized(holdersMap) {
            holdersMap.clear()
        }
    }

    fun collectAll(): List<CompositionImpl> = synchronized(holdersMap) {
        holdersMap.values.toList()
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
        private var state = mutableListOf<Pair<CompositionImpl, @Composable () -> Unit>>()

        @TestOnly
        fun clearRoots() {
            Compositions.clear()
        }

        // Called before Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun saveStateAndDispose(context: Any) {
            state.clear()
            val holders = Compositions.collectAll()
            holders.mapTo(state) { it to it.composable }
            holders.filter { it.isRoot }.forEach { it.setContent({}) }
        }

        // Called after Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun loadStateAndCompose(context: Any) {
            val roots = mutableListOf<CompositionImpl>()
            state.forEach { (composition, composable) ->
                composition.composable = composable
                if (composition.isRoot) {
                    roots.add(composition)
                }
            }
            roots.forEach { it.setContent(it.composable) }
            state.clear()
        }

        @TestOnly
        internal fun simulateHotReload(context: Any) {
            saveStateAndDispose(context)
            loadStateAndCompose(context)
        }
    }
}

/**
 * @suppress
 */
@TestOnly
fun simulateHotReload(context: Any) = HotReloader.simulateHotReload(context)

/**
 * @suppress
 */
@TestOnly
fun clearRoots() = HotReloader.clearRoots()
