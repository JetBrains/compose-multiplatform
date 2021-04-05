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

package androidx.compose.testutils.benchmark.android

import android.R
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.RenderNode
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi

class AndroidTestCaseRunner<T : AndroidTestCase>(
    private val testCaseFactory: () -> T,
    private val activity: Activity
) {

    val measuredWidth: Int
        get() = view!!.measuredWidth
    val measuredHeight: Int
        get() = view!!.measuredHeight

    private var view: ViewGroup? = null

    private val screenWithSpec: Int
    private val screenHeightSpec: Int
    private val capture = if (Build.VERSION.SDK_INT >= 29) RenderNodeCapture() else PictureCapture()
    private var canvas: Canvas? = null

    private var testCase: T? = null

    init {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") /* defaultDisplay + getMetrics() */
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        screenWithSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        screenHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    }

    fun setupContent() {
        require(view == null) { "Content was already set!" }
        view = setupContentInternal(activity)
    }

    private fun setupContentInternal(activity: Activity): ViewGroup {
        testCase = testCaseFactory()
        return testCase!!.getContent(activity).also { activity.setContentView(it) }
    }

    fun measure() {
        getView().measure(screenWithSpec, screenHeightSpec)
    }

    fun measureWithSpec(widthSpec: Int, heightSpec: Int) {
        getView().measure(widthSpec, heightSpec)
    }

    fun drawPrepare() {
        canvas = capture.beginRecording(getView().width, getView().height)
    }

    fun draw() {
        getView().draw(canvas)
    }

    fun drawFinish() {
        capture.endRecording()
    }

    fun requestLayout() {
        getView().requestLayout()
    }

    fun layout() {
        val view = getView()
        view.layout(view.left, view.top, view.right, view.bottom)
    }

    fun doFrame() {
        if (view == null) {
            setupContent()
        }

        measure()
        layout()
        drawPrepare()
        draw()
        drawFinish()
    }

    fun invalidateViews() {
        invalidateViews(getView())
    }

    fun disposeContent() {
        if (view == null) {
            // Already disposed or never created any content
            return
        }

        // Clear the view
        val rootView = activity.findViewById(R.id.content) as ViewGroup
        rootView.removeAllViews()
        // Important so we can set the content again.
        view = null
        testCase = null
    }

    fun getTestCase(): T {
        return testCase!!
    }

    private fun getView(): ViewGroup {
        require(view != null) { "View was not set! Call setupContent first!" }
        return view!!
    }
}

private fun invalidateViews(view: View) {
    view.invalidate()
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            invalidateViews(child)
        }
    }
}

// We must separate the use of RenderNode so that it isn't referenced in any
// way on platforms that don't have it. This extracts RenderNode use to a
// potentially unloaded class, RenderNodeCapture.
private interface DrawCapture {
    fun beginRecording(width: Int, height: Int): Canvas
    fun endRecording()
}

@RequiresApi(Build.VERSION_CODES.Q)
private class RenderNodeCapture : DrawCapture {
    private val renderNode = RenderNode("Test")

    override fun beginRecording(width: Int, height: Int): Canvas {
        renderNode.setPosition(0, 0, width, height)
        return renderNode.beginRecording()
    }

    override fun endRecording() {
        renderNode.endRecording()
    }
}

private class PictureCapture : DrawCapture {
    private val picture = Picture()

    override fun beginRecording(width: Int, height: Int): Canvas {
        return picture.beginRecording(width, height)
    }

    override fun endRecording() {
        picture.endRecording()
    }
}
