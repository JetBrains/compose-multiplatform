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

import androidx.compose.foundation.AmbientContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
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
    val rule = createComposeRule()

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    val onSurface = MaterialTheme.colors.onSurface

                    assertThat(AmbientContentColor.current).isEqualTo(onSurface)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(AmbientEmphasisLevels.current.high) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastHighEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(AmbientEmphasisLevels.current.medium) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastMediumEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(AmbientEmphasisLevels.current.disabled) {
                        val onSurface = MaterialTheme.colors.onSurface
                        val modifiedOnSurface = onSurface.copy(
                            alpha = ReducedContrastDisabledEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    val onPrimary = MaterialTheme.colors.onPrimary

                    assertThat(AmbientContentColor.current).isEqualTo(onPrimary)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.high) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastHighEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.medium) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastMediumEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.disabled) {
                        val onPrimary = MaterialTheme.colors.onPrimary
                        val modifiedOnPrimary = onPrimary.copy(
                            alpha = HighContrastDisabledEmphasisAlpha
                        )

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_colorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    assertThat(AmbientContentColor.current).isEqualTo(Color.Yellow)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.high) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastHighEmphasisAlpha
                        } else {
                            ReducedContrastHighEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastMediumEmphasisAlpha
                        } else {
                            ReducedContrastMediumEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastDisabledEmphasisAlpha
                        } else {
                            ReducedContrastDisabledEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.high) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastHighEmphasisAlpha
                        } else {
                            HighContrastHighEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastMediumEmphasisAlpha
                        } else {
                            HighContrastMediumEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun disabledEmphasis_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastDisabledEmphasisAlpha
                        } else {
                            HighContrastDisabledEmphasisAlpha
                        }
                        val modifiedColor = contentColor.copy(alpha = expectedAlpha)

                        assertThat(AmbientContentColor.current).isEqualTo(modifiedColor)
                    }
                }
            }
        }
    }

    @Test
    fun translucentColor_emphasisNotApplied() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.5f, 0.5f, 0.5f, 0.5f)
                Surface(contentColor = contentColor) {
                    ProvideEmphasis(AmbientEmphasisLevels.current.high) {
                        assertThat(AmbientContentColor.current).isEqualTo(contentColor)
                    }
                }
            }
        }
    }
}
