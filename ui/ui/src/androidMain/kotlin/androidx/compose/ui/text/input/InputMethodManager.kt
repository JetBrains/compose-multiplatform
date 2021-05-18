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

import android.content.Context
import android.os.IBinder
import android.view.View
import android.view.inputmethod.ExtractedText

internal interface InputMethodManager {
    fun restartInput(view: View)

    fun showSoftInput(view: View)

    fun hideSoftInputFromWindow(windowToken: IBinder?)

    fun updateExtractedText(
        view: View,
        token: Int,
        extractedText: ExtractedText
    )

    fun updateSelection(
        view: View,
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
internal class InputMethodManagerImpl(context: Context) : InputMethodManager {

    private val imm by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
    }

    override fun restartInput(view: View) {
        imm.restartInput(view)
    }

    override fun showSoftInput(view: View) {
        imm.showSoftInput(view, 0)
    }

    override fun hideSoftInputFromWindow(windowToken: IBinder?) {
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun updateExtractedText(
        view: View,
        token: Int,
        extractedText: ExtractedText
    ) {
        imm.updateExtractedText(view, token, extractedText)
    }

    override fun updateSelection(
        view: View,
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    ) {
        imm.updateSelection(view, selectionStart, selectionEnd, compositionStart, compositionEnd)
    }
}