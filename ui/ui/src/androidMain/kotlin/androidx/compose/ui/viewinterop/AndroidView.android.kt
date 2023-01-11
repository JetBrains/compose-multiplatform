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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.materialize
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Composes an Android [View] obtained from [factory]. The [factory] block will be called
 * exactly once to obtain the [View] to be composed, and it is also guaranteed to be invoked on
 * the UI thread. Therefore, in addition to creating the [View], the [factory] can also be used
 * to perform one-off initializations and [View] constant properties' setting.
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [View] properties depending on state. When state changes,
 * the block will be reexecuted to set the new properties. Note the block will also be ran once
 * right after the [factory] block completes.
 *
 * [AndroidView] is commonly needed for using Views that are infeasible to be reimplemented in
 * Compose and there is no corresponding Compose API. Common examples for the moment are
 * WebView, SurfaceView, AdView, etc.
 *
 * This version of [AndroidView] does not automatically pool or reuse Views. If placed inside of a
 * reusable container (including inside a [LazyRow][androidx.compose.foundation.lazy.LazyRow] or
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]), the View instances will always be
 * discarded and recreated if the composition hierarchy containing the AndroidView changes, even
 * if its group structure did not change and the View could have conceivably been reused. To
 * opt-in to View reuse, use the `AndroidView(factory, onReset, modifier, update, onRelease)`
 * overload instead.
 *
 * [AndroidView] will not clip its content to the layout bounds. Use [View.setClipToOutline] on
 * the child View to clip the contents, if desired. Developers will likely want to do this with
 * all subclasses of SurfaceView to keep its contents contained.
 *
 * [AndroidView] has nested scroll interop capabilities if the containing view has nested scroll
 * enabled. This means this Composable can dispatch scroll deltas if it is placed inside a
 * container that participates in nested scroll. For more information on how to enable
 * nested scroll interop:
 * @sample androidx.compose.ui.samples.ViewInComposeNestedScrollInteropSample
 *
 * @sample androidx.compose.ui.samples.AndroidViewSample
 *
 * @param factory The block creating the [View] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
@UiComposable
fun <T : View> AndroidView(
    factory: (Context) -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate
) {
    val dispatcher = remember { NestedScrollDispatcher() }
    val materializedModifier = currentComposer.materialize(
        modifier.nestedScroll(NoOpScrollConnection, dispatcher)
    )
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // These locals are initialized from the view tree at the AndroidComposeView hosting this
    // composition, but they need to be passed to this Android View so that the ViewTree*Owner
    // functions return the correct owners if different local values were provided by the
    // composition, e.g. by a navigation library.
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    ComposeNode<LayoutNode, UiApplier>(
        factory = createAndroidViewNodeFactory(factory, dispatcher),
        update = {
            updateViewHolderParams<T>(
                modifier = materializedModifier,
                density = density,
                lifecycleOwner = lifecycleOwner,
                savedStateRegistryOwner = savedStateRegistryOwner,
                layoutDirection = layoutDirection
            )
            set(update) { requireViewFactoryHolder<T>().updateBlock = it }
        }
    )
}

