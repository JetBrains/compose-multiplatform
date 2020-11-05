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
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density

/**
 * A base class used to host a [View] inside Compose.
 * This API is not designed to be used directly, but rather using the [AndroidView] and
 * `AndroidViewBinding` APIs, which are built on top of [AndroidViewHolder].
 */
// Opt in snapshot observing APIs.
@OptIn(ExperimentalComposeApi::class)
@InternalInteropApi
abstract class AndroidViewHolder(context: Context) : ViewGroup(context) {
    init {
        clipChildren = false
    }

    /**
     * The view hosted by this holder.
     */
    var view: View? = null
        internal set(value) {
            if (value !== field) {
                field = value
                removeAllViews()
                if (value != null) {
                    addView(value)
                    runUpdate()
                }
            }
        }

    /**
     * The update logic of the [View].
     */
    var update: () -> Unit = {}
        protected set(value) {
            field = value
            hasUpdateBlock = true
            runUpdate()
        }
    private var hasUpdateBlock = false

    /**
     * The modifier of the `LayoutNode` corresponding to this [View].
     */
    var modifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke(value)
            }
        }

    internal var onModifierChanged: ((Modifier) -> Unit)? = null

    /**
     * The screen density of the layout.
     */
    var density: Density = Density(1f)
        set(value) {
            if (value !== field) {
                field = value
                onDensityChanged?.invoke(value)
            }
        }

    internal var onDensityChanged: ((Density) -> Unit)? = null

    @OptIn(ExperimentalComposeApi::class)
    private val snapshotObserver = SnapshotStateObserver { command ->
        if (handler.looper === Looper.myLooper()) {
            command()
        } else {
            handler.post(command)
        }
    }

    private val onCommitAffectingUpdate: (AndroidViewHolder) -> Unit = {
        handler.post(runUpdate)
    }

    @OptIn(ExperimentalComposeApi::class)
    private val runUpdate: () -> Unit = {
        if (hasUpdateBlock) {
            snapshotObserver.observeReads(this, onCommitAffectingUpdate) {
                update()
            }
        }
    }

    internal var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        view?.measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(view?.measuredWidth ?: 0, view?.measuredHeight ?: 0)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        view?.layout(0, 0, r - l, b - t)
    }

    override fun getLayoutParams(): LayoutParams? {
        return view?.layoutParams
            ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        snapshotObserver.enableStateUpdatesObserving(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotObserver.enableStateUpdatesObserving(false)
        // remove all observations:
        snapshotObserver.clear()
    }
}

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an experimental API for Compose UI LayoutNode and is likely to change " +
        "before becoming stable."
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
annotation class InternalInteropApi
