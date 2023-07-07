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
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.R
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * The container we will use for [ViewLayer]s.
 */
internal class ViewLayerContainer(context: Context) : DrawChildContainer(context) {
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // we draw our children as part of AndroidComposeView.dispatchDraw
    }

    /**
     * We control our own child Views and we don't want the View system to force updating
     * the display lists.
     * We override hidden protected method from ViewGroup
     */
    protected fun dispatchGetDisplayList() {
    }
}

/**
 * The container we will use for [ViewLayer]s when [ViewLayer.shouldUseDispatchDraw] is true.
 */
internal open class DrawChildContainer(context: Context) : ViewGroup(context) {
    private var isDrawing = false

    init {
        clipChildren = false

        // Hide this view and its children in tools:
        setTag(R.id.hide_in_inspector_tag, true)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // we don't layout our children
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // we don't measure our children
        setMeasuredDimension(0, 0)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // We must updateDisplayListIfDirty for all invalidated Views.

        // We only want to call super.dispatchDraw() if there is an invalidated layer
        var doDispatch = false
        for (i in 0 until super.getChildCount()) {
            val child = getChildAt(i) as ViewLayer
            if (child.isInvalidated) {
                doDispatch = true
                break
            }
        }

        if (doDispatch) {
            isDrawing = true
            try {
                super.dispatchDraw(canvas)
            } finally {
                isDrawing = false
            }
        }
    }

    /**
     * We don't want to advertise children to the transition system. ViewLayers shouldn't be
     * watched for add/remove for transitions purposes.
     */
    override fun getChildCount(): Int = if (isDrawing) super.getChildCount() else 0

    // we change visibility for this method so ViewLayer can use it for drawing
    internal fun drawChild(canvas: Canvas, view: View, drawingTime: Long) {
        super.drawChild(canvas.nativeCanvas, view, drawingTime)
    }
}
