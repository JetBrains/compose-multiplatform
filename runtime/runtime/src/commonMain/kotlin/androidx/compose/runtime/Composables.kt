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

package androidx.compose.runtime

/**
 * Remember the value produced by [calculation]. [calculation] will only be evaluated during the composition.
 * Recomposition will always return the value produced by composition.
 */
@Composable
inline fun <T> remember(calculation: @DisallowComposableCalls () -> T): T =
    currentComposer.cache(false, calculation)

/**
 * Remember the value returned by [calculation] if [key1] is equal to the previous composition,
 * otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T> remember(
    key1: Any?,
    calculation: @DisallowComposableCalls () -> T
): T {
    return currentComposer.cache(currentComposer.changed(key1), calculation)
}

/**
 * Remember the value returned by [calculation] if [key1] and [key2] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T> remember(
    key1: Any?,
    key2: Any?,
    calculation: @DisallowComposableCalls () -> T
): T {
    return currentComposer.cache(
        currentComposer.changed(key1) or currentComposer.changed(key2),
        calculation
    )
}

/**
 * Remember the value returned by [calculation] if [key1], [key2] and [key3] are equal to the
 * previous composition, otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T> remember(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    calculation: @DisallowComposableCalls () -> T
): T {
    return currentComposer.cache(
        currentComposer.changed(key1) or
            currentComposer.changed(key2) or
            currentComposer.changed(key3),
        calculation
    )
}

/**
 * Remember the value returned by [calculation] if all values of [keys] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T> remember(
    vararg keys: Any?,
    calculation: @DisallowComposableCalls () -> T
): T {
    var invalid = false
    for (key in keys) invalid = invalid or currentComposer.changed(key)
    return currentComposer.cache(invalid, calculation)
}

/**
 * [key] is a utility composable that is used to "group" or "key" a block of execution inside of a
 * composition. This is sometimes needed for correctness inside of control-flow that may cause a
 * given composable invocation to execute more than once during composition.
 *
 * The value for a key *does not need to be globally unique*, and needs only be unique amongst the
 * invocations of [key] *at that point* in composition.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.runtime.samples.LocallyUniqueKeys
 *
 * Even though there are users with the same id composed in both the top and the bottom loop,
 * because they are different calls to [key], there is no need to create compound keys.
 *
 * The key must be unique for each element in the collection, however, or children and local state
 * might be reused in unintended ways.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.runtime.samples.NotAlwaysUniqueKeys
 *
 * This example assumes that `parent.id` is a unique key for each item in the collection,
 * but this is only true if it is fair to assume that a parent will only ever have a single child,
 * which may not be the case.  Instead, it may be more correct to do the following:
 *
 * @sample androidx.compose.runtime.samples.MoreCorrectUniqueKeys
 *
 * A compound key can be created by passing in multiple arguments:
 *
 * @sample androidx.compose.runtime.samples.TwoInputsKeySample
 *
 * @param keys The set of values to be used to create a compound key. These will be compared to
 * their previous values using [equals] and [hashCode]
 * @param block The composable children for this group.
 */
@Composable
inline fun <T> key(
    @Suppress("UNUSED_PARAMETER")
    vararg keys: Any?,
    block: @Composable () -> T
) = block()

/**
 * A utility function to mark a composition as supporting recycling. If the [key] changes the
 * composition is replaced by a new composition (as would happen for [key]) but reusable nodes
 * that are emitted by [ReusableComposeNode] are reused.
 *
 * @param key the value that is used to trigger recycling. If recomposed with a different value
 * the composer creates a new composition but tries to reuse reusable nodes.
 * @param content the composable children that are recyclable.
 */
@Composable
inline fun ReusableContent(
    key: Any?,
    content: @Composable () -> Unit
) {
    currentComposer.startReusableGroup(reuseKey, key)
    content()
    currentComposer.endReusableGroup()
}

/**
 * An optional utility function used when hosting [ReusableContent]. If [active] is false the
 * content is treated as if it is deleted by removing all remembered objects from the composition
 * but the node produced for the tree are not removed. When the composition later becomes active
 * then the nodes are able to be reused inside [ReusableContent] content without requiring the
 * remembered state of the composition's lifetime being arbitrarily extended.
 *
 * @param active when [active] is `true` [content] is composed normally. When [active] is `false`
 * then the content is deactivated and all remembered state is treated as if the content was
 * deleted but the nodes managed by the composition's [Applier] are unaffected. A [active] becomes
 * `true` any reusable nodes from the previously active composition are candidates for reuse.
 * @param content the composable content that is managed by this composable.
 */
@Composable
@ExplicitGroupsComposable
inline fun ReusableContentHost(
    active: Boolean,
    crossinline content: @Composable () -> Unit
) {
    currentComposer.startReusableGroup(reuseKey, active)
    val activeChanged = currentComposer.changed(active)
    if (active) {
        content()
    } else {
        currentComposer.deactivateToEndGroup(activeChanged)
    }
    currentComposer.endReusableGroup()
}

/**
 * TODO(lmr): provide documentation
 */
val currentComposer: Composer
    @ReadOnlyComposable
    @Composable get() { throw NotImplementedError("Implemented as an intrinsic") }

/**
 * Returns an object which can be used to invalidate the current scope at this point in composition.
 * This object can be used to manually cause recompositions.
 */
val currentRecomposeScope: RecomposeScope
    @ReadOnlyComposable
    @OptIn(InternalComposeApi::class)
    @Composable get() {
        val scope = currentComposer.recomposeScope ?: error("no recompose scope found")
        currentComposer.recordUsed(scope)
        return scope
    }

