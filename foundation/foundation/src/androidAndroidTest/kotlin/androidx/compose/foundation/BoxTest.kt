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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class BoxTest {

    @get:Rule
    val rule = createComposeRule()

    private val contentTag = "Box"

    @Test
    fun box_testPadding_all() {
        var childSize: IntSize? = null
        val size = 100.dp
        val padding = 20.dp
        rule.setContent {
            SemanticsParent {
                Box(Modifier.preferredSize(size).padding(padding)) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(rule.density) {
            val paddingSide = padding.toIntPx()
            Truth.assertThat(childSize!!.width).isEqualTo(size.toIntPx() - paddingSide * 2)
            Truth.assertThat(childSize!!.height).isEqualTo(size.toIntPx() - paddingSide * 2)
        }
    }

    @Test
    fun box_testPadding_separate() {
        var childSize: IntSize? = null
        var childPosition: Offset? = null
        val size = 100.dp
        val start = 17.dp
        val top = 2.dp
        val end = 5.dp
        val bottom = 8.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size).padding(start, top, end, bottom)
                ) {
                    Box(Modifier.fillMaxSize().onPositioned {
                        childSize = it.size
                        childPosition = it.positionInRoot
                    })
                }
            }
        }
        with(rule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - start.toIntPx() - end.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
            Truth.assertThat(childPosition!!)
                .isEqualTo(Offset(start.toIntPx().toFloat(), top.toIntPx().toFloat()))
        }
    }

    @Test
    fun box_testPadding_rtl() {
        var childSize: IntSize? = null
        var childPosition: Offset? = null
        val size = 100.dp
        val start = 17.dp
        val top = 2.dp
        val end = 5.dp
        val bottom = 8.dp
        rule.setContent {
            SemanticsParent {
                Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                    Box(
                        Modifier.preferredSize(size).padding(start, top, end, bottom)
                    ) {
                        Box(Modifier.fillMaxSize().onPositioned {
                            childSize = it.size
                            childPosition = it.positionInRoot
                        })
                    }
                }
            }
        }
        with(rule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - start.toIntPx() - end.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
            Truth.assertThat(childPosition!!)
                .isEqualTo(Offset(end.toIntPx().toFloat(), top.toIntPx().toFloat()))
        }
    }

    @Test
    fun box_testPadding_concreteOverride() {
        var childSize: IntSize? = null
        val size = 100.dp
        val padding = 10.dp
        val left = 17.dp
        val top = 2.dp
        val bottom = 8.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier
                        .preferredSize(size)
                        .padding(start = left, top = top, bottom = bottom, end = padding)
                ) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(rule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - left.toIntPx() - padding.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
        }
    }

    @Test
    fun box_testLayout_multipleChildren() {
        val size = 100.dp
        val childSize = 20.dp
        var childPosition1: Offset? = null
        var childPosition2: Offset? = null
        var childPosition3: Offset? = null
        rule.setContent {
            SemanticsParent {
                Box(
                    modifier = Modifier.preferredSize(size),
                    alignment = Alignment.TopCenter
                ) {
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition1 = it.positionInRoot
                    })
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition2 = it.positionInRoot
                    })
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition3 = it.positionInRoot
                    })
                }
            }
        }
        with(rule.density) {
            Truth.assertThat(childPosition1).isEqualTo(
                Offset(
                    (size.toIntPx() - childSize.toIntPx()) / 2f,
                    0f
                )
            )
            Truth.assertThat(childPosition2).isEqualTo(
                Offset(
                    (size.toIntPx() - childSize.toIntPx()) / 2f,
                    childSize.toIntPx().toFloat()
                )
            )
            Truth.assertThat(childPosition3).isEqualTo(
                Offset(
                    (size.toIntPx() - childSize.toIntPx()) / 2f,
                    childSize.toIntPx().toFloat() * 2
                )
            )
        }
    }

    @Test
    fun box_testLayout_absoluteAlignment() {
        val size = 100.dp
        val childSize = 20.dp
        var childPosition: Offset? = null
        rule.setContent {
            SemanticsParent {
                Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier.preferredSize(size),
                        alignment = AbsoluteAlignment.TopLeft
                    ) {
                        Box(Modifier.size(childSize).onPositioned {
                            childPosition = it.positionInRoot
                        })
                    }
                }
            }
        }
        Truth.assertThat(childPosition).isEqualTo(Offset(0f, 0f))
    }

    @Test
    fun box_testBackground() {
        rule.setContent {
            SemanticsParent {
                Box(Modifier.preferredSize(50.dp).background(Color.Red))
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToBitmap()
        bitmap.assertShape(rule.density, RectangleShape, Color.Red, Color.Red)
    }

    @Test
    fun box_testBackground_doesntAffectPadding() {
        val size = 50.dp
        val padding = 10.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size).background(Color.Red).padding(padding)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Blue))
                }
            }
        }
        with(rule.density) {
            val bitmap = rule.onNodeWithTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeSizeY = (size.toPx() - padding.toPx() - padding.toPx())
            )
        }
    }

    @Test
    fun box_testBackground_shape() {
        val size = 50.dp
        val padding = 10.dp
        rule.setContent {
            SemanticsParent {
                Box(Modifier.preferredSize(size).background(Color.Red).padding(padding)) {
                    Box(Modifier.fillMaxSize().background(Color.Blue, CircleShape))
                }
            }
        }
        with(rule.density) {
            val bitmap = rule.onNodeWithTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeSizeY = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder() {
        val size = 50.dp
        val borderSize = 10.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier
                        .preferredSize(size)
                        .background(Color.Blue)
                        .border(BorderStroke(borderSize, Color.Red))
                        .padding(borderSize)
                )
            }
        }
        with(rule.density) {
            val bitmap = rule.onNodeWithTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - borderSize.toPx() * 2),
                shapeSizeY = (size.toPx() - borderSize.toPx() * 2),
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder_respectsShape() {
        val size = 50.dp
        val borderSize = 10.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size).background(Color.Red)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Blue, CircleShape)
                            .border(BorderStroke(borderSize, Color.Blue), CircleShape)
                    )
                }
            }
        }
        with(rule.density) {
            val bitmap = rule.onNodeWithTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder_addsPadding() {
        var childSize: IntSize? = null
        val size = 50.dp
        val borderSize = 10.dp
        rule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size)
                        .border(BorderStroke(borderSize, Color.Red))
                        .padding(borderSize)
                ) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(rule.density) {
            Truth.assertThat(childSize!!.width)
                .isEqualTo(size.toIntPx() - borderSize.toIntPx() * 2)
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - borderSize.toIntPx() * 2)
        }
    }

    @Composable
    private fun SemanticsParent(children: @Composable Density.() -> Unit) {
        Stack(
            Modifier
                .testTag(contentTag)
                .wrapContentSize(Alignment.TopStart)
        ) {
            DensityAmbient.current.children()
        }
    }
}
