/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.test.filters.MediumTest
import androidx.compose.foundation.contentColor
import androidx.compose.ui.graphics.Color
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("unused")
@MediumTest
@RunWith(Parameterized::class)
class EmphasisTest(private val colors: Colors, private val debugParameterName: String) {
    private val ReducedContrastHighEmphasisAlpha = 0.87f
    private val ReducedContrastMediumEmphasisAlpha = 0.60f
    private val ReducedContrastDisabledEmphasisAlpha = 0.38f

    private val HighContrastHighEmphasisAlpha = 1.00f
    private val HighContrastMediumEmphasisAlpha = 0.74f
    private val HighContrastDisabledEmphasisAlpha = 0.38f

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initColors() = arrayOf(
            arrayOf(lightColors(), "Light theme"),
            arrayOf(darkColors(), "Dark theme")
        )
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    val onSurface = MaterialTheme.colors.onSurface

                    assertThat(contentColor()).isEqualTo(onSurface)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(EmphasisAmbient.current.high) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastHighEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(EmphasisAmbient.current.medium) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastMediumEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(EmphasisAmbient.current.disabled) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastDisabledEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    val onPrimary = MaterialTheme.colors.onPrimary

                    assertThat(contentColor()).isEqualTo(onPrimary)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(EmphasisAmbient.current.high) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastHighEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(EmphasisAmbient.current.medium) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastMediumEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(EmphasisAmbient.current.disabled) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastDisabledEmphasisAlpha
                        )

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_colorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    assertThat(contentColor()).isEqualTo(Color.Yellow)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.high) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastHighEmphasisAlpha
                        } else {
                            ReducedContrastHighEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastMediumEmphasisAlpha
                        } else {
                            ReducedContrastMediumEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastDisabledEmphasisAlpha
                        } else {
                            ReducedContrastDisabledEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.high) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastHighEmphasisAlpha
                        } else {
                            HighContrastHighEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastMediumEmphasisAlpha
                        } else {
                            HighContrastMediumEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastDisabledEmphasisAlpha
                        } else {
                            HighContrastDisabledEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun translucentColor_emphasisNotApplied() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.5f, 0.5f, 0.5f, 0.5f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(EmphasisAmbient.current.high) {
                        assertThat(contentColor()).isEqualTo(contentColor)
                    }
                }
            }
        }
    }
}