/**
 * Composes an Android [View] obtained from [factory], allowing Compose to reuse Android [View]
 * instances when placed in a container that supports reuse. Reuse occurs when compatible instances
 * of [AndroidView] are inserted and removed during recomposition. Two instances of `AndroidView`
 * are considered compatible if they are invoked with the same composable group structure. The most
 * common scenario where this happens is in lazy layout APIs like `LazyRow` and `LazyColumn`, which
 * can reuse layout nodes (and Views, in this case) between items when scrolling.
 *
 * [AndroidView] will invoke [factory] once to create the View being composed. [factory] is a good
 * place to perform any one-time initialization for your layout after it is inflated. This view will
 * be retained and reused until the composable is discarded.
 *
 * When the View is recomposed, the [update] callback will be invoked. This is an ideal place to
 * set view properties to the target state. The [update] block will be run once after [factory]
 * block completes, and also after the View is reused.
 *
 * When the View is about to be reused, [onReset] will be invoked, signaling that the View should
 * be prepared to appear in a new context in the composition hierarchy. This callback is invoked
 * before [update] and may be used to reset any transient View state like animations or user input.
 *
 * When the View is removed from the composition hierarchy, [onRelease] will be invoked. Once this
 * callback returns, Compose will never attempt to reuse the previous View instance. If one is
 * needed in the future, a new instance of the node will be created, with a fresh lifecycle that
 * begins by calling the factory.
 *
 * In addition to being reset and released, Compose may also temporarily detach the View from the
 * composition hierarchy if it is temporarily deactivated (Namely, if it appears in a
 * [ReusableContentHost] that is not currently active or inside of a
 * [movable content][androidx.compose.runtime.movableContentOf] block that is being moved). If this
 * happens, the View will be removed from its parent, but retained by Compose so that it may be
 * reused if its content host becomes active again. If this does not happen and the View is instead
 * discarded entirely, the [onRelease] callback will be invoked.
 *
 * If you need to observe whether the View is currently used in the composition hierarchy, you may
 * observe whether it is attached via [View.addOnAttachStateChangeListener]. The View may also
 * observe the lifecycle of its host via [findViewTreeLifecycleOwner]. The lifecycle returned by
 * this function will match the [LocalLifecycleOwner]. Note that the lifecycle is not set and cannot
 * be used until the View is attached.
 *
 * All three callback parameters to this function ([update], [onReset], and [onRelease]) are invoked
 * on the UI thread.
 *
 * @sample androidx.compose.ui.samples.ReusableAndroidViewInLazyColumnSample
 *
 * @param factory The block creating the [View] to be composed.
 * @param onReset A callback invoked as a signal that the view is about to be attached to the
 * composition hierarchy in a different context than its original creation. This callback is invoked
 * before [update] and should prepare the view for general reuse.
 * @param modifier The modifier to be applied to the layout.
 * @param update A callback to be invoked after the layout is inflated and upon recomposition to
 * update the information and state of the view.
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 */
@ExperimentalComposeUiApi
@Composable
@UiComposable
fun <T : View> AndroidView(
    factory: (Context) -> T,
    onReset: (T) -> Unit,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
    onRelease: (T) -> Unit = NoOpUpdate
) {
    // TODO: There is a potential edge case with nested scrolling where this dispatcher may become
    //  out of sync in the ViewHolder and in the materialized Modifier. This will happen whenever
    //  the node is reused because remember blocks are reset and therefore a new dispatcher will be
    //  created when reusing an AndroidView composable, but the new dispatcher will not make its
    //  way into the AndroidViewHolder, meaning the composable and view will no longer be connected
    //  to one another after reuse. This can be addressed by moving the nested scrolling behavior
    //  into AndroidViewHolder, which requires refactoring the nestedScroll modifier to
    //  Modifier.Node (in progress in aosp/2404403). This only affects this overload of AndroidView,
    //  and not the stable, non-reusable AndroidView API.
    val dispatcher = remember { NestedScrollDispatcher() }
    val materializedModifier = currentComposer.materialize(
        modifier.nestedScroll(NoOpScrollConnection, dispatcher)
    )

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // These locals are initialized from the view tree at the AndroidComposeView hosting this
    // composition, but they need to be passed to this Android View so that the ViewTree*Owner
    // functions return the correct owners if different local values were provided by the
    // composition, e.g. by a navigation library.
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    ReusableComposeNode<LayoutNode, UiApplier>(
        factory = createAndroidViewNodeFactory(factory, dispatcher),
        update = {
            updateViewHolderParams<T>(
                modifier = materializedModifier,
                density = density,
                lifecycleOwner = lifecycleOwner,
                savedStateRegistryOwner = savedStateRegistryOwner,
                layoutDirection = layoutDirection
            )
            set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
            set(update) { requireViewFactoryHolder<T>().updateBlock = it }
            set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
        }
    )
}

