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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.Owner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewTreeLifecycleOwner

/**
 * Base class for custom [android.view.View]s implemented using Jetpack Compose UI.
 * Subclasses should implement the [Content] function with the appropriate content.
 * Calls to [addView] and its variants and overloads will fail with [IllegalStateException].
 *
 * This [android.view.View] requires that the window it is attached to contains a
 * [ViewTreeLifecycleOwner]. This [androidx.lifecycle.LifecycleOwner] is used to
 * [dispose][androidx.compose.runtime.Composition.dispose] of the underlying composition
 * when the host [Lifecycle] is destroyed, permitting the view to be attached and
 * detached repeatedly while preserving the composition. Call [disposeComposition]
 * to dispose of the underlying composition earlier, or if the view is never initially
 * attached to a window. (The requirement to dispose of the composition explicitly
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

    private var composition: Composition? = null

    private var parentReference: CompositionReference? = null
        set(value) {
            if (field !== value) {
                field = value
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
     * Set the [CompositionReference] that should be the parent of this view's composition.
     * If [parent] is `null` it will be determined automatically from the window the view is
     * attached to.
     */
    fun setParentCompositionReference(parent: CompositionReference?) {
        parentReference = parent
    }

    // Leaking `this` during init is generally dangerous, but we know that the implementation of
    // this particular ViewCompositionStrategy is not going to do something harmful with it.
    @Suppress("LeakingThis")
    private var disposeViewCompositionStrategy: (() -> Unit)? =
        ViewCompositionStrategy.DisposeOnDetachedFromWindow.installFor(this)

    /**
     * Set the strategy for managing disposal of this View's internal composition.
     * Defaults to [ViewCompositionStrategy.DisposeOnDetachedFromWindow].
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
     * [CompositionReference] has been [set][setParentCompositionReference] explicitly.
     */
    fun createComposition() {
        check(parentReference != null || isAttachedToWindow) {
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

    @Suppress("DEPRECATION") // Still using ViewGroup.setContent for now
    private fun ensureCompositionCreated() {
        if (composition == null) {
            try {
                creatingComposition = true
                composition = setContent(
                    parentReference ?: findViewTreeCompositionReference() ?: windowRecomposer
                ) {
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

        if (shouldCreateCompositionOnAttachedToWindow) {
            ensureCompositionCreated()
        }
    }

    final override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        ensureCompositionCreated()
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

    final override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        getChildAt(0)?.layout(
            paddingLeft,
            paddingTop,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
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
}

/**
 * A [android.view.View] that can host Jetpack Compose UI content.
 * Use [setContent] to supply the content composable function for the view.
 *
 * This [android.view.View] requires that the window it is attached to contains a
 * [ViewTreeLifecycleOwner]. This [androidx.lifecycle.LifecycleOwner] is used to
 * [dispose][androidx.compose.runtime.Composition.dispose] of the underlying composition
 * when the host [Lifecycle] is destroyed, permitting the view to be attached and
 * detached repeatedly while preserving the composition. Call [disposeComposition]
 * to dispose of the underlying composition earlier, or if the view is never initially
 * attached to a window. (The requirement to dispose of the composition explicitly
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
