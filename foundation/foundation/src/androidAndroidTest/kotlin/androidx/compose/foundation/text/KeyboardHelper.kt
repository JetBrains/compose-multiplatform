/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Helper methods for hiding and showing the keyboard in tests.
 * Call [initialize] from your test rule's content before calling any other methods on this class.
 */
@OptIn(ExperimentalComposeUiApi::class)
class KeyboardHelper(
    private val composeRule: ComposeContentTestRule,
    private val timeout: Long = 15_000L
) {
    /**
     * The [View] hosting the compose rule's content. Must be set before calling any methods on this
     * class.
     */
    private lateinit var view: View
    private val imm by lazy {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    /**
     * Call this at the top of your test composition before using the helper.
     */
    @Composable
    fun initialize() {
        view = LocalView.current
    }

    /**
     * Requests the keyboard to be hidden without waiting for it.
     */
    fun hideKeyboard() {
        composeRule.runOnIdle {
            // Use both techniques to hide it, at least one of them will hopefully work.
            hideKeyboardWithInsets()
            hideKeyboardWithImm()
        }
    }

    /**
     * Blocks until the [timeout] or the keyboard's visibility matches [visible].
     * May be called from the test thread or the main thread.
     */
    fun waitForKeyboardVisibility(visible: Boolean) {
        waitUntil(timeout) {
            isSoftwareKeyboardShown() == visible
        }
    }

    fun hideKeyboardIfShown() {
        if (composeRule.runOnIdle { isSoftwareKeyboardShown() }) {
            hideKeyboard()
            waitForKeyboardVisibility(visible = false)
        }
    }

    fun isSoftwareKeyboardShown(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            isSoftwareKeyboardShownWithInsets()
        } else {
            isSoftwareKeyboardShownWithImm()
        }
    }

    @RequiresApi(23)
    private fun isSoftwareKeyboardShownWithInsets(): Boolean {
        val insets = view.rootWindowInsets ?: return false
        val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
        return insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
    }

    private fun isSoftwareKeyboardShownWithImm(): Boolean {
        // TODO(b/163742556): This is just a proxy for software keyboard visibility. Find a better
        //  way to check if the software keyboard is shown.
        return imm.isAcceptingText
    }

    private fun hideKeyboardWithImm() {
        view.post {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun hideKeyboardWithInsets() {
        view.findWindow()?.let { WindowInsetsControllerCompat(it, view) }
            ?.hide(WindowInsetsCompat.Type.ime())
    }

    private fun waitUntil(timeout: Long, condition: () -> Boolean) {
        if (Build.VERSION.SDK_INT >= 30) {
            view.waitForWindowInsetsUntil(timeout, condition)
        } else {
            composeRule.waitUntil(timeout, condition)
        }
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

    @RequiresApi(30)
    fun View.waitForWindowInsetsUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val latch = CountDownLatch(1)
        rootView.setOnApplyWindowInsetsListener { view, windowInsets ->
            if (condition()) {
                latch.countDown()
            }
            view.onApplyWindowInsets(windowInsets)
            windowInsets
        }
        rootView.setWindowInsetsAnimationCallback(
            InsetAnimationCallback {
                if (condition()) {
                    latch.countDown()
                }
            }
        )

        // if condition already met return
        if (condition()) return

        // else wait for condition to be met
        val conditionMet = latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
        assertThat(conditionMet).isTrue()
    }
}

@RequiresApi(30)
private class InsetAnimationCallback(val block: () -> Unit) :
    WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

    override fun onProgress(
        insets: WindowInsets,
        runningAnimations: MutableList<WindowInsetsAnimation>
    ) = insets

    override fun onEnd(animation: WindowInsetsAnimation) {
        block()
        super.onEnd(animation)
    }
}