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
import android.os.IBinder
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.UiComposable
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.Owner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewTreeLifecycleOwner
import java.lang.ref.WeakReference

/**
 * Base class for custom [android.view.View]s implemented using Jetpack Compose UI.
 * Subclasses should implement the [Content] function with the appropriate content.
 * Calls to [addView] and its variants and overloads will fail with [IllegalStateException].
 *
 * By default, the composition is disposed according to [ViewCompositionStrategy.Default].
 * Call [disposeComposition] to dispose of the underlying composition earlier, or if the view is
 * never initially attached to a window. (The requirement to dispose of the composition explicitly
 * in the event that the view is never (re)attached is temporary.)
 */
abstract class AbstractComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    init {
        clipChildren = false
        clipToPadding = false
    }

    /**
     * The first time we successfully locate this we'll save it here.
     * If this View moves to the [android.view.ViewOverlay] we won't be able
     * to find view tree dependencies; this happens when using transition APIs
     * to animate views out in particular.
     *
     * We only ever set this when we're attached to a window.
     */
    private var cachedViewTreeCompositionContext: WeakReference<CompositionContext>? = null

    /**
     * The [getWindowToken] of the window this view was last attached to.
     * If we become attached to a new window we clear [cachedViewTreeCompositionContext]
     * so that we might appeal to the (possibly lazily created) [windowRecomposer]
     * if [findViewTreeCompositionContext] can't locate one instead of using the previous
     * [cachedViewTreeCompositionContext].
     */
    private var previousAttachedWindowToken: IBinder? = null
        set(value) {
            if (field !== value) {
                field = value
                cachedViewTreeCompositionContext = null
            }
        }

    private var composition: Composition? = null

    /**
     * The explicitly set [CompositionContext] to use as the parent of compositions created
     * for this view. Set by [setParentCompositionContext].
     *
     * If set to a non-null value [cachedViewTreeCompositionContext] will be cleared.
     */
    private var parentContext: CompositionContext? = null
        set(value) {
            if (field !== value) {
                field = value
                if (value != null) {
                    cachedViewTreeCompositionContext = null
                }
                val old = composition
                if (old !== null) {
                    old.dispose()
                    composition = null

                    // Recreate the composition now if we are attached.
                    if (isAttachedToWindow) {
                        ensureCompositionCreated()
                    }
                }
            }
        }

    /**
     * Set the [CompositionContext] that should be the parent of this view's composition.
     * If [parent] is `null` it will be determined automatically from the window the view is
     * attached to.
     */
    fun setParentCompositionContext(parent: CompositionContext?) {
        parentContext = parent
    }

    // Leaking `this` during init is generally dangerous, but we know that the implementation of
    // this particular ViewCompositionStrategy is not going to do something harmful with it.
    @Suppress("LeakingThis")
    private var disposeViewCompositionStrategy: (() -> Unit)? =
        ViewCompositionStrategy.Default.installFor(this)

    /**
     * Set the strategy for managing disposal of this View's internal composition.
     * Defaults to [ViewCompositionStrategy.Default].
     *
     * This View's composition is a live resource that must be disposed to ensure that
     * long-lived references to it do not persist
     *
     * See [ViewCompositionStrategy] for more information.
     */
    fun setViewCompositionStrategy(strategy: ViewCompositionStrategy) {
        disposeViewCompositionStrategy?.invoke()
        disposeViewCompositionStrategy = strategy.installFor(this)
    }

    /**
     * If `true`, this View's composition will be created when it becomes attached to a
     * window for the first time. Defaults to `true`.
     *
     * Subclasses may choose to override this property to prevent this eager initial composition
     * in cases where the view's content is not yet ready. Initial composition will still occur
     * when this view is first measured.
     */
    protected open val shouldCreateCompositionOnAttachedToWindow: Boolean
        get() = true

    /**
     * Enables the display of visual layout bounds for the Compose UI content of this view.
     * This is typically managed
     */
    @OptIn(InternalCoreApi::class)
    @InternalComposeUiApi
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    var showLayoutBounds: Boolean = false
        set(value) {
            field = value
            getChildAt(0)?.let {
                (it as Owner).showLayoutBounds = value
            }
        }

    /**
     * The Jetpack Compose UI content for this view.
     * Subclasses must implement this method to provide content. Initial composition will
     * occur when the view becomes attached to a window or when [createComposition] is called,
     * whichever comes first.
     */
    @Composable
    @UiComposable
    abstract fun Content()

    /**
     * Perform initial composition for this view.
     * Once this method is called or the view becomes attached to a window,
     * either [disposeComposition] must be called or the [ViewTreeLifecycleOwner] must
     * reach the [Lifecycle.State.DESTROYED] state for the composition to be cleaned up
     * properly. (This restriction is temporary.)
     *
     * If this method is called when the composition has already been created it has no effect.
     *
     * This method should only be called if this view [isAttachedToWindow] or if a parent
     * [CompositionContext] has been [set][setParentCompositionContext] explicitly.
     */
    fun createComposition() {
        check(parentContext != null || isAttachedToWindow) {
            "createComposition requires either a parent reference or the View to be attached" +
                "to a window. Attach the View or call setParentCompositionReference."
        }
        ensureCompositionCreated()
    }

    private var creatingComposition = false
    private fun checkAddView() {
        if (!creatingComposition) {
            throw UnsupportedOperationException(
                "Cannot add views to " +
                    "${javaClass.simpleName}; only Compose content is supported"
            )
        }
    }

    /**
     * `true` if the [CompositionContext] can be considered to be "alive" for the purposes
     * of locally caching it in case the view is placed into a ViewOverlay.
     * [Recomposer]s that are in the [Recomposer.State.ShuttingDown] state or lower should
     * not be cached or reusedif currently cached, as they will never recompose content.
     */
    private val CompositionContext.isAlive: Boolean
        get() = this !is Recomposer || currentState.value > Recomposer.State.ShuttingDown

    /**
     * Cache this [CompositionContext] in [cachedViewTreeCompositionContext] if it [isAlive]
     * and return the [CompositionContext] itself either way.
     */
    private fun CompositionContext.cacheIfAlive(): CompositionContext = also { context ->
        context.takeIf { it.isAlive }
            ?.let { cachedViewTreeCompositionContext = WeakReference(it) }
    }

    /**
     * Determine the correct [CompositionContext] to use as the parent of this view's
     * composition. This can result in caching a looked-up [CompositionContext] for use
     * later. See [cachedViewTreeCompositionContext] for more details.
     *
     * If [cachedViewTreeCompositionContext] is available but [findViewTreeCompositionContext]
     * cannot find a parent context, we will use the cached context if present before appealing
     * to the [windowRecomposer], as [windowRecomposer] can lazily create a recomposer.
     * If we're reattached to the same window and [findViewTreeCompositionContext] can't find the
     * context that [windowRecomposer] would install, we might be in the [getOverlay] of some
     * part of the view hierarchy to animate the disappearance of this and other views. We still
     * need to be able to compose/recompose in this state without creating a brand new recomposer
     * to do it, as well as still locate any view tree dependencies.
     */
    private fun resolveParentCompositionContext() = parentContext
        ?: findViewTreeCompositionContext()?.cacheIfAlive()
        ?: cachedViewTreeCompositionContext?.get()?.takeIf { it.isAlive }
        ?: windowRecomposer.cacheIfAlive()

    @Suppress("DEPRECATION") // Still using ViewGroup.setContent for now
    private fun ensureCompositionCreated() {
        if (composition == null) {
            try {
                creatingComposition = true
                composition = setContent(resolveParentCompositionContext()) {
                    Content()
                }
            } finally {
                creatingComposition = false
            }
        }
    }

    /**
     * Dispose of the underlying composition and [requestLayout].
     * A new composition will be created if [createComposition] is called or when needed to
     * lay out this view.
     */
    fun disposeComposition() {
        composition?.dispose()
        composition = null
        requestLayout()
    }

    /**
     * `true` if this View is host to an active Compose UI composition.
     * An active composition may consume resources.
     */
    val hasComposition: Boolean get() = composition != null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        previousAttachedWindowToken = windowToken

        if (shouldCreateCompositionOnAttachedToWindow) {
            ensureCompositionCreated()
        }
    }

    final override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        ensureCompositionCreated()
        internalOnMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @Suppress("WrongCall")
    internal open fun internalOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val width = maxOf(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight)
        val height = maxOf(0, MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom)
        child.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)),
        )
        setMeasuredDimension(
            child.measuredWidth + paddingLeft + paddingRight,
            child.measuredHeight + paddingTop + paddingBottom
        )
    }

    final override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) =
        internalOnLayout(changed, left, top, right, bottom)

    internal open fun internalOnLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        getChildAt(0)?.layout(
            paddingLeft,
            paddingTop,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // Force the single child for our composition to have the same LayoutDirection
        // that we do. We will get onRtlPropertiesChanged eagerly as the value changes,
        // but the composition child view won't until it measures. This can be too late
        // to catch the composition pass for that frame, so propagate it eagerly.
        getChildAt(0)?.layoutDirection = layoutDirection
    }

    // Below: enforce restrictions on adding child views to this ViewGroup

    override fun addView(child: View?) {
        checkAddView()
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        checkAddView()
        super.addView(child, index)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        checkAddView()
        super.addView(child, width, height)
    }

    override fun addView(child: View?, params: LayoutParams?) {
        checkAddView()
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        checkAddView()
        super.addView(child, index, params)
    }

    override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(
        child: View?,
        index: Int,
        params: LayoutParams?,
        preventRequestLayout: Boolean
    ): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    override fun shouldDelayChildPressedState(): Boolean = false
}

/**
 * A [android.view.View] that can host Jetpack Compose UI content.
 * Use [setContent] to supply the content composable function for the view.
 *
 * By default, the composition is disposed according to [ViewCompositionStrategy.Default].
 * Call [disposeComposition] to dispose of the underlying composition earlier, or if the view is
 * never initially attached to a window. (The requirement to dispose of the composition explicitly
 * in the event that the view is never (re)attached is temporary.)
 */
class ComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * [createComposition] is called, whichever comes first.
     */
    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
    }
}
