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

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ColorsTest {

    @get:Rule
    val rule = createComposeRule()

    /**
     * Test for switching between provided [Colors], ensuring that the existing colors objects
     * are preserved. (b/182635582)
     */
    @Test
    fun switchingBetweenColors() {
        val lightColors = lightColors()
        val darkColors = darkColors()
        val colorState = mutableStateOf(lightColors)
        var currentColors: Colors? = null
        rule.setContent {
            MaterialTheme(colorState.value) {
                Button(onReadColors = { currentColors = it })
            }
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColors())).isTrue()
            assertThat(darkColors.contentEquals(darkColors())).isTrue()
            // Current colors should be light
            assertThat(currentColors!!.contentEquals(lightColors)).isTrue()
            // Change current colors to dark
            colorState.value = darkColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColors())).isTrue()
            assertThat(darkColors.contentEquals(darkColors())).isTrue()
            // Current colors should be dark
            assertThat(currentColors!!.contentEquals(darkColors)).isTrue()
            // Change current colors back to light
            colorState.value = lightColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColors())).isTrue()
            assertThat(darkColors.contentEquals(darkColors())).isTrue()
            // Current colors should be light
            assertThat(currentColors!!.contentEquals(lightColors)).isTrue()
        }
    }

    @Composable
    private fun Button(onReadColors: (Colors) -> Unit) {
        val colors = MaterialTheme.colors
        onReadColors(colors)
    }
}

/**
 * [Colors] is @Stable, so by contract it doesn't have equals implemented. And since it creates a
 * new Colors object to mutate internally, we can't compare references. Instead we compare the
 * properties to make sure that the properties are equal.
 *
 * @return true if all the properties inside [this] are equal to those in [other], false otherwise.
 */
private fun Colors.contentEquals(other: Colors): Boolean {
    if (primary != other.primary) return false
    if (primaryVariant != other.primaryVariant) return false
    if (secondary != other.secondary) return false
    if (secondaryVariant != other.secondaryVariant) return false
    if (background != other.background) return false
    if (surface != other.surface) return false
    if (error != other.error) return false
    if (onPrimary != other.onPrimary) return false
    if (onSecondary != other.onSecondary) return false
    if (onBackground != other.onBackground) return false
    if (onSurface != other.onSurface) return false
    if (onError != other.onError) return false
    if (isLight != other.isLight) return false

    return true
}