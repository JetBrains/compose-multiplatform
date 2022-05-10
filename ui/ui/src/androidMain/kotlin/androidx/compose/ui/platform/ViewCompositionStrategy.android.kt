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
import androidx.compose.ui.platform.ViewCompositionStrategy.Companion.Default
import androidx.customview.poolingcontainer.PoolingContainerListener
import androidx.customview.poolingcontainer.addPoolingContainerListener
import androidx.customview.poolingcontainer.isPoolingContainer
import androidx.customview.poolingcontainer.isWithinPoolingContainer
import androidx.customview.poolingcontainer.removePoolingContainerListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner

/**
 * A strategy for managing the underlying Composition of Compose UI [View]s such as
 * [ComposeView] and [AbstractComposeView]. See [AbstractComposeView.setViewCompositionStrategy].
 *
 * Compose views involve ongoing work and registering the composition with external
 * event sources. These registrations can cause the composition to remain live and
 * ineligible for garbage collection for long after the host View may have been abandoned.
 * These resources and registrations can be released manually at any time by calling
 * [AbstractComposeView.disposeComposition] and a new composition will be created automatically
 * when needed. A [ViewCompositionStrategy] defines a strategy for disposing the composition
 * automatically at an appropriate time.
 *
 * By default, Compose UI views are configured to [Default].
 */
interface ViewCompositionStrategy {

    /**
     * Install this strategy for [view] and return a function that will uninstall it later.
     * This function should not be called directly; it is called by
     * [AbstractComposeView.setViewCompositionStrategy] after uninstalling the previous strategy.
     */
    fun installFor(view: AbstractComposeView): () -> Unit

    /**
     * This companion object may be used to define extension factory functions for other
     * strategies to aid in discovery via autocomplete. e.g.:
     * `fun ViewCompositionStrategy.Companion.MyStrategy(): MyStrategy`
     */
    companion object {
        /**
         * The default strategy for [AbstractComposeView] and [ComposeView].
         *
         * Currently, this is [DisposeOnDetachedFromWindowOrReleasedFromPool], though this
         * implementation detail may change.
         */
        // WARNING: the implementation of the default strategy is installed with a reference to
        // `this` on a not-fully-constructed object in AbstractComposeView.
        // Be careful not to do anything that would break that.
        val Default: ViewCompositionStrategy
            get() = DisposeOnDetachedFromWindowOrReleasedFromPool
    }

    /**
     * The composition will be disposed automatically when the view is detached from a window,
     * unless it is part of a [pooling container][isPoolingContainer], such as `RecyclerView`.
     *
     * When not within a pooling container, this behaves exactly the same as
     * [DisposeOnDetachedFromWindow].
     */
    // WARNING: the implementation of the default strategy is installed with a reference to
    // `this` on a not-fully-constructed object in AbstractComposeView.
    // Be careful not to do anything that would break that.
    object DisposeOnDetachedFromWindowOrReleasedFromPool : ViewCompositionStrategy {
        override fun installFor(view: AbstractComposeView): () -> Unit {
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}

                override fun onViewDetachedFromWindow(v: View) {
                    if (!view.isWithinPoolingContainer) {
                        view.disposeComposition()
                    }
                }
            }
            view.addOnAttachStateChangeListener(listener)

            val poolingContainerListener = PoolingContainerListener { view.disposeComposition() }
            view.addPoolingContainerListener(poolingContainerListener)

            return {
                view.removeOnAttachStateChangeListener(listener)
                view.removePoolingContainerListener(poolingContainerListener)
            }
        }
    }

    /**
     * [ViewCompositionStrategy] that disposes the composition whenever the view becomes detached
     * from a window. If the user of a Compose UI view never explicitly calls
     * [AbstractComposeView.createComposition], this strategy is always safe and will always
     * clean up composition resources with no explicit action required - just use the view like
     * any other View and let garbage collection do the rest. (If
     * [AbstractComposeView.createComposition] is called while the view is detached from a window,
     * [AbstractComposeView.disposeComposition] must be called manually if the view is not later
     * attached to a window.)
     *
     * [DisposeOnDetachedFromWindow] is the default strategy for [AbstractComposeView] and
     * [ComposeView].
     */
    object DisposeOnDetachedFromWindow : ViewCompositionStrategy {
        override fun installFor(view: AbstractComposeView): () -> Unit {
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}

                override fun onViewDetachedFromWindow(v: View) {
                    view.disposeComposition()
                }
            }
            view.addOnAttachStateChangeListener(listener)
            return { view.removeOnAttachStateChangeListener(listener) }
        }
    }

    /**
     * [ViewCompositionStrategy] that disposes the composition when [lifecycle] is
     * [destroyed][Lifecycle.Event.ON_DESTROY]. This strategy is appropriate for Compose UI views
     * that share a 1-1 relationship with a known [LifecycleOwner].
     */
    class DisposeOnLifecycleDestroyed(
        private val lifecycle: Lifecycle
    ) : ViewCompositionStrategy {
        constructor(lifecycleOwner: LifecycleOwner) : this(lifecycleOwner.lifecycle)

        override fun installFor(view: AbstractComposeView): () -> Unit =
            installForLifecycle(view, lifecycle)
    }

    /**
     * [ViewCompositionStrategy] that disposes the composition when the [ViewTreeLifecycleOwner]
     * of the next window the view is attached to is [destroyed][Lifecycle.Event.ON_DESTROY].
     * This strategy is appropriate for Compose UI views that share a 1-1 relationship with
     * their closest [ViewTreeLifecycleOwner], such as a Fragment view.
     */
    object DisposeOnViewTreeLifecycleDestroyed : ViewCompositionStrategy {
        override fun installFor(view: AbstractComposeView): () -> Unit {
            if (view.isAttachedToWindow) {
                val lco = checkNotNull(ViewTreeLifecycleOwner.get(view)) {
                    "View tree for $view has no ViewTreeLifecycleOwner"
                }
                return installForLifecycle(view, lco.lifecycle)
            } else {
                // We change this reference after we successfully attach
                var disposer: () -> Unit
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val lco = checkNotNull(ViewTreeLifecycleOwner.get(view)) {
                            "View tree for $view has no ViewTreeLifecycleOwner"
                        }
                        disposer = installForLifecycle(view, lco.lifecycle)

                        // Ensure this runs only once
                        view.removeOnAttachStateChangeListener(this)
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                }
                view.addOnAttachStateChangeListener(listener)
                disposer = { view.removeOnAttachStateChangeListener(listener) }
                return { disposer() }
            }
        }
    }
}

private fun installForLifecycle(view: AbstractComposeView, lifecycle: Lifecycle): () -> Unit {
    check(lifecycle.currentState > Lifecycle.State.DESTROYED) {
        "Cannot configure $view to disposeComposition at Lifecycle ON_DESTROY: $lifecycle" +
            "is already destroyed"
    }
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            view.disposeComposition()
        }
    }
    lifecycle.addObserver(observer)
    return { lifecycle.removeObserver(observer) }
}
