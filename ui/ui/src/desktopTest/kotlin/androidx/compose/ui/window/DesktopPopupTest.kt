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

import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopPopupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `pass ambients to popup`() {
        val ambient = staticAmbientOf<Int>()

        var actualAmbientValue = 0

        rule.setContent {
            Providers(ambient provides 3) {
                Popup {
                    actualAmbientValue = ambient.current
                }
            }
        }

        Truth.assertThat(actualAmbientValue).isEqualTo(3)
    }

    @Test
    fun `onDispose inside popup`() {
        var isPopupShowing by mutableStateOf(true)
        var isDisposed = false

        rule.setContent {
            if (isPopupShowing) {
                Popup {
                    onDispose {
                        isDisposed = true
                    }
                }
            }
        }

        isPopupShowing = false
        rule.waitForIdle()

        Truth.assertThat(isDisposed).isEqualTo(true)
    }

    @Test
    fun `use density inside popup`() {
        var density by mutableStateOf(Density(2f, 1f))
        var densityInsidePopup = 0f

        rule.setContent {
            Providers(AmbientDensity provides density) {
                Popup {
                    densityInsidePopup = AmbientDensity.current.density
                }
            }
        }

        Truth.assertThat(densityInsidePopup).isEqualTo(2f)

        density = Density(3f, 1f)
        rule.waitForIdle()
        Truth.assertThat(densityInsidePopup).isEqualTo(3f)
    }
}