/**
 * Returns the current [CompositionLocalContext] which contains all
 * [CompositionLocal]'s in the current composition and their values
 * provided by [CompositionLocalProvider]'s.
 * This context can be used to pass locals to another composition via [CompositionLocalProvider].
 * That is usually needed if another composition is not a subcomposition of the current one.
 */
@OptIn(InternalComposeApi::class)
val currentCompositionLocalContext: CompositionLocalContext
    @Composable get() = CompositionLocalContext(
        currentComposer.buildContext().getCompositionLocalScope()
    )

/**
 * This a hash value used to coordinate map externally stored state to the composition. For
 * example, this is used by saved instance state to preserve state across activity lifetime
 * boundaries.
 *
 * This value is likely to be unique but is not guaranteed unique. There are known cases,
 * such as for loops without a [key], where the runtime does not have enough information to
 * make the compound key hash unique.
 */
val currentCompositeKeyHash: Int
    @Composable
    @ExplicitGroupsComposable
    @OptIn(InternalComposeApi::class)
    get() = currentComposer.compoundKeyHash

/**
 * Emits a node into the composition of type [T].
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 *
 * @see Updater
 * @see Applier
 * @see Composition
 */
// ComposeNode is a special case of readonly composable and handles creating its own groups, so
// it is okay to use.
@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE", "UnnecessaryLambdaCreation")
@Composable inline fun <T : Any, reified E : Applier<*>> ComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode { factory() }
    } else {
        currentComposer.useNode()
    }
    Updater<T>(currentComposer).update()
    currentComposer.endNode()
}

/**
 * Emits a recyclable node into the composition of type [T].
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 *
 * @see Updater
 * @see Applier
 * @see Composition
 */
// ComposeNode is a special case of readonly composable and handles creating its own groups, so
// it is okay to use.
@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE", "UnnecessaryLambdaCreation")
@Composable inline fun <T : Any, reified E : Applier<*>> ReusableComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startReusableNode()
    if (currentComposer.inserting) {
        currentComposer.createNode { factory() }
    } else {
        currentComposer.useNode()
    }
    currentComposer.disableReusing()
    Updater<T>(currentComposer).update()
    currentComposer.enableReusing()
    currentComposer.endNode()
}

/**
 * Emits a node into the composition of type [T]. Nodes emitted inside of [content] will become
 * children of the emitted node.
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 * @param content the composable content that will emit the "children" of this node.
 *
 * @see Updater
 * @see Applier
 * @see Composition
 */
// ComposeNode is a special case of readonly composable and handles creating its own groups, so
// it is okay to use.
@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
@Composable
inline fun <T : Any?, reified E : Applier<*>> ComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }
    Updater<T>(currentComposer).update()
    content()
    currentComposer.endNode()
}

/**
 * Emits a recyclable node into the composition of type [T]. Nodes emitted inside of [content] will
 * become children of the emitted node.
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 * @param content the composable content that will emit the "children" of this node.
 *
 * @see Updater
 * @see Applier
 * @see Composition
 */
// ComposeNode is a special case of readonly composable and handles creating its own groups, so
// it is okay to use.
@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
@Composable
inline fun <T : Any?, reified E : Applier<*>> ReusableComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startReusableNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }
    currentComposer.disableReusing()
    Updater<T>(currentComposer).update()
    currentComposer.enableReusing()
    content()
    currentComposer.endNode()
}

/**
 * Emits a node into the composition of type [T]. Nodes emitted inside of [content] will become
 * children of the emitted node.
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 * @param skippableUpdate A function to perform updates on the node. Unlike [update], this
 * function is Composable and will therefore be skipped unless it has been invalidated by some
 * other mechanism. This can be useful to perform expensive calculations for updating the node
 * where the calculations are likely to have the same inputs over time, so the function's
 * execution can be skipped.
 * @param content the composable content that will emit the "children" of this node.
 *
 * @see Updater
 * @see SkippableUpdater
 * @see Applier
 * @see Composition
 */
@Composable @ExplicitGroupsComposable
inline fun <T, reified E : Applier<*>> ComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    noinline skippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }
    Updater<T>(currentComposer).update()
    SkippableUpdater<T>(currentComposer).skippableUpdate()
    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content()
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

/**
 * Emits a recyclable node into the composition of type [T]. Nodes emitted inside of [content] will
 * become children of the emitted node.
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param factory A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 * @param skippableUpdate A function to perform updates on the node. Unlike [update], this
 * function is Composable and will therefore be skipped unless it has been invalidated by some
 * other mechanism. This can be useful to perform expensive calculations for updating the node
 * where the calculations are likely to have the same inputs over time, so the function's
 * execution can be skipped.
 * @param content the composable content that will emit the "children" of this node.
 *
 * @see Updater
 * @see SkippableUpdater
 * @see Applier
 * @see Composition
 */
@Composable @ExplicitGroupsComposable
inline fun <T, reified E : Applier<*>> ReusableComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    noinline skippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startReusableNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }
    currentComposer.disableReusing()
    Updater<T>(currentComposer).update()
    currentComposer.enableReusing()
    SkippableUpdater<T>(currentComposer).skippableUpdate()
    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content()
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

@PublishedApi
internal fun invalidApplier(): Unit = error("Invalid applier")

/**
 * An Effect to construct a [CompositionContext] at the current point of composition. This can be
 * used to run a separate composition in the context of the current one, preserving
 * [CompositionLocal]s and propagating invalidations. When this call leaves the composition, the
 * context is invalidated.
 */
@OptIn(InternalComposeApi::class)
@Composable fun rememberCompositionContext(): CompositionContext {
    return currentComposer.buildContext()
}
