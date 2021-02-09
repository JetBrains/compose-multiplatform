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

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Owner
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class PopupSecureFlagTest(private val setSecureFlagOnActivity: Boolean) {

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

    private val testTag = "testedPopup"

    @Test
    fun noFlagSetOnPopup() {
        rule.setContent {
            TestPopup(PopupProperties())
        }

        if (setSecureFlagOnActivity) {
            // Flag was inherited from the Activity
            assertThat(isSecureFlagEnabledForPopup()).isTrue()
        } else {
            // No flag set
            assertThat(isSecureFlagEnabledForPopup()).isFalse()
        }
    }

    @Test
    fun forcedFlagOnPopupToDisabled() {
        rule.setContent {
            TestPopup(PopupProperties(securePolicy = SecureFlagPolicy.SecureOff))
        }

        // This tests that we also override the flag from the Activity
        assertThat(isSecureFlagEnabledForPopup()).isFalse()
    }

    @Test
    fun forcedFlagOnPopupToEnabled() {
        rule.setContent {
            TestPopup(PopupProperties(securePolicy = SecureFlagPolicy.SecureOn))
        }

        assertThat(isSecureFlagEnabledForPopup()).isTrue()
    }

    @Test
    fun toggleFlagOnPopup() {
        var properties: PopupProperties
        by mutableStateOf(PopupProperties(securePolicy = SecureFlagPolicy.SecureOff))

        rule.setContent {
            TestPopup(properties)
        }

        assertThat(isSecureFlagEnabledForPopup()).isFalse()

        // Toggle flag
        properties = PopupProperties(securePolicy = SecureFlagPolicy.SecureOn)
        assertThat(isSecureFlagEnabledForPopup()).isTrue()

        // Set to inherit
        properties = PopupProperties(securePolicy = SecureFlagPolicy.Inherit)
        assertThat(isSecureFlagEnabledForPopup()).isEqualTo(setSecureFlagOnActivity)
    }

    @Composable
    fun TestPopup(popupProperties: PopupProperties) {
        SimpleContainer {
            PopupTestTag(testTag) {
                Popup(
                    alignment = Alignment.Center,
                    properties = popupProperties
                ) {
                    SimpleContainer(Modifier.size(50.dp), content = {})
                }
            }
        }
    }

    private fun isSecureFlagEnabledForPopup(): Boolean {
        // Make sure that current measurement/drawing is finished
        rule.runOnIdle { }
        val popupMatcher = PopupLayoutMatcher(testTag)
        Espresso.onView(CoreMatchers.instanceOf(Owner::class.java))
            .inRoot(popupMatcher)
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        return popupMatcher.lastSeenWindowParams!!.flags and
            WindowManager.LayoutParams.FLAG_SECURE != 0
    }
}