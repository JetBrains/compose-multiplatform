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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun originalOrderingWhenTheDefaultElevationIsUsed() {
        rule.setMaterialContent {
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(color = Color.Yellow) {
                    Box(Modifier.fillMaxSize())
                }
                Surface(color = Color.Green) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.White
            )
    }

    @Test
    fun absoluteElevationCompositionLocalIsSet() {
        var outerElevation: Dp? = null
        var innerElevation: Dp? = null
        rule.setMaterialContent {
            Surface(elevation = 2.dp) {
                outerElevation = LocalAbsoluteElevation.current
                Surface(elevation = 4.dp) {
                    innerElevation = LocalAbsoluteElevation.current
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(outerElevation).isEqualTo(2.dp)
            Truth.assertThat(innerElevation).isEqualTo(6.dp)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun absoluteElevationIsNotUsedForShadows() {
        rule.setMaterialContent {
            Column {
                Box(
                    Modifier
                        .padding(10.dp)
                        .size(10.dp, 10.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("top level")
                ) {
                    Surface(
                        Modifier.fillMaxSize().padding(2.dp),
                        elevation = 2.dp,
                        color = Color.Blue,
                        content = {}
                    )
                }

                // Nested surface to increase the absolute elevation
                Surface(elevation = 2.dp) {
                    Box(
                        Modifier
                            .padding(10.dp)
                            .size(10.dp, 10.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("nested")
                    ) {
                        Surface(
                            Modifier.fillMaxSize().padding(2.dp),
                            elevation = 2.dp,
                            color = Color.Blue,
                            content = {}
                        )
                    }
                }
            }
        }

        val topLevelSurfaceBitmap = rule.onNodeWithTag("top level").captureToImage()
        val nestedSurfaceBitmap = rule.onNodeWithTag("nested").captureToImage()
            .asAndroidBitmap()

        topLevelSurfaceBitmap.assertPixels {
            Color(nestedSurfaceBitmap.getPixel(it.x, it.y))
        }
    }

    /**
     * Tests that composed modifiers applied to Surface are applied within the changes to
     * [LocalContentColor], so they can consume the updated values.
     */
    @Test
    fun contentColorSetBeforeModifier() {
        var contentColor: Color = Color.Unspecified
        val expectedColor = Color.Blue
        rule.setMaterialContent {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                Surface(
                    Modifier.composed {
                        contentColor = LocalContentColor.current
                        Modifier
                    },
                    elevation = 2.dp,
                    contentColor = expectedColor,
                    content = {}
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentColor).isEqualTo(expectedColor)
        }
    }
}
