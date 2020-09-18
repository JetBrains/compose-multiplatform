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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun zOrderingBasedOnElevationIsApplied() {
        rule.setMaterialContent {
            Box(
                Modifier
                    .preferredSize(10.dp, 10.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
                Surface(color = Color.Green) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag("box")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Yellow,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun originalOrderingWhenTheDefaultElevationIsUsed() {
        rule.setMaterialContent {
            Box(
                Modifier
                    .preferredSize(10.dp, 10.dp)
                    .semantics(mergeAllDescendants = true) {}
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
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun elevationRawValueIsUsedAsZIndex_drawsBelow() {
        rule.setMaterialContent {
            Box(
                Modifier
                    .preferredSize(10.dp, 10.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("box")
            ) {
                Box(Modifier.fillMaxSize().background(color = Color.Green).zIndex(3f))
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag("box")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.Green
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun elevationRawValueIsUsedAsZIndex_drawsAbove() {
        rule.setMaterialContent {
            Box(
                Modifier
                    .preferredSize(10.dp, 10.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("box")
            ) {
                Box(Modifier.fillMaxSize().background(color = Color.Green).zIndex(1f))
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag("box")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Yellow,
                backgroundColor = Color.Yellow
            )
    }
}
