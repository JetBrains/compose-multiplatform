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

package androidx.compose.material.icons

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.javaGetter

const val ProgrammaticTestTag = "programmatic"
const val XmlTestTag = "Xml"

/**
 * Test to ensure equality (both structurally, and visually) between programmatically generated
 * Material [androidx.compose.material.icons.Icons] and their XML source.
 */
@Suppress("unused")
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class IconComparisonTest(
    private val iconSublist: List<Pair<KProperty0<ImageVector>, String>>,
    private val debugParameterName: String
) {

    companion object {
        /**
         * Arbitrarily split [AllIcons] into equal parts. This is needed as one test with the
         * whole of [AllIcons] will exceed the timeout allowed for a test in CI, so we split it
         * up to stay under the limit.
         *
         * Additionally, we run large batches of comparisons per method, instead of one icon per
         * method, so that we can re-use the same Activity instance between test runs. Most of the
         * cost of a simple test like this is in Activity instantiation so re-using the same
         * activity reduces time to run this test ~tenfold.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initIconSublist(): Array<Array<Any>> {
            val numberOfChunks = 6
            val listSize = ceil(AllIcons.size / numberOfChunks.toFloat()).roundToInt()
            val subLists = AllIcons.chunked(listSize)
            return subLists.mapIndexed { index, list ->
                arrayOf(list, "${index + 1}of$numberOfChunks")
            }.toTypedArray()
        }
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val matcher = MSSIMMatcher(threshold = 0.99)

    @Test
    fun compareImageVectors() {
        iconSublist.forEach { (property, drawableName) ->
            var xmlVector: ImageVector? = null
            val programmaticVector = property.get()

            rule.activityRule.scenario.onActivity {
                it.setContent {
                    xmlVector = drawableName.toImageVector()
                    DrawVectors(programmaticVector, xmlVector!!)
                }
            }

            rule.waitForIdle()

            val iconName = property.javaGetter!!.declaringClass.canonicalName!!

            // The XML inflated ImageVector doesn't have a name, and we set a name in the
            // programmatic ImageVector. This doesn't affect how the ImageVector is drawn, so we
            // make sure the names match so the comparison does not fail.
            xmlVector = xmlVector!!.copy(name = programmaticVector.name)

            assertImageVectorsAreEqual(xmlVector!!, programmaticVector, iconName)

            matcher.assertBitmapsAreEqual(
                rule.onNodeWithTag(XmlTestTag).captureToImage().asAndroidBitmap(),
                rule.onNodeWithTag(ProgrammaticTestTag).captureToImage().asAndroidBitmap(),
                iconName
            )

            // Dispose between composing each pair of icons to ensure correctness
            rule.activityRule.scenario.onActivity {
                it.setContentView(View(it))
            }
        }
    }
}

/**
 * Helper method to copy the existing [ImageVector] modifying the name
 * for use in equality checks.
 */
private fun ImageVector.copy(name: String): ImageVector {
    val builder = ImageVector.Builder(
        name, defaultWidth, defaultHeight, viewportWidth, viewportHeight, tintColor, tintBlendMode
    )
    val root = this.root
    // Stack of vector groups and current child index being traversed
    val stack = ArrayList<Pair<Int, VectorGroup>>()
    stack.add(Pair(0, root))

    while (!stack.isEmpty()) {
        val current = stack[stack.size - 1]
        var currentIndex = current.first
        var currentGroup = current.second
        while (currentIndex < currentGroup.size) {
            val vectorNode = currentGroup[currentIndex]
            when (vectorNode) {
                is VectorGroup -> {
                    // keep track of the current index to continue parsing groups
                    // when we eventually "pop" the stack of groups
                    stack.add(Pair(currentIndex + 1, currentGroup))
                    builder.addGroup(
                        name = vectorNode.name,
                        rotate = vectorNode.rotation,
                        pivotX = vectorNode.pivotX,
                        pivotY = vectorNode.pivotY,
                        scaleX = vectorNode.scaleX,
                        scaleY = vectorNode.scaleY,
                        translationX = vectorNode.translationX,
                        translationY = vectorNode.translationY,
                        clipPathData = vectorNode.clipPathData
                    )
                    currentGroup = vectorNode
                    currentIndex = 0
                }
                is VectorPath -> {
                    builder.addPath(
                        name = vectorNode.name,
                        pathData = vectorNode.pathData,
                        pathFillType = vectorNode.pathFillType,
                        fill = vectorNode.fill,
                        fillAlpha = vectorNode.fillAlpha,
                        stroke = vectorNode.stroke,
                        strokeAlpha = vectorNode.strokeAlpha,
                        strokeLineWidth = vectorNode.strokeLineWidth,
                        strokeLineCap = vectorNode.strokeLineCap,
                        strokeLineJoin = vectorNode.strokeLineJoin,
                        strokeLineMiter = vectorNode.strokeLineMiter,
                        trimPathStart = vectorNode.trimPathStart,
                        trimPathEnd = vectorNode.trimPathEnd,
                        trimPathOffset = vectorNode.trimPathOffset
                    )
                }
            }
            currentIndex++
        }
        // "pop" the most recent group after we have examined each of the children
        stack.removeAt(stack.size - 1)
    }
    return builder.build()
}

/**
 * @return the [ImageVector] matching the drawable with [this] name.
 */
@Composable
private fun String.toImageVector(): ImageVector {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(this, "drawable", context.packageName)
    return ImageVector.vectorResource(resId)
}

/**
 * Compares two [ImageVector]s and ensures that they are deeply equal, comparing all children
 * recursively.
 */
private fun assertImageVectorsAreEqual(
    xmlVector: ImageVector,
    programmaticVector: ImageVector,
    iconName: String
) {
    try {
        Truth.assertThat(programmaticVector).isEqualTo(xmlVector)
    } catch (e: AssertionError) {
        val message = "ImageVector comparison failed for $iconName\n" + e.localizedMessage
        throw AssertionError(message, e)
    }
}

/**
 * Compares each pixel in two bitmaps, asserting they are equal.
 */
private fun MSSIMMatcher.assertBitmapsAreEqual(
    xmlBitmap: Bitmap,
    programmaticBitmap: Bitmap,
    iconName: String
) {
    try {
        Truth.assertThat(programmaticBitmap.width).isEqualTo(xmlBitmap.width)
        Truth.assertThat(programmaticBitmap.height).isEqualTo(xmlBitmap.height)
    } catch (e: AssertionError) {
        val message = "Bitmap comparison failed for $iconName\n" + e.localizedMessage
        throw AssertionError(message, e)
    }

    val xmlPixelArray = with(xmlBitmap) {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        pixels
    }

    val programmaticPixelArray = with(programmaticBitmap) {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        pixels
    }

    val result = this.compareBitmaps(
        xmlPixelArray, programmaticPixelArray,
        programmaticBitmap.width, programmaticBitmap.height
    )

    if (!result.matches) {
        throw AssertionError(
            "Bitmap comparison failed for $iconName, stats: " +
                "${result.comparisonStatistics}\n"
        )
    }
}

/**
 * Renders both vectors in a column using the corresponding [ProgrammaticTestTag] and
 * [XmlTestTag] for [programmaticVector] and [xmlVector].
 */
@Composable
private fun DrawVectors(programmaticVector: ImageVector, xmlVector: ImageVector) {
    Box {
        // Ideally these icons would be 24 dp, but due to density changes across devices we test
        // against in CI, on some devices using DP here causes there to be anti-aliasing issues.
        // Using ipx directly ensures that we will always have a consistent layout / drawing
        // story, so anti-aliasing should be identical.
        val layoutSize = with(LocalDensity.current) {
            Modifier.size(72.toDp())
        }
        Row(Modifier.align(Alignment.Center)) {
            Box(
                modifier = layoutSize.paint(
                    rememberVectorPainter(programmaticVector),
                    colorFilter = ColorFilter.tint(Color.Red)
                ).testTag(ProgrammaticTestTag)
            )
            Box(
                modifier = layoutSize.paint(
                    rememberVectorPainter(xmlVector),
                    colorFilter = ColorFilter.tint(Color.Red)
                ).testTag(XmlTestTag)
            )
        }
    }
}