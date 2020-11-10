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

import android.view.View
import android.view.ViewParent
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.PausableMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.dispatch.AndroidUiDispatcher
import androidx.compose.runtime.dispatch.MonotonicFrameClock
import androidx.compose.ui.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewTreeLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The [CompositionReference] that should be used as a parent for compositions at or below
 * this view in the hierarchy. Set to non-`null` to provide a [CompositionReference]
 * for compositions created by child views, or `null` to fall back to any [CompositionReference]
 * provided by ancestor views.
 *
 * See [findViewTreeCompositionReference].
 */
var View.compositionReference: CompositionReference?
    get() = getTag(R.id.androidx_compose_ui_view_composition_reference) as? CompositionReference
    set(value) {
        setTag(R.id.androidx_compose_ui_view_composition_reference, value)
    }

/**
 * Returns the parent [CompositionReference] for this point in the view hierarchy, or `null`
 * if none can be found.
 *
 * See [compositionReference] to get or set the parent [CompositionReference] for
 * a specific view.
 */
fun View.findViewTreeCompositionReference(): CompositionReference? {
    var found: CompositionReference? = compositionReference
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.compositionReference
        parent = parent.getParent()
    }
    return found
}

// Flag for temporarily keeping compatibility with existing testing code that relies on
// Recomposer.current() to perform synchronization
internal var UseRecomposerCurrentAsWindowRecomposer = true

/**
 * Get or lazily create a [Recomposer] for this view's window. The view must be attached
 * to a window with a [ViewTreeLifecycleOwner] registered at the root to access this property.
 */
internal val View.windowRecomposer: Recomposer
    get() {
        if (UseRecomposerCurrentAsWindowRecomposer) {
            return Recomposer.current()
        }
        check(isAttachedToWindow) {
            "Cannot locate windowRecomposer; View $this is not attached to a window"
        }
        val rootView = rootView
        return when (val rootParentRef = rootView.compositionReference) {
            null -> rootView.createViewTreeRecomposer()
            is Recomposer -> rootParentRef
            else -> error("root viewTreeParentCompositionReference is not a Recomposer")
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun View.createViewTreeRecomposer(): Recomposer {
    val currentThreadContext = AndroidUiDispatcher.CurrentThread
    val pausableClock = currentThreadContext[MonotonicFrameClock]?.let {
        PausableMonotonicFrameClock(it).apply { pause() }
    }
    val contextWithClock = currentThreadContext + (pausableClock ?: EmptyCoroutineContext)
    val recomposer = Recomposer(contextWithClock)
    val runRecomposeScope = CoroutineScope(contextWithClock)
    val viewTreeLifecycleOwner = checkNotNull(ViewTreeLifecycleOwner.get(this)) {
        "ViewTreeLifecycleOwner not found from $this"
    }
    setTag(R.id.androidx_compose_ui_view_composition_reference, recomposer)
    viewTreeLifecycleOwner.lifecycle.addObserver(
        LifecycleEventObserver { _, event ->
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (event) {
                Lifecycle.Event.ON_CREATE ->
                    // Undispatched launch since we've configured this scope
                    // to be on the UI thread
                    runRecomposeScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        recomposer.runRecomposeAndApplyChanges()
                    }
                Lifecycle.Event.ON_START -> pausableClock?.resume()
                Lifecycle.Event.ON_STOP -> pausableClock?.pause()
                Lifecycle.Event.ON_DESTROY -> {
                    recomposer.shutDown()
                    setTag(R.id.androidx_compose_ui_view_composition_reference, null)
                }
            }
        }
    )
    return recomposer
}
