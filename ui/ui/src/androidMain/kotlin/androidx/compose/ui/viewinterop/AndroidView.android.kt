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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.materialize
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Ref
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection

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
 * [AndroidView] will clip its content to the layout bounds, as being clipped is a common
 * assumption made by [View]s - keeping clipping disabled might lead to unexpected drawing behavior.
 * Note this deviates from Compose's practice of keeping clipping opt-in, disabled by default.
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
    val context = LocalContext.current
    // NoOp Connection required by nested scroll modifier. This is noOp because we don't want
    // to influence nested scrolling with it and it is required by the modifier
    val noOpConnection = remember { object : NestedScrollConnection {} }
    // NestedScrollDispatcher that will be passed/used for nested scroll interop
    val dispatcher = remember { NestedScrollDispatcher() }
    val nestedScrollInteropModifier = Modifier.nestedScroll(noOpConnection, dispatcher)
    // Create a semantics node for accessibility. Semantics modifier is composed and need to be
    // materialized. So it can't be added in AndroidViewHolder when assigning modifier to layout
    // node, which is after the materialize call.
    val modifierWithSemantics = modifier.then(nestedScrollInteropModifier).semantics(true) {}
    val materialized = currentComposer.materialize(modifierWithSemantics)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentReference = rememberCompositionContext()
    val stateRegistry = LocalSaveableStateRegistry.current
    val stateKey = currentCompositeKeyHash.toString()
    val viewFactoryHolderRef = remember { Ref<ViewFactoryHolder<T>>() }

    // These locals are initialized from the view tree at the AndroidComposeView hosting this
    // composition, but they need to be passed to this Android View so that the ViewTree*Owner
    // functions return the correct owners if different local values were provided by the
    // composition, e.g. by a navigation library.
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    ComposeNode<LayoutNode, UiApplier>(
        factory = {
            val viewFactoryHolder = ViewFactoryHolder<T>(context, parentReference, dispatcher)
            viewFactoryHolder.factory = factory
            @Suppress("UNCHECKED_CAST")
            val savedState = stateRegistry?.consumeRestored(stateKey) as? SparseArray<Parcelable>
            if (savedState != null) viewFactoryHolder.typedView?.restoreHierarchyState(savedState)
            viewFactoryHolderRef.value = viewFactoryHolder
            viewFactoryHolder.layoutNode
        },
        update = {
            set(materialized) { viewFactoryHolderRef.value!!.modifier = it }
            set(density) { viewFactoryHolderRef.value!!.density = it }
            set(lifecycleOwner) { viewFactoryHolderRef.value!!.lifecycleOwner = it }
            set(savedStateRegistryOwner) {
                viewFactoryHolderRef.value!!.savedStateRegistryOwner = it
            }
            set(update) { viewFactoryHolderRef.value!!.updateBlock = it }
            set(layoutDirection) {
                viewFactoryHolderRef.value!!.layoutDirection = when (it) {
                    LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
                    LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
                }
            }
        }
    )

    if (stateRegistry != null) {
        DisposableEffect(stateRegistry, stateKey) {
            val valueProvider = {
                val hierarchyState = SparseArray<Parcelable>()
                viewFactoryHolderRef.value!!.typedView?.saveHierarchyState(hierarchyState)
                hierarchyState
            }
            val entry = stateRegistry.registerProvider(stateKey, valueProvider)
            onDispose {
                entry.unregister()
            }
        }
    }
}

/**
 * An empty update block used by [AndroidView].
 */
val NoOpUpdate: View.() -> Unit = {}

internal class ViewFactoryHolder<T : View>(
    context: Context,
    parentContext: CompositionContext? = null,
    dispatcher: NestedScrollDispatcher = NestedScrollDispatcher()
) : AndroidViewHolder(context, parentContext, dispatcher), ViewRootForInspector {

    internal var typedView: T? = null

    override val viewRoot: View get() = this

    var factory: ((Context) -> T)? = null
        set(value) {
            field = value
            if (value != null) {
                typedView = value(context)
                view = typedView
            }
        }

    var updateBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            update = { typedView?.apply(updateBlock) }
        }
}
