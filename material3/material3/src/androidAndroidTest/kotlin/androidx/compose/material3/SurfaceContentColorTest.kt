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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceContentColorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun surfaceSetsCorrectContentColors_primary() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.primary) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_primaryContainer() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_secondary() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.secondary) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSecondary)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_secondaryContainer() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_tertiary() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.tertiary) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_tertiaryContainer() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_error() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.error) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onError)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_errorContainer() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_background() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_surface() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    @Test
    fun surfaceSetsCorrectContentColors_surfaceVariant() {
        rule.setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Test
    fun surfaceDoesNotSetContentColor_withCustomColor() {
        rule.setContent {
            MaterialTheme {
                Surface(color = Color.Yellow) {
                    assertThat(LocalContentColor.current).isEqualTo(Color.Black)
                }
            }
        }
    }

    @Test
    fun surfaceInheritsParent_withCustomColor() {
        rule.setContent {
            MaterialTheme {
                // This surface sets contentColor to be onSurface
                Surface {
                    // This surface should inherit the parent contentColor, as yellow is not part
                    // of the theme
                    Surface(color = Color.Yellow) {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
