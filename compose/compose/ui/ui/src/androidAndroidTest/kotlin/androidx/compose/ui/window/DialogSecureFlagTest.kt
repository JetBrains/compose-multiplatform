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

package androidx.compose.ui.window

import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class DialogSecureFlagTest(private val setSecureFlagOnActivity: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "secureActivity={0}")
        fun initParameters() = arrayOf(false, true)
    }

    @get:Rule
    val rule = createAndroidComposeRule(
        if (setSecureFlagOnActivity) {
            ActivityWithFlagSecure::class.java
        } else {
            ComponentActivity::class.java
        }
    )

    @Test
    fun noFlagSetOnDialog() {
        rule.setContent {
            TestDialog(DialogProperties())
        }

        if (setSecureFlagOnActivity) {
            // Flag was inherited from the Activity
            assertThat(isSecureFlagEnabledForDialog()).isTrue()
        } else {
            // No flag set
            assertThat(isSecureFlagEnabledForDialog()).isFalse()
        }
    }

    @Test
    fun forcedFlagOnDialogToDisabled() {
        rule.setContent {
            TestDialog(DialogProperties(securePolicy = SecureFlagPolicy.SecureOff))
        }

        // This tests that we also override the flag from the Activity
        assertThat(isSecureFlagEnabledForDialog()).isFalse()
    }

    @Test
    fun forcedFlagOnDialogToEnabled() {
        rule.setContent {
            TestDialog(DialogProperties(securePolicy = SecureFlagPolicy.SecureOn))
        }

        assertThat(isSecureFlagEnabledForDialog()).isTrue()
    }

    @Test
    fun toggleFlagOnDialog() {
        var properties: DialogProperties
        by mutableStateOf(DialogProperties(securePolicy = SecureFlagPolicy.SecureOff))

        rule.setContent {
            TestDialog(properties)
        }

        assertThat(isSecureFlagEnabledForDialog()).isFalse()

        // Toggle flag
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)
        assertThat(isSecureFlagEnabledForDialog()).isTrue()

        // Set to inherit
        properties = DialogProperties(securePolicy = SecureFlagPolicy.Inherit)
        assertThat(isSecureFlagEnabledForDialog()).isEqualTo(setSecureFlagOnActivity)
    }

    @Composable
    fun TestDialog(dialogProperties: DialogProperties) {
        SimpleContainer {
            Dialog(
                onDismissRequest = { },
                properties = dialogProperties
            ) {
                SimpleContainer(Modifier.size(50.dp), content = {})
            }
        }
    }

    private fun isSecureFlagEnabledForDialog(): Boolean {
        val owner = rule
            .onNode(isDialog())
            .fetchSemanticsNode("").root as View
        return (owner.rootView.layoutParams as WindowManager.LayoutParams).flags and
            WindowManager.LayoutParams.FLAG_SECURE != 0
    }
}