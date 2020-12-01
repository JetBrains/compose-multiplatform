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

@file:OptIn(ComposeCompilerApi::class, ExperimentalComposeApi::class)
package androidx.compose.runtime

/**
 * Emits a node into the composition of type [T].
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param ctor A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 *
 * @see Updater
 * @see Applier
 * @see emit
 * @see compositionFor
 */
@Suppress("ComposableNaming")
@OptIn(ComposeCompilerApi::class)
@Composable inline fun <T : Any, reified E : Applier<*>> emit(
    noinline ctor: () -> T,
    update: Updater<T>.() -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    val node = if (currentComposer.inserting)
        ctor().also { currentComposer.emitNode(it) }
    else
        @Suppress("UNCHECKED_CAST")
        currentComposer.useNode() as T
    Updater(currentComposer, node).update()
    currentComposer.endNode()
}

// TODO(lmr): make assert more informative
// TODO(lmr): consider invoking children manually
// TODO(lmr): consider ComposableContract for this
/**
 * Emits a node into the composition of type [T]. Nodes emitted inside of [content] will become
 * children of the emitted node.
 *
 * This function will throw a runtime exception if [E] is not a subtype of the applier of the
 * [currentComposer].
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param ctor A function which will create a new instance of [T]. This function is NOT
 * guaranteed to be called in place.
 * @param update A function to perform updates on the node. This will run every time emit is
 * executed. This function is called in place and will be inlined.
 * @param content the composable content that will emit the "children" of this node.
 *
 * @see Updater
 * @see Applier
 * @see emit
 * @see compositionFor
 */
@Suppress("ComposableNaming")
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T : Any?, reified E : Applier<*>> emit(
    noinline ctor: () -> T,
    update: Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    val node = if (currentComposer.inserting)
        ctor().also { currentComposer.emitNode(it) }
    else
        @Suppress("UNCHECKED_CAST")
        currentComposer.useNode() as T
    Updater(currentComposer, node).update()
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
 * @param ctor A function which will create a new instance of [T]. This function is NOT
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
 * @see emit
 * @see compositionFor
 */
@Suppress("ComposableNaming")
@OptIn(ComposeCompilerApi::class)
@Composable @ComposableContract(readonly = true)
inline fun <T : Any?, reified E : Applier<*>> emit(
    noinline ctor: () -> T,
    update: Updater<T>.() -> Unit,
    noinline skippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    if (currentComposer.applier !is E) invalidApplier()
    currentComposer.startNode()
    val node = if (currentComposer.inserting)
        ctor().also { currentComposer.emitNode(it) }
    else
        @Suppress("UNCHECKED_CAST")
        currentComposer.useNode() as T
    Updater(currentComposer, node).update()
    SkippableUpdater(currentComposer, node).skippableUpdate()
    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content()
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

@PublishedApi
internal fun invalidApplier(): Unit = error("Invalid applier")