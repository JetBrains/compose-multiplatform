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

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewParent
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.PausableMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.R
import androidx.core.os.HandlerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The [CompositionContext] that should be used as a parent for compositions at or below
 * this view in the hierarchy. Set to non-`null` to provide a [CompositionContext]
 * for compositions created by child views, or `null` to fall back to any [CompositionContext]
 * provided by ancestor views.
 *
 * See [findViewTreeCompositionContext].
 */
var View.compositionContext: CompositionContext?
    get() = getTag(R.id.androidx_compose_ui_view_composition_context) as? CompositionContext
    set(value) {
        setTag(R.id.androidx_compose_ui_view_composition_context, value)
    }

/**
 * Returns the parent [CompositionContext] for this point in the view hierarchy, or `null`
 * if none can be found.
 *
 * See [compositionContext] to get or set the parent [CompositionContext] for
 * a specific view.
 */
fun View.findViewTreeCompositionContext(): CompositionContext? {
    var found: CompositionContext? = compositionContext
    if (found != null) return found
    var parent: ViewParent? = parent
    while (found == null && parent is View) {
        found = parent.compositionContext
        parent = parent.getParent()
    }
    return found
}

private val animationScale = mutableMapOf<Context, StateFlow<Float>>()

// Callers of this function should pass an application context. Passing an activity context might
// result in activity leaks.
private fun getAnimationScaleFlowFor(applicationContext: Context): StateFlow<Float> {
    return synchronized(animationScale) {
        animationScale.getOrPut(applicationContext) {
            val resolver = applicationContext.contentResolver
            val animationScaleUri =
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
            val channel = Channel<Unit>(CONFLATED)
            val contentObserver =
                object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        channel.trySend(Unit)
                    }
                }

            // TODO: Switch to callbackFlow when it becomes stable
            flow {
                resolver.registerContentObserver(animationScaleUri, false, contentObserver)
                try {
                    for (value in channel) {
                        val newValue = Settings.Global.getFloat(
                            applicationContext.contentResolver,
                            Settings.Global.ANIMATOR_DURATION_SCALE,
                            1f
                        )
                        emit(newValue)
                    }
                } finally {
                    resolver.unregisterContentObserver(contentObserver)
                }
            }.stateIn(
                MainScope(),
                SharingStarted.WhileSubscribed(),
                Settings.Global.getFloat(
                    applicationContext.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                )
            )
        }
    }
}

/**
 * A factory for creating an Android window-scoped [Recomposer]. See [createRecomposer].
 */
@InternalComposeUiApi
fun interface WindowRecomposerFactory {
    /**
     * Get a [Recomposer] for the window where [windowRootView] is at the root of the window's
     * [View] hierarchy. The factory is responsible for establishing a policy for
     * [shutting down][Recomposer.cancel] the returned [Recomposer]. [windowRootView] will
     * hold a hard reference to the returned [Recomposer] until it [joins][Recomposer.join]
     * after shutting down.
     */
    fun createRecomposer(windowRootView: View): Recomposer

    companion object {
        /**
         * A [WindowRecomposerFactory] that creates **lifecycle-aware** [Recomposer]s.
         *
         * Returned [Recomposer]s will be bound to the [ViewTreeLifecycleOwner] registered
         * at the [root][View.getRootView] of the view hierarchy and run
         * [recomposition][Recomposer.runRecomposeAndApplyChanges] and composition effects on the
         * [AndroidUiDispatcher.CurrentThread] for the window's UI thread. The associated
         * [MonotonicFrameClock] will only produce frames when the [Lifecycle] is at least
         * [Lifecycle.State.STARTED], causing animations and other uses of [MonotonicFrameClock]
         * APIs to suspend until a **visible** frame will be produced.
         */
        @OptIn(ExperimentalComposeUiApi::class)
        val LifecycleAware: WindowRecomposerFactory = WindowRecomposerFactory { rootView ->
            rootView.createLifecycleAwareWindowRecomposer()
        }
    }
}

@InternalComposeUiApi
object WindowRecomposerPolicy {

    private val factory = AtomicReference<WindowRecomposerFactory>(
        WindowRecomposerFactory.LifecycleAware
    )

    // Don't expose the actual AtomicReference as @PublishedApi; we might convert to atomicfu later
    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun getAndSetFactory(
        factory: WindowRecomposerFactory
    ): WindowRecomposerFactory = this.factory.getAndSet(factory)

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun compareAndSetFactory(
        expected: WindowRecomposerFactory,
        factory: WindowRecomposerFactory
    ): Boolean = this.factory.compareAndSet(expected, factory)

    fun setFactory(factory: WindowRecomposerFactory) {
        this.factory.set(factory)
    }

    inline fun <R> withFactory(
        factory: WindowRecomposerFactory,
        block: () -> R
    ): R {
        var cause: Throwable? = null
        val oldFactory = getAndSetFactory(factory)
        return try {
            block()
        } catch (t: Throwable) {
            cause = t
            throw t
        } finally {
            if (!compareAndSetFactory(factory, oldFactory)) {
                val err = IllegalStateException(
                    "WindowRecomposerFactory was set to unexpected value; cannot safely restore " +
                        "old state"
                )
                if (cause == null) throw err
                cause.addSuppressed(err)
                throw cause
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    internal fun createAndInstallWindowRecomposer(rootView: View): Recomposer {
        val newRecomposer = factory.get().createRecomposer(rootView)
        rootView.compositionContext = newRecomposer

        // If the Recomposer shuts down, unregister it so that a future request for a window
        // recomposer will consult the factory for a new one.
        val unsetJob = GlobalScope.launch(
            rootView.handler.asCoroutineDispatcher("windowRecomposer cleanup").immediate
        ) {
            try {
                newRecomposer.join()
            } finally {
                // Unset if the view is detached. (See below for the attach state change listener.)
                // Since this is in a finally in this coroutine, even if this job is cancelled we
                // will resume on the window's UI thread and perform this manipulation there.
                val viewTagRecomposer = rootView.compositionContext
                if (viewTagRecomposer === newRecomposer) {
                    rootView.compositionContext = null
                }
            }
        }

        // If the root view is detached, cancel the await for recomposer shutdown above.
        // This will also unset the tag reference to this recomposer during its cleanup.
        rootView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    // cancel the job to clean up the view tags.
                    // this will happen immediately since unsetJob is on an immediate dispatcher
                    // for this view's UI thread instead of waiting for the recomposer to join.
                    // NOTE: This does NOT cancel the returned recomposer itself, as it may be
                    // a shared-instance recomposer that should remain running/is reused elsewhere.
                    unsetJob.cancel()
                }
            }
        )
        return newRecomposer
    }
}

/**
 * Find the "content child" for this view. The content child is the view that is either
 * a direct child of the view with id [android.R.id.content] (and was therefore set as a
 * content view into an activity or dialog window) or the root view of the window.
 *
 * This is used as opposed to [View.getRootView] as the Android framework can reuse an activity
 * window's decor views across activity recreation events. Since a window recomposer is associated
 * with the lifecycle of the host activity, we want that recomposer to shut down and create a new
 * one for the new activity instance.
 */
private val View.contentChild: View
    get() {
        var self: View = this
        var parent: ViewParent? = self.parent
        while (parent is View) {
            if (parent.id == android.R.id.content) return self
            self = parent
            parent = self.parent
        }
        return self
    }

/**
 * Get or lazily create a [Recomposer] for this view's window. The view must be attached
 * to a window with a [ViewTreeLifecycleOwner] registered at the root to access this property.
 */
@OptIn(InternalComposeUiApi::class)
internal val View.windowRecomposer: Recomposer
    get() {
        check(isAttachedToWindow) {
            "Cannot locate windowRecomposer; View $this is not attached to a window"
        }
        val rootView = contentChild
        return when (val rootParentRef = rootView.compositionContext) {
            null -> WindowRecomposerPolicy.createAndInstallWindowRecomposer(rootView)
            is Recomposer -> rootParentRef
            else -> error("root viewTreeParentCompositionContext is not a Recomposer")
        }
    }

/**
 * Create a [Lifecycle] and [window attachment][View.isAttachedToWindow]-aware [Recomposer] for
 * this [View] with the same behavior as [WindowRecomposerFactory.LifecycleAware].
 *
 * [coroutineContext] will override any [CoroutineContext] elements from the default configuration
 * normally used for this content view. The default [CoroutineContext] contains
 * [AndroidUiDispatcher.CurrentThread]; this function should only be called from the UI thread
 * of this [View] or its intended UI thread if it is currently detached.
 *
 * If [lifecycle] is `null` or not supplied the [ViewTreeLifecycleOwner] will be used;
 * if a non-null [lifecycle] is not provided and a [ViewTreeLifecycleOwner] is not present
 * an [IllegalStateException] will be thrown.
 *
 * The returned [Recomposer] will be [cancelled][Recomposer.cancel] when this [View] is detached
 * from a window or if its determined [Lifecycle] is [destroyed][Lifecycle.Event.ON_DESTROY].
 * Recomposition and associated [frame-based][MonotonicFrameClock] effects may be throttled
 * or paused while the [Lifecycle] is not at least [Lifecycle.State.STARTED].
 */
@ExperimentalComposeUiApi
fun View.createLifecycleAwareWindowRecomposer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    lifecycle: Lifecycle? = null
): Recomposer {
    // Only access AndroidUiDispatcher.CurrentThread if we would use an element from it,
    // otherwise prevent lazy initialization.
    val baseContext = if (coroutineContext[ContinuationInterceptor] == null ||
        coroutineContext[MonotonicFrameClock] == null
    ) {
        AndroidUiDispatcher.CurrentThread + coroutineContext
    } else coroutineContext
    val pausableClock = baseContext[MonotonicFrameClock]?.let {
        PausableMonotonicFrameClock(it).apply { pause() }
    }

    var systemDurationScaleSettingConsumer: MotionDurationScaleImpl? = null
    val motionDurationScale = baseContext[MotionDurationScale] ?: MotionDurationScaleImpl().also {
        systemDurationScaleSettingConsumer = it
    }

    val contextWithClockAndMotionScale =
        baseContext + (pausableClock ?: EmptyCoroutineContext) + motionDurationScale
    val recomposer = Recomposer(contextWithClockAndMotionScale)
    val runRecomposeScope = CoroutineScope(contextWithClockAndMotionScale)
    val viewTreeLifecycle =
        checkNotNull(lifecycle ?: ViewTreeLifecycleOwner.get(this)?.lifecycle) {
            "ViewTreeLifecycleOwner not found from $this"
        }

    // Removing the view holding the ViewTreeRecomposer means we may never be reattached again.
    // Since this factory function is used to create a new recomposer for each invocation and
    // doesn't reuse a single instance like other factories might, shut it down whenever it
    // becomes detached. This can easily happen as part of setting a new content view.
    addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                removeOnAttachStateChangeListener(this)
                recomposer.cancel()
            }
        }
    )
    viewTreeLifecycle.addObserver(
        object : LifecycleEventObserver {
            override fun onStateChanged(
                lifecycleOwner: LifecycleOwner,
                event: Lifecycle.Event
            ) {
                val self = this
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        // Undispatched launch since we've configured this scope
                        // to be on the UI thread
                        runRecomposeScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            var durationScaleJob: Job? = null
                            try {
                                durationScaleJob = systemDurationScaleSettingConsumer?.let {
                                    val durationScaleStateFlow = getAnimationScaleFlowFor(
                                        context.applicationContext
                                    )
                                    it.scaleFactor = durationScaleStateFlow.value
                                    launch {
                                        durationScaleStateFlow.collect { scaleFactor ->
                                            it.scaleFactor = scaleFactor
                                        }
                                    }
                                }
                                recomposer.runRecomposeAndApplyChanges()
                            } finally {
                                durationScaleJob?.cancel()
                                // If runRecomposeAndApplyChanges returns or this coroutine is
                                // cancelled it means we no longer care about this lifecycle.
                                // Clean up the dangling references tied to this observer.
                                lifecycleOwner.lifecycle.removeObserver(self)
                            }
                        }
                    }
                    Lifecycle.Event.ON_START -> pausableClock?.resume()
                    Lifecycle.Event.ON_STOP -> pausableClock?.pause()
                    Lifecycle.Event.ON_DESTROY -> {
                        recomposer.cancel()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        // Nothing
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        // Nothing
                    }
                    Lifecycle.Event.ON_ANY -> {
                        // Nothing
                    }
                }
            }
        }
    )
    return recomposer
}

private class MotionDurationScaleImpl : MotionDurationScale {
    override var scaleFactor by mutableStateOf(1f)
}
