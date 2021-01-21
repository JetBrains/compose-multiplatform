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
package androidx.compose.ui.platform

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionData
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionFor
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.tooling.InspectionTables
import androidx.compose.ui.R
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.UiApplier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.Collections
import java.util.WeakHashMap

private val TAG = "Wrapper"

// TODO(chuckj): This is a temporary work-around until subframes exist so that
// nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
// instead.
@MainThread
@OptIn(ExperimentalComposeApi::class)
internal actual fun subcomposeInto(
    container: LayoutNode,
    parent: CompositionReference,
    composable: @Composable () -> Unit
): Composition = compositionFor(
    container,
    UiApplier(container),
    parent
).apply {
    setContent(composable)
}

/**
 * Composes the given composable into the given activity. The [content] will become the root view
 * of the given activity.
 *
 * [Composition.dispose] is called automatically when the Activity is destroyed.
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContent(
    parent: CompositionReference? = null,
    content: @Composable () -> Unit
) {
    val existingComposeView = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ComposeView

    if (existingComposeView != null) with(existingComposeView) {
        setParentCompositionReference(parent)
        setContent(content)
    } else ComposeView(this).apply {
        // Set content and parent **before** setContentView
        // to have ComposeView create the composition on attach
        setParentCompositionReference(parent)
        setContent(content)
        setContentView(this, DefaultLayoutParams)
    }
}

/**
 * Composes the given composable into the given view.
 *
 * The new composition can be logically "linked" to an existing one, by providing a
 * [parent]. This will ensure that invalidations and ambients will flow through
 * the two compositions as if they were not separate.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 *
 * @param parent The [Recomposer] or parent composition reference.
 * @param content Composable that will be the content of the view.
 */
@Suppress("DEPRECATION")
@Deprecated("Use ComposeView or AbstractComposeView instead.")
fun ViewGroup.setContent(
    parent: CompositionReference,
    content: @Composable () -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? AndroidComposeView
        } else {
            removeAllViews(); null
        } ?: AndroidComposeView(context).also { addView(it.view, DefaultLayoutParams) }
    return doSetContent(composeView, parent, content)
}

@OptIn(InternalComposeApi::class)
private fun doSetContent(
    owner: AndroidComposeView,
    parent: CompositionReference,
    content: @Composable () -> Unit
): Composition {
    if (inspectionWanted(owner)) {
        owner.setTag(
            R.id.inspection_slot_table_set,
            Collections.newSetFromMap(WeakHashMap<CompositionData, Boolean>())
        )
        enableDebugInspectorInfo()
    }
    @OptIn(ExperimentalComposeApi::class)
    val original = compositionFor(owner.root, UiApplier(owner.root), parent)
    val wrapped = owner.view.getTag(R.id.wrapped_composition_tag)
        as? WrappedComposition
        ?: WrappedComposition(owner, original).also {
            owner.view.setTag(R.id.wrapped_composition_tag, it)
        }
    wrapped.setContent(content)
    return wrapped
}

private fun enableDebugInspectorInfo() {
    // Set isDebugInspectorInfoEnabled to true via reflection such that R8 cannot see the
    // assignment. This allows the InspectorInfo lambdas to be stripped from release builds.
    @OptIn(InternalComposeApi::class)
    if (!isDebugInspectorInfoEnabled) {
        try {
            val packageClass = Class.forName("androidx.compose.ui.platform.InspectableValueKt")
            val field = packageClass.getDeclaredField("isDebugInspectorInfoEnabled")
            field.isAccessible = true
            field.setBoolean(null, true)
        } catch (ignored: Exception) {
            Log.w(TAG, "Could not access isDebugInspectorInfoEnabled. Please set explicitly.")
        }
    }
}

private class WrappedComposition(
    val owner: AndroidComposeView,
    val original: Composition
) : Composition, LifecycleEventObserver {

    private var disposed = false
    private var addedToLifecycle: Lifecycle? = null
    private var lastContent: @Composable () -> Unit = emptyContent()

    @OptIn(InternalComposeApi::class)
    override fun setContent(content: @Composable () -> Unit) {
        owner.setOnViewTreeOwnersAvailable {
            if (!disposed) {
                val lifecycle = it.lifecycleOwner.lifecycle
                lastContent = content
                if (addedToLifecycle == null) {
                    addedToLifecycle = lifecycle
                    // this will call ON_CREATE synchronously if we already created
                    lifecycle.addObserver(this)
                } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    original.setContent {

                        @Suppress("UNCHECKED_CAST")
                        val inspectionTable =
                            owner.getTag(R.id.inspection_slot_table_set) as?
                                MutableSet<CompositionData>
                                ?: (owner.parent as? View)?.getTag(R.id.inspection_slot_table_set)
                                    as? MutableSet<CompositionData>
                        if (inspectionTable != null) {
                            @OptIn(InternalComposeApi::class)
                            inspectionTable.add(currentComposer.compositionData)
                            currentComposer.collectParameterInformation()
                        }

                        LaunchedEffect(owner) { owner.keyboardVisibilityEventLoop() }
                        LaunchedEffect(owner) { owner.boundsUpdatesEventLoop() }

                        Providers(InspectionTables provides inspectionTable) {
                            ProvideAndroidAmbients(owner, content)
                        }
                    }
                }
            }
        }
    }

    override fun dispose() {
        if (!disposed) {
            disposed = true
            owner.view.setTag(R.id.wrapped_composition_tag, null)
            addedToLifecycle?.removeObserver(this)
        }
        original.dispose()
    }

    override fun hasInvalidations() = original.hasInvalidations()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            dispose()
        } else if (event == Lifecycle.Event.ON_CREATE) {
            if (!disposed) {
                setContent(lastContent)
            }
        }
    }
}

private val DefaultLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)

/**
 * Determines if inspection is wanted for the Layout Inspector.
 *
 * When DEBUG_VIEW_ATTRIBUTES an/or DEBUG_VIEW_ATTRIBUTES_APPLICATION_PACKAGE is turned on for the
 * current application the Layout Inspector is inspecting. An application cannot directly access
 * these global settings, nor is the static field: View.sDebugViewAttributes available.
 *
 *
 * Instead check if the attributeSourceResourceMap is not empty.
 */
private fun inspectionWanted(owner: AndroidComposeView): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        owner.attributeSourceResourceMap.isNotEmpty()
