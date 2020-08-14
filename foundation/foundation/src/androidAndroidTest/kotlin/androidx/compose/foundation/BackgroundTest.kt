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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Density
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableParameter
import androidx.compose.ui.platform.testTag
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class BackgroundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contentTag = "Content"

    @Test
    fun background_colorRect() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40f.toDp()).background(Color.Magenta),
                    gravity = ContentGravity.Center
                ) {
                    Box(Modifier.preferredSize(20f.toDp()).background(Color.White))
                }
            }
        }
        val bitmap = onNodeWithTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_brushRect() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40f.toDp()).background(Color.Magenta),
                    gravity = ContentGravity.Center
                ) {
                    Box(
                        Modifier.preferredSize(20f.toDp())
                            .background(SolidColor(Color.White))
                    )
                }
            }
        }
        val bitmap = onNodeWithTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_colorCircle() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40f.toDp())
                        .background(Color.Magenta)
                        .background(color = Color.White, shape = CircleShape)
                )
            }
        }
        val bitmap = onNodeWithTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_brushCircle() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40f.toDp())
                        .background(Color.Magenta)
                        .background(
                            brush = SolidColor(Color.White),
                            shape = CircleShape
                        )
                )
            }
        }
        val bitmap = onNodeWithTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun testInspectableParameter() {
        val exclusions = listOf("nameFallback", "lastSize", "lastOutline")
        val modifier = Modifier.background(Color.Magenta) as InspectableParameter
        assertThat(modifier.nameFallback).isEqualTo("background")
        assertThat(modifier.valueOverride).isEqualTo(Color.Magenta)
        assertThat(modifier.inspectableElements.map { it.name }.toList())
            .containsExactlyElementsIn(modifier.javaClass.declaredFields
                .filter { !it.isSynthetic && !exclusions.contains(it.name) }
                .map { it.name })
    }

    @Composable
    private fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack(Modifier.testTag(contentTag)) {
            DensityAmbient.current.children()
        }
    }
}