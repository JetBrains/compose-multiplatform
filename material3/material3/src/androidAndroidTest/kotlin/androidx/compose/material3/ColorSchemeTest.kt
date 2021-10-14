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

package androidx.compose.material3

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
class ColorSchemeTest {

    @get:Rule
    val rule = createComposeRule()

    /**
     * Test for switching between provided [ColorScheme]s, ensuring that the existing colors objects
     * are preserved. (b/182635582)
     */
    @Test
    fun switchingBetweenColors() {
        val lightColors = lightColorScheme()
        val darkColors = darkColorScheme()
        val colorSchemeState = mutableStateOf(lightColors)
        var currentColorScheme: ColorScheme? = null
        rule.setContent {
            MaterialTheme(colorSchemeState.value) {
                Button(onReadColorScheme = { currentColorScheme = it })
            }
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be light
            assertThat(currentColorScheme!!.contentEquals(lightColors)).isTrue()
            // Change current colors to dark
            colorSchemeState.value = darkColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be dark
            assertThat(currentColorScheme!!.contentEquals(darkColors)).isTrue()
            // Change current colors back to light
            colorSchemeState.value = lightColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be light
            assertThat(currentColorScheme!!.contentEquals(lightColors)).isTrue()
        }
    }

    @Composable
    private fun Button(onReadColorScheme: (ColorScheme) -> Unit) {
        val colorScheme = MaterialTheme.colorScheme
        onReadColorScheme(colorScheme)
    }
}

/**
 * [ColorScheme] is @Stable, so by contract it doesn't have equals implemented. And since it creates a
 * new Colors object to mutate internally, we can't compare references. Instead we compare the
 * properties to make sure that the properties are equal.
 *
 * @return true if all the properties inside [this] are equal to those in [other], false otherwise.
 */
private fun ColorScheme.contentEquals(other: ColorScheme): Boolean {
    if (primary != other.primary) return false
    if (onPrimary != other.onPrimary) return false
    if (primaryContainer != other.primaryContainer) return false
    if (onPrimaryContainer != other.onPrimaryContainer) return false
    if (inversePrimary != other.inversePrimary) return false
    if (secondary != other.secondary) return false
    if (onSecondary != other.onSecondary) return false
    if (secondaryContainer != other.secondaryContainer) return false
    if (onSecondaryContainer != other.onSecondaryContainer) return false
    if (tertiary != other.tertiary) return false
    if (onTertiary != other.onTertiary) return false
    if (tertiaryContainer != other.tertiaryContainer) return false
    if (onTertiaryContainer != other.onTertiaryContainer) return false
    if (background != other.background) return false
    if (onBackground != other.onBackground) return false
    if (surface != other.surface) return false
    if (onSurface != other.onSurface) return false
    if (surfaceVariant != other.surfaceVariant) return false
    if (onSurfaceVariant != other.onSurfaceVariant) return false
    if (inverseSurface != other.inverseSurface) return false
    if (inverseOnSurface != other.inverseOnSurface) return false
    if (error != other.error) return false
    if (onError != other.onError) return false
    if (errorContainer != other.errorContainer) return false
    if (onErrorContainer != other.onErrorContainer) return false
    if (outline != other.outline) return false
    return true
}
