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

import androidx.compose.runtime.Providers
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
class ContentAlphaTest(private val colors: Colors, private val debugParameterName: String) {
    private val ReducedContrastHighContentAlpha = 0.87f
    private val ReducedContrastMediumContentAlpha = 0.60f
    private val ReducedContrastDisabledContentAlpha = 0.38f

    private val HighContrastHighContentAlpha = 1.00f
    private val HighContrastMediumContentAlpha = 0.74f
    private val HighContrastDisabledContentAlpha = 0.38f

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
    fun noContentAlphaSpecified_contentColorUnmodified_surface() {
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
    fun highContentAlpha_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    Providers(AmbientContentAlpha provides ContentAlpha.high) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(ReducedContrastHighContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun mediumContentAlpha_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    Providers(AmbientContentAlpha provides ContentAlpha.medium) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(ReducedContrastMediumContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun disabledContentAlpha_contentColorSet_surface() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface {
                    Providers(AmbientContentAlpha provides ContentAlpha.disabled) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(ReducedContrastDisabledContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun noContentAlphaSpecified_contentColorUnmodified_primary() {
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
    fun highContentAlpha_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    Providers(AmbientContentAlpha provides ContentAlpha.high) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(HighContrastHighContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun mediumContentAlpha_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    Providers(AmbientContentAlpha provides ContentAlpha.medium) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(HighContrastMediumContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun disabledContentAlpha_contentColorSet_primary() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    Providers(AmbientContentAlpha provides ContentAlpha.disabled) {
                        assertThat(AmbientContentAlpha.current)
                            .isEqualTo(HighContrastDisabledContentAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun noContentAlphaSpecified_contentColorUnmodified_colorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    assertThat(AmbientContentColor.current).isEqualTo(Color.Yellow)
                }
            }
        }
    }

    @Test
    fun highContentAlpha_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.high) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastHighContentAlpha
                        } else {
                            ReducedContrastHighContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun mediumContentAlpha_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastMediumContentAlpha
                        } else {
                            ReducedContrastMediumContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun disabledContentAlpha_contentColorSet_highLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.9f, 0.9f, 0.9f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            HighContrastDisabledContentAlpha
                        } else {
                            ReducedContrastDisabledContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun highContentAlpha_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.high) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastHighContentAlpha
                        } else {
                            HighContrastHighContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun mediumContentAlpha_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.medium) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastMediumContentAlpha
                        } else {
                            HighContrastMediumContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }

    @Test
    fun disabledContentAlpha_contentColorSet_lowLuminanceColorNotFromTheme() {
        rule.setContent {
            MaterialTheme(colors) {
                val contentColor = Color(0.1f, 0.1f, 0.1f)
                Surface(contentColor = contentColor) {
                    Providers(AmbientContentAlpha provides ContentAlpha.disabled) {
                        val expectedAlpha = if (colors.isLight) {
                            ReducedContrastDisabledContentAlpha
                        } else {
                            HighContrastDisabledContentAlpha
                        }
                        assertThat(AmbientContentAlpha.current).isEqualTo(expectedAlpha)
                    }
                }
            }
        }
    }
}
