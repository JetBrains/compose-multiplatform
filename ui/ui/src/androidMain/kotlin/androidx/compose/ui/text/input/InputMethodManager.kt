/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.ExtractedText
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal interface InputMethodManager {
    fun restartInput()

    fun showSoftInput()

    fun hideSoftInput()

    fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    )

    fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    )
}

/**
 * Wrapper class to prevent depending on getSystemService and final InputMethodManager.
 * Let's us test TextInputServiceAndroid class.
 */
internal class InputMethodManagerImpl(private val view: View) : InputMethodManager {

    private val imm by lazy(LazyThreadSafetyMode.NONE) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
    }

    private val helper = if (Build.VERSION.SDK_INT < 30) {
        ImmHelper21(view)
    } else {
        ImmHelper30(view)
    }

    override fun restartInput() {
        imm.restartInput(view)
    }

    override fun showSoftInput() {
        if (DEBUG && !view.hasWindowFocus()) {
            Log.d(TAG, "InputMethodManagerImpl: requesting soft input on non focused field")
        }

        helper.showSoftInput(imm)
    }

    override fun hideSoftInput() {
        helper.hideSoftInput(imm)
    }

    override fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    ) {
        imm.updateExtractedText(view, token, extractedText)
    }

    override fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    ) {
        imm.updateSelection(view, selectionStart, selectionEnd, compositionStart, compositionEnd)
    }
}

private interface ImmHelper {
    fun showSoftInput(imm: android.view.inputmethod.InputMethodManager)
    fun hideSoftInput(imm: android.view.inputmethod.InputMethodManager)
}

private class ImmHelper21(private val view: View) : ImmHelper {

    @DoNotInline
    override fun showSoftInput(imm: android.view.inputmethod.InputMethodManager) {
        view.post {
            imm.showSoftInput(view, 0)
        }
    }

    @DoNotInline
    override fun hideSoftInput(imm: android.view.inputmethod.InputMethodManager) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

@RequiresApi(30)
private class ImmHelper30(private val view: View) : ImmHelper {

    /**
     * Get a [WindowInsetsControllerCompat] for the view. This returns a new instance every time,
     * since the view may return null or not null at different times depending on window attach
     * state.
     */
    private val insetsControllerCompat
        // This can return null when, for example, the view is not attached to a window.
        get() = view.findWindow()?.let { WindowInsetsControllerCompat(it, view) }

    /**
     * This class falls back to the legacy implementation when the window insets controller isn't
     * available.
     */
    private val immHelper21: ImmHelper21
        get() = _immHelper21 ?: ImmHelper21(view).also { _immHelper21 = it }
    private var _immHelper21: ImmHelper21? = null

    @DoNotInline
    override fun showSoftInput(imm: android.view.inputmethod.InputMethodManager) {
        insetsControllerCompat?.apply {
            show(WindowInsetsCompat.Type.ime())
        } ?: immHelper21.showSoftInput(imm)
    }

    @DoNotInline
    override fun hideSoftInput(imm: android.view.inputmethod.InputMethodManager) {
        insetsControllerCompat?.apply {
            hide(WindowInsetsCompat.Type.ime())
        } ?: immHelper21.hideSoftInput(imm)
    }

    // TODO(b/221889664) Replace with composition local when available.
    private fun View.findWindow(): Window? =
        (parent as? DialogWindowProvider)?.window
            ?: context.findWindow()

    private tailrec fun Context.findWindow(): Window? =
        when (this) {
            is Activity -> window
            is ContextWrapper -> baseContext.findWindow()
            else -> null
        }
}