@Composable
private fun <T : View> createAndroidViewNodeFactory(
    factory: (Context) -> T,
    dispatcher: NestedScrollDispatcher
): () -> LayoutNode {
    val context = LocalContext.current
    val parentReference = rememberCompositionContext()
    val stateRegistry = LocalSaveableStateRegistry.current
    val stateKey = currentCompositeKeyHash.toString()

    return {
        ViewFactoryHolder<T>(
            context = context,
            factory = factory,
            parentContext = parentReference,
            dispatcher = dispatcher,
            saveStateRegistry = stateRegistry,
            saveStateKey = stateKey
        ).layoutNode
    }
}

private fun <T : View> Updater<LayoutNode>.updateViewHolderParams(
    modifier: Modifier,
    density: Density,
    lifecycleOwner: LifecycleOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    layoutDirection: LayoutDirection,
) {
    set(modifier) { requireViewFactoryHolder<T>().modifier = it }
    set(density) { requireViewFactoryHolder<T>().density = it }
    set(lifecycleOwner) { requireViewFactoryHolder<T>().lifecycleOwner = it }
    set(savedStateRegistryOwner) {
        requireViewFactoryHolder<T>().savedStateRegistryOwner = it
    }
    set(layoutDirection) {
        requireViewFactoryHolder<T>().layoutDirection = when (it) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : View> LayoutNode.requireViewFactoryHolder(): ViewFactoryHolder<T> {
    return checkNotNull(interopViewFactoryHolder) as ViewFactoryHolder<T>
}

/**
 * An empty update block used by [AndroidView].
 */
val NoOpUpdate: View.() -> Unit = {}

/**
 * No-op Connection required by nested scroll modifier. This is No-op because we don't want
 * to influence nested scrolling with it and it is required by [Modifier.nestedScroll].
 */
private val NoOpScrollConnection = object : NestedScrollConnection {}

internal class ViewFactoryHolder<T : View> private constructor(
    context: Context,
    parentContext: CompositionContext? = null,
    val typedView: T,
    // NestedScrollDispatcher that will be passed/used for nested scroll interop
    val dispatcher: NestedScrollDispatcher,
    private val saveStateRegistry: SaveableStateRegistry?,
    private val saveStateKey: String
) : AndroidViewHolder(context, parentContext, dispatcher), ViewRootForInspector {

    constructor(
        context: Context,
        factory: (Context) -> T,
        parentContext: CompositionContext? = null,
        dispatcher: NestedScrollDispatcher,
        saveStateRegistry: SaveableStateRegistry?,
        saveStateKey: String
    ) : this(
        context = context,
        typedView = factory(context),
        dispatcher = dispatcher,
        parentContext = parentContext,
        saveStateRegistry = saveStateRegistry,
        saveStateKey = saveStateKey,
    )

    override val viewRoot: View get() = this

    private var saveableRegistryEntry: SaveableStateRegistry.Entry? = null
        set(value) {
            field?.unregister()
            field = value
        }

    init {
        clipChildren = false

        view = typedView
        @Suppress("UNCHECKED_CAST")
        val savedState = saveStateRegistry
            ?.consumeRestored(saveStateKey) as? SparseArray<Parcelable>
        savedState?.let { typedView.restoreHierarchyState(it) }
        registerSaveStateProvider()
    }

    var updateBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            update = { typedView.apply(updateBlock) }
        }

    var resetBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            reset = { typedView.apply(resetBlock) }
        }

    var releaseBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            release = {
                typedView.apply(releaseBlock)
                unregisterSaveStateProvider()
            }
        }

    private fun registerSaveStateProvider() {
        if (saveStateRegistry != null) {
            saveableRegistryEntry = saveStateRegistry.registerProvider(saveStateKey) {
                SparseArray<Parcelable>().apply {
                    typedView.saveHierarchyState(this)
                }
            }
        }
    }

    private fun unregisterSaveStateProvider() {
        saveableRegistryEntry = null
    }
}