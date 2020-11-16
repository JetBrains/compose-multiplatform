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
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewTreeLifecycleOwner

/**
 * Base class for custom [android.view.View]s implemented using Jetpack Compose UI.
 * Subclasses should implement the [Content] function with the appropriate content.
 * Calls to [addView] and its variants and overloads will fail with [IllegalStateException].
 *
 * This [android.view.View] requires that the window it is attached to contains a
 * [ViewTreeLifecycleOwner]. This [androidx.lifecycle.LifecycleOwner] is used to
 * [dispose][androidx.compose.Composition.dispose] of the underlying composition
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
    }

    private var composition: Composition? = null

    /**
     * The Jetpack Compose UI content for this view.
     * Subclasses must implement this method to provide content. Initial composition will
     * occur when the view becomes attached to a window or when [createComposition] is called,
     * whichever comes first.
     */
    @Composable
    abstract fun Content()

    private object DisposedComposition : Composition {
        override fun setContent(content: () -> Unit) {
            // No-op
        }

        override fun dispose() {
            // No-op
        }

        override fun hasInvalidations() = false
    }

    /**
     * Perform initial composition for this view.
     * Once this method is called or the view becomes attached to a window,
     * either [disposeComposition] must be called or the [ViewTreeLifecycleOwner] must
     * reach the [Lifecycle.State.DESTROYED] state for the composition to be cleaned up
     * properly. (This restriction is temporary.)
     *
     * If this method is called when the composition has already been created it has no effect.
     * If it is called after the composition is [disposed][disposeComposition] it will throw
     * [IllegalStateException].
     */
    fun createComposition() {
        check(composition !== DisposedComposition) {
            "Cannot create composition - composition was already disposed"
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

    private fun ensureCompositionCreated() {
        if (composition == null) {
            // TODO: Cannot use try/catch here until b/161894067 is fixed.
            creatingComposition = true
            composition = setContent(Recomposer.current()) {
                Content()
            }
            creatingComposition = false
        }
    }

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

    /**
     * Dispose of the underlying composition.
     * The result of this call is permanent; once disposed a ComposeView cannot be used again
     * and will remain empty.
     */
    fun disposeComposition() {
        composition?.dispose()
        composition = DisposedComposition
    }

    /**
     * `true` if [disposeComposition] has been called, either explicitly or by the host window's
     * [ViewTreeLifecycleOwner] being destroyed.
     */
    val isDisposed: Boolean get() = composition === DisposedComposition

    private var lastLifecycle: Lifecycle? = null

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            disposeComposition()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val newLifecycleOwner = checkNotNull(ViewTreeLifecycleOwner.get(this)) {
            "ViewTreeLifecycleOwner is not present in this window. Use ComponentActivity, " +
                "FragmentActivity or AppCompatActivity to configure ViewTreeLifecycleOwner " +
                "automatically, or call ViewTreeLifecycleOwner.set() for this View or an " +
                "ancestor in the same window."
        }
        val newLifecycle = newLifecycleOwner.lifecycle
        if (newLifecycle !== lastLifecycle) {
            lastLifecycle?.removeObserver(lifecycleObserver)
            lastLifecycle = newLifecycle
            newLifecycle.addObserver(lifecycleObserver)
        }
        ensureCompositionCreated()
    }

    final override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val child = checkNotNull(getChildAt(0)) { "Composition view not present for measure!" }
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
        val child = checkNotNull(getChildAt(0)) { "Composition view not present for layout!" }
        child.layout(
            paddingLeft,
            paddingTop,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
    }
}

/**
 * A [android.view.View] that can host Jetpack Compose UI content.
 * Use [setContent] to supply the content composable function for the view.
 *
 * This [android.view.View] requires that the window it is attached to contains a
 * [ViewTreeLifecycleOwner]. This [androidx.lifecycle.LifecycleOwner] is used to
 * [dispose][androidx.compose.Composition.dispose] of the underlying composition
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

    // Note: the call to emptyContent() below instead of a literal {} works around
    // https://youtrack.jetbrains.com/issue/KT-17467, which causes the compiler to emit classes
    // named `content` and `Content` (from the Content method's composable update scope)
    // which causes compilation problems on case-insensitive filesystems.
    @Suppress("RemoveExplicitTypeArguments")
    private val content = mutableStateOf<@Composable () -> Unit>(emptyContent())

    @Composable
    override fun Content() {
        content.value()
    }

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * [createComposition] is called, whichever comes first.
     */
    fun setContent(content: @Composable () -> Unit) {
        this.content.value = content
    }
}
