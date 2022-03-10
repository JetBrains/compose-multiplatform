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
package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ColorSchemeScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun colorScheme() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorSchemeDemo()
            }
        }
        assertToggeableAgainstGolden("color_scheme_${scheme.name}")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        private val LightCustomColorScheme = lightColorScheme(
            primary = Color(0xFF984816),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDBC9),
            onPrimaryContainer = Color(0xFF341000),
            inversePrimary = Color(0xFFFFB68F),
            secondary = Color(0xFF765849),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDBC9),
            onSecondaryContainer = Color(0xFF2B160B),
            tertiary = Color(0xFF656032),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFEBE4AA),
            onTertiaryContainer = Color(0xFF1F1C00),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF201A17),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF201A17),
            surfaceVariant = Color(0xFFF4DED5),
            onSurfaceVariant = Color(0xFF53443D),
            inverseSurface = Color(0xFF362F2C),
            inverseOnSurface = Color(0xFFFBEEE9),
            error = Color(0xFFBA1B1B),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD4),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF85736B),
        )

        private val DarkCustomColorScheme = darkColorScheme(
            primary = Color(0xFFFFB68F),
            onPrimary = Color(0xFF562000),
            primaryContainer = Color(0xFF793100),
            onPrimaryContainer = Color(0xFFFFDBC9),
            inversePrimary = Color(0xFF984816),
            secondary = Color(0xFFE6BEAC),
            onSecondary = Color(0xFF432B1E),
            secondaryContainer = Color(0xFF5C4032),
            onSecondaryContainer = Color(0xFFFFDBC9),
            tertiary = Color(0xFFCFC890),
            onTertiary = Color(0xFF353107),
            tertiaryContainer = Color(0xFF4C481C),
            onTertiaryContainer = Color(0xFFEBE4AA),
            background = Color(0xFF201A17),
            onBackground = Color(0xFFEDE0DB),
            surface = Color(0xFF201A17),
            onSurface = Color(0xFFEDE0DB),
            surfaceVariant = Color(0xFF53443D),
            onSurfaceVariant = Color(0xFFD7C2B9),
            inverseSurface = Color(0xFFEDE0DB),
            inverseOnSurface = Color(0xFF362F2C),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFFA08D85),
        )

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper("light", lightColorScheme()),
            ColorSchemeWrapper("light_dynamic", LightCustomColorScheme),
            ColorSchemeWrapper("dark", darkColorScheme()),
            ColorSchemeWrapper("dark_dynamic", DarkCustomColorScheme),
        )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private val Tag = "ColorScheme"
}

@Composable
private fun ColorSchemeDemo() {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Surfaces", style = MaterialTheme.typography.bodyLarge)
            ColorTile(
                text = "On Background",
                color = colorScheme.onBackground,
            )
            ColorTile(
                text = "Background",
                color = colorScheme.background,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Surface",
                        color = colorScheme.onSurface,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Surface Variant",
                        color = colorScheme.onSurfaceVariant,
                    )
                },
            )
            ColorTile(text = "Surface", color = colorScheme.surface)
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Primary",
                        color = colorScheme.inversePrimary,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Inverse On Surface",
                        color = colorScheme.inverseOnSurface,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Surface Variant",
                        color = colorScheme.surfaceVariant,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Inverse Surface",
                        color = colorScheme.inverseSurface,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Surface Tint",
                        color = colorScheme.surfaceTint,
                    )
                },
                rightTile = { Box(Modifier.fillMaxWidth()) },
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Content", style = MaterialTheme.typography.bodyLarge)
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Primary Container",
                        color = colorScheme.onPrimaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Primary",
                        color = colorScheme.onPrimary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Primary Container",
                        color = colorScheme.primaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Primary",
                        color = colorScheme.primary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Secondary Container",
                        color = colorScheme.onSecondaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Secondary",
                        color = colorScheme.onSecondary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Secondary Container",
                        color = colorScheme.secondaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Secondary",
                        color = colorScheme.secondary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Tertiary Container",
                        color = colorScheme.onTertiaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Tertiary",
                        color = colorScheme.onTertiary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Tertiary Container",
                        color = colorScheme.tertiaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Tertiary",
                        color = colorScheme.tertiary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Utility", style = MaterialTheme.typography.bodyLarge)
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Error",
                        color = colorScheme.onError,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Outline",
                        color = colorScheme.outline,
                    )
                }
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Error",
                        color = colorScheme.error,
                    )
                },
                rightTile = { Box(Modifier.fillMaxWidth()) },
            )
        }
    }
}

@Composable
private fun DoubleTile(leftTile: @Composable () -> Unit, rightTile: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) { leftTile() }
        Box(modifier = Modifier.weight(1f)) { rightTile() }
    }
}

@Composable
private fun ColorTile(text: String, color: Color) {
    var borderColor = Color.Transparent
    if (color == Color.Black) {
        borderColor = Color.White
    } else if (color == Color.White) borderColor = Color.Black

    Surface(
        modifier = Modifier.height(48.dp).fillMaxWidth(),
        color = color,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text,
            Modifier.padding(4.dp),
            style =
            MaterialTheme.typography.bodyMedium.copy(
                if (color.luminance() < .25) Color.White else Color.Black
            )
        )
    }
}
