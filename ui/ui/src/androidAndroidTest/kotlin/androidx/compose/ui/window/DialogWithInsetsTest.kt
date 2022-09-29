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
package androidx.compose.ui.window

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogWithInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ActivityWithInsets>()

    /**
     * Make sure that insets are available in the Dialog.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun dialogSupportsWindowInsets() {
        var dialogSize = IntSize.Zero
        val focusRequester = FocusRequester()
        var imeInsets = Insets.NONE
        lateinit var controller: WindowInsetsControllerCompat
        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(decorFitsSystemWindows = false)
            ) {
                val view = LocalView.current
                SideEffect {
                    val dialogWindowProvider = findDialogWindowProviderInParent(view)
                    if (dialogWindowProvider != null) {
                        controller = WindowInsetsControllerCompat(dialogWindowProvider.window, view)
                    }
                }
                val density = LocalDensity.current
                imeInsets = Insets.of(
                    WindowInsets.ime.getLeft(density, LayoutDirection.Ltr),
                    WindowInsets.ime.getTop(density),
                    WindowInsets.ime.getRight(density, LayoutDirection.Ltr),
                    WindowInsets.ime.getBottom(density),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .imePadding()
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                dialogSize = it
                            }) {
                        TextField(
                            value = "Hello World",
                            onValueChange = { },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        val originalSize = dialogSize

        // show the IME
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.waitForIdle()
        rule.runOnUiThread {
            controller.show(WindowInsetsCompat.Type.ime())
        }

        rule.waitUntil {
            dialogSize != originalSize
        }
        rule.waitForIdle()
        assertNotEquals(Insets.NONE, imeInsets)
    }

    private fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
        if (view is DialogWindowProvider) {
            return view
        }
        val parent = view.parent ?: return null
        if (parent is View) {
            return findDialogWindowProviderInParent(parent)
        }
        return null
    }
}