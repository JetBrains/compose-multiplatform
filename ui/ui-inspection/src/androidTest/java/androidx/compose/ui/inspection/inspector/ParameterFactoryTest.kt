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

package androidx.compose.ui.inspection.inspector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.packFloats
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val ROOT_ID = 3L
private const val NODE_ID = -7L
private const val ANCHOR_HASH = 77
private const val PARAM_INDEX = 4
private const val MAX_RECURSIONS = 2
private const val MAX_ITERABLE_SIZE = 5

@Suppress("unused")
private fun topLevelFunction() {
}

@MediumTest
@RunWith(AndroidJUnit4::class)
class ParameterFactoryTest {
    private val factory = ParameterFactory(InlineClassConverter())

    @Before
    fun before() {
        factory.density = Density(2.0f)
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testAbsoluteAlignment() {
        assertThat(lookup(AbsoluteAlignment.TopLeft))
            .isEqualTo(ParameterType.String to "TopLeft")
        assertThat(lookup(AbsoluteAlignment.TopRight))
            .isEqualTo(ParameterType.String to "TopRight")
        assertThat(lookup(AbsoluteAlignment.CenterLeft))
            .isEqualTo(ParameterType.String to "CenterLeft")
        assertThat(lookup(AbsoluteAlignment.CenterRight))
            .isEqualTo(ParameterType.String to "CenterRight")
        assertThat(lookup(AbsoluteAlignment.BottomLeft))
            .isEqualTo(ParameterType.String to "BottomLeft")
        assertThat(lookup(AbsoluteAlignment.BottomRight))
            .isEqualTo(ParameterType.String to "BottomRight")
        assertThat(lookup(AbsoluteAlignment.Left))
            .isEqualTo(ParameterType.String to "Left")
        assertThat(lookup(AbsoluteAlignment.Right))
            .isEqualTo(ParameterType.String to "Right")
    }

    @Test
    fun testAlignment() {
        assertThat(lookup(Alignment.Top)).isEqualTo(ParameterType.String to "Top")
        assertThat(lookup(Alignment.Bottom)).isEqualTo(ParameterType.String to "Bottom")
        assertThat(lookup(Alignment.CenterVertically))
            .isEqualTo(ParameterType.String to "CenterVertically")

        assertThat(lookup(Alignment.Start)).isEqualTo(ParameterType.String to "Start")
        assertThat(lookup(Alignment.End)).isEqualTo(ParameterType.String to "End")
        assertThat(lookup(Alignment.CenterHorizontally))
            .isEqualTo(ParameterType.String to "CenterHorizontally")

        assertThat(lookup(Alignment.TopStart)).isEqualTo(ParameterType.String to "TopStart")
        assertThat(lookup(Alignment.TopCenter)).isEqualTo(ParameterType.String to "TopCenter")
        assertThat(lookup(Alignment.TopEnd)).isEqualTo(ParameterType.String to "TopEnd")
        assertThat(lookup(Alignment.CenterStart)).isEqualTo(ParameterType.String to "CenterStart")
        assertThat(lookup(Alignment.Center)).isEqualTo(ParameterType.String to "Center")
        assertThat(lookup(Alignment.CenterEnd)).isEqualTo(ParameterType.String to "CenterEnd")
        assertThat(lookup(Alignment.BottomStart)).isEqualTo(ParameterType.String to "BottomStart")
        assertThat(lookup(Alignment.BottomCenter)).isEqualTo(ParameterType.String to "BottomCenter")
        assertThat(lookup(Alignment.BottomEnd)).isEqualTo(ParameterType.String to "BottomEnd")
    }

    @Test
    fun testAnnotatedString() {
        assertThat(lookup(AnnotatedString("Hello"))).isEqualTo(ParameterType.String to "Hello")
    }

    @Test
    fun testArrangement() {
        assertThat(lookup(Arrangement.Top)).isEqualTo(ParameterType.String to "Top")
        assertThat(lookup(Arrangement.Bottom)).isEqualTo(ParameterType.String to "Bottom")
        assertThat(lookup(Arrangement.Center)).isEqualTo(ParameterType.String to "Center")
        assertThat(lookup(Arrangement.Start)).isEqualTo(ParameterType.String to "Start")
        assertThat(lookup(Arrangement.End)).isEqualTo(ParameterType.String to "End")
        assertThat(lookup(Arrangement.SpaceEvenly)).isEqualTo(ParameterType.String to "SpaceEvenly")
        assertThat(lookup(Arrangement.SpaceBetween))
            .isEqualTo(ParameterType.String to "SpaceBetween")
        assertThat(lookup(Arrangement.SpaceAround)).isEqualTo(ParameterType.String to "SpaceAround")
    }

    @Test
    fun testBaselineShift() {
        assertThat(lookup(BaselineShift.None)).isEqualTo(ParameterType.String to "None")
        assertThat(lookup(BaselineShift.Subscript)).isEqualTo(ParameterType.String to "Subscript")
        assertThat(lookup(BaselineShift.Superscript))
            .isEqualTo(ParameterType.String to "Superscript")
        assertThat(lookup(BaselineShift(0.0f))).isEqualTo(ParameterType.String to "None")
        assertThat(lookup(BaselineShift(-0.5f))).isEqualTo(ParameterType.String to "Subscript")
        assertThat(lookup(BaselineShift(0.5f))).isEqualTo(ParameterType.String to "Superscript")
        assertThat(lookup(BaselineShift(0.1f))).isEqualTo(ParameterType.Float to 0.1f)
        assertThat(lookup(BaselineShift(0.75f))).isEqualTo(ParameterType.Float to 0.75f)
    }

    @Test
    fun testBoolean() {
        assertThat(lookup(true)).isEqualTo(ParameterType.Boolean to true)
        assertThat(lookup(false)).isEqualTo(ParameterType.Boolean to false)
    }

    @Test
    fun testBorder() {
        validate(create("borderstroke", BorderStroke(2.0.dp, Color.Magenta))) {
            parameter("borderstroke", ParameterType.String, "BorderStroke") {
                parameter("brush", ParameterType.Color, Color.Magenta.toArgb())
                parameter("width", ParameterType.DimensionDp, 2.0f)
            }
        }
    }

    @Test
    fun testBrush() {
        assertThat(lookup(SolidColor(Color.Red)))
            .isEqualTo(ParameterType.Color to Color.Red.toArgb())
        validate(
            create(
                "brush",
                Brush.linearGradient(
                    colors = listOf(Color.Red, Color.Blue),
                    start = Offset(0.0f, 0.5f),
                    end = Offset(5.0f, 10.0f)
                )
            )
        ) {
            parameter("brush", ParameterType.String, "LinearGradient") {
                parameter("colors", ParameterType.Iterable, "List[2]") {
                    parameter("[0]", ParameterType.Color, Color.Red.toArgb())
                    parameter("[1]", ParameterType.Color, Color.Blue.toArgb())
                }
                parameter("end", ParameterType.String, Offset::class.java.simpleName) {
                    parameter("x", ParameterType.DimensionDp, 2.5f)
                    parameter("y", ParameterType.DimensionDp, 5.0f)
                }
                parameter("intrinsicSize", ParameterType.String, Size::class.java.simpleName) {
                    val width = 5.0f
                    val height = 9.5f
                    parameter("height", ParameterType.Float, height)
                    parameter("maxDimension", ParameterType.Float, height)
                    parameter("minDimension", ParameterType.Float, width)
                    parameter("packedValue", ParameterType.Int64, packFloats(width, height))
                    parameter("width", ParameterType.Float, width)
                }
                parameter("start", ParameterType.String, Offset::class.java.simpleName) {
                    parameter("x", ParameterType.DimensionDp, 0.0f)
                    parameter("y", ParameterType.DimensionDp, 0.25f)
                }
                parameter("tileMode", ParameterType.String, "Clamp", index = 5)
                parameter("createdSize", ParameterType.String, "Unspecified", index = 6)
            }
        }
        // TODO: add tests for RadialGradient & ShaderBrush
    }

    @Test
    fun testColor() {
        assertThat(lookup(Color.Blue)).isEqualTo(ParameterType.Color to 0xff0000ff.toInt())
        assertThat(lookup(Color.Red)).isEqualTo(ParameterType.Color to 0xffff0000.toInt())
        assertThat(lookup(Color.Transparent)).isEqualTo(ParameterType.Color to 0x00000000)
        assertThat(lookup(Color.Unspecified)).isEqualTo(ParameterType.String to "Unspecified")
    }

    @Test
    fun testComposableLambda() = runBlocking {
        // capture here to force the lambda to not be created as a singleton.
        val capture = "Hello World"
        @Suppress("COMPOSABLE_INVOCATION")
        val c: @Composable () -> Unit = { Text(text = capture) }
        val result = lookup(c as Any)
        val array = result.second as Array<*>
        assertThat(result.first).isEqualTo(ParameterType.Lambda)
        assertThat(array).hasLength(1)
        assertThat(array[0]?.javaClass?.name).isEqualTo(
            "${ParameterFactoryTest::class.java.name}\$testComposableLambda\$1\$c\$1"
        )
    }

    @Test
    fun testCornerBasedShape() {
        validate(create("corner", RoundedCornerShape(2.0.dp, 0.5.dp, 2.5.dp, 0.7.dp))) {
            parameter("corner", ParameterType.String, RoundedCornerShape::class.java.simpleName) {
                parameter("bottomEnd", ParameterType.DimensionDp, 2.5f)
                parameter("bottomStart", ParameterType.DimensionDp, 0.7f)
                parameter("topEnd", ParameterType.DimensionDp, 0.5f)
                parameter("topStart", ParameterType.DimensionDp, 2.0f)
            }
        }
        validate(create("corner", CutCornerShape(2))) {
            parameter("corner", ParameterType.String, CutCornerShape::class.java.simpleName) {
                parameter("bottomEnd", ParameterType.String, "2.0%")
                parameter("bottomStart", ParameterType.String, "2.0%")
                parameter("topEnd", ParameterType.String, "2.0%")
                parameter("topStart", ParameterType.String, "2.0%")
            }
        }
        validate(create("corner", RoundedCornerShape(1.0f, 10.0f, 2.0f, 3.5f))) {
            parameter("corner", ParameterType.String, RoundedCornerShape::class.java.simpleName) {
                parameter("bottomEnd", ParameterType.String, "2.0px")
                parameter("bottomStart", ParameterType.String, "3.5px")
                parameter("topEnd", ParameterType.String, "10.0px")
                parameter("topStart", ParameterType.String, "1.0px")
            }
        }
    }

    @Test
    fun testCornerSize() {
        assertThat(lookup(ZeroCornerSize)).isEqualTo(ParameterType.String to "ZeroCornerSize")
        assertThat(lookup(CornerSize(2.4.dp))).isEqualTo(ParameterType.DimensionDp to 2.4f)
        assertThat(lookup(CornerSize(2.4f))).isEqualTo(ParameterType.String to "2.4px")
        assertThat(lookup(CornerSize(3))).isEqualTo(ParameterType.String to "3.0%")
    }

    @Test
    fun testDouble() {
        assertThat(lookup(3.1428)).isEqualTo(ParameterType.Double to 3.1428)
    }

    @Test
    fun testDp() {
        assertThat(lookup(2.0.dp)).isEqualTo(ParameterType.DimensionDp to 2.0f)
        assertThat(lookup(Dp.Hairline)).isEqualTo(ParameterType.DimensionDp to 0.0f)
        assertThat(lookup(Dp.Unspecified)).isEqualTo(ParameterType.DimensionDp to Float.NaN)
        assertThat(lookup(Dp.Infinity))
            .isEqualTo(ParameterType.DimensionDp to Float.POSITIVE_INFINITY)
    }

    @Test
    fun testEnum() {
        assertThat(lookup(ColorModel.Lab)).isEqualTo(ParameterType.String to "Lab")
        assertThat(lookup(ColorModel.Rgb)).isEqualTo(ParameterType.String to "Rgb")
        assertThat(lookup(ColorModel.Cmyk)).isEqualTo(ParameterType.String to "Cmyk")
    }

    @Test
    fun testFloat() {
        assertThat(lookup(3.1428f)).isEqualTo(ParameterType.Float to 3.1428f)
    }

    @Test
    fun testFontFamily() {
        assertThat(lookup(FontFamily.Cursive)).isEqualTo(ParameterType.String to "Cursive")
        assertThat(lookup(FontFamily.Default)).isEqualTo(ParameterType.String to "Default")
        assertThat(lookup(FontFamily.SansSerif)).isEqualTo(ParameterType.String to "SansSerif")
        assertThat(lookup(FontFamily.Serif)).isEqualTo(ParameterType.String to "Serif")
        assertThat(lookup(FontFamily.Monospace)).isEqualTo(ParameterType.String to "Monospace")
    }

    @Test
    fun testFontListFontFamily() {
        val family = FontFamily(
            listOf(
                Font(1234, FontWeight.Normal, FontStyle.Italic),
                Font(1235, FontWeight.Normal, FontStyle.Normal),
                Font(1236, FontWeight.Bold, FontStyle.Italic),
                Font(1237, FontWeight.Bold, FontStyle.Normal)
            )
        )
        assertThat(lookup(family)).isEqualTo(ParameterType.Resource to 1235)
    }

    @Test
    fun testFontWeight() {
        assertThat(lookup(FontWeight.Thin)).isEqualTo(ParameterType.String to "W100")
        assertThat(lookup(FontWeight.ExtraLight)).isEqualTo(ParameterType.String to "W200")
        assertThat(lookup(FontWeight.Light)).isEqualTo(ParameterType.String to "W300")
        assertThat(lookup(FontWeight.Normal)).isEqualTo(ParameterType.String to "W400")
        assertThat(lookup(FontWeight.Medium)).isEqualTo(ParameterType.String to "W500")
        assertThat(lookup(FontWeight.SemiBold)).isEqualTo(ParameterType.String to "W600")
        assertThat(lookup(FontWeight.Bold)).isEqualTo(ParameterType.String to "W700")
        assertThat(lookup(FontWeight.ExtraBold)).isEqualTo(ParameterType.String to "W800")
        assertThat(lookup(FontWeight.Black)).isEqualTo(ParameterType.String to "W900")
        assertThat(lookup(FontWeight.W100)).isEqualTo(ParameterType.String to "W100")
        assertThat(lookup(FontWeight.W200)).isEqualTo(ParameterType.String to "W200")
        assertThat(lookup(FontWeight.W300)).isEqualTo(ParameterType.String to "W300")
        assertThat(lookup(FontWeight.W400)).isEqualTo(ParameterType.String to "W400")
        assertThat(lookup(FontWeight.W500)).isEqualTo(ParameterType.String to "W500")
        assertThat(lookup(FontWeight.W600)).isEqualTo(ParameterType.String to "W600")
        assertThat(lookup(FontWeight.W700)).isEqualTo(ParameterType.String to "W700")
        assertThat(lookup(FontWeight.W800)).isEqualTo(ParameterType.String to "W800")
        assertThat(lookup(FontWeight.W900)).isEqualTo(ParameterType.String to "W900")
    }

    @Test
    fun testFunctionReference() {
        val ref1 = ::testInt
        val map1 = lookup(ref1)
        val array1 = map1.second as Array<*>
        assertThat(map1.first).isEqualTo(ParameterType.FunctionReference)
        assertThat(array1.contentEquals(arrayOf(ref1, "testInt"))).isTrue()
        val ref2 = ::topLevelFunction
        val map2 = lookup(ref2)
        val array2 = map2.second as Array<*>
        assertThat(map2.first).isEqualTo(ParameterType.FunctionReference)
        assertThat(array2.contentEquals(arrayOf(ref2, "topLevelFunction"))).isTrue()
    }

    @Test
    fun testPaddingValues() {
        validate(create("padding", PaddingValues(2.0.dp, 0.5.dp, 2.5.dp, 0.7.dp))) {
            parameter(
                "padding",
                ParameterType.String,
                "PaddingValuesImpl"
            ) {
                parameter("bottom", ParameterType.DimensionDp, 0.7f)
                parameter("end", ParameterType.DimensionDp, 2.5f)
                parameter("start", ParameterType.DimensionDp, 2.0f)
                parameter("top", ParameterType.DimensionDp, 0.5f)
            }
        }
    }

    @Test
    fun testInt() {
        assertThat(lookup(12345)).isEqualTo(ParameterType.Int32 to 12345)
    }

    @Test
    fun testLambda() {
        val a: (Int) -> Int = { it }
        val map = lookup(a)
        val array = map.second as Array<*>
        assertThat(map.first).isEqualTo(ParameterType.Lambda)
        assertThat(array.contentEquals(arrayOf<Any>(a))).isTrue()
    }

    @Test
    fun testLocale() {
        assertThat(lookup(Locale("fr-CA"))).isEqualTo(ParameterType.String to "fr-CA")
        assertThat(lookup(Locale("fr-BE"))).isEqualTo(ParameterType.String to "fr-BE")
    }

    @Test
    fun testLocaleList() {
        validate(create("locales", LocaleList(Locale("fr-ca"), Locale("fr-be")))) {
            parameter("locales", ParameterType.Iterable, "Collection[2]") {
                parameter("[0]", ParameterType.String, "fr-CA")
                parameter("[1]", ParameterType.String, "fr-BE")
            }
        }
    }

    @Test
    fun testLong() {
        assertThat(lookup(12345L)).isEqualTo(ParameterType.Int64 to 12345L)
    }

    @Test
    fun testMap() {
        val map = mapOf(1 to "one", 2 to "two")
        validate(create("map", map)) {
            parameter("map", ParameterType.Iterable, "Map[2]") {
                parameter("[1]", ParameterType.String, "one") {
                    parameter("key", ParameterType.Int32, 1)
                    parameter("value", ParameterType.String, "one")
                }
                parameter("[2]", ParameterType.String, "two") {
                    parameter("key", ParameterType.Int32, 2)
                    parameter("value", ParameterType.String, "two")
                }
            }
        }
    }

    @Test
    fun testMapEntry() {
        val entry = object : Map.Entry<String, String> {
            override val key = "Hello"
            override val value = "World"
        }
        validate(create("myEntry", entry)) {
            parameter("myEntry", ParameterType.String, "World") {
                parameter("key", ParameterType.String, "Hello")
                parameter("value", ParameterType.String, "World")
            }
        }
    }

    @Test
    fun testMapWithComplexTypes() {
        val k1 = MyClass("k1")
        val k2 = MyClass("k2")
        val v1 = MyClass("v1")
        val v2 = MyClass("v2")
        val map = mapOf(k1 to v1, k2 to v2)
        validate(create("map", map, maxRecursions = 3)) {
            parameter("map", ParameterType.Iterable, "Map[2]") {
                parameter("[MyClass]", ParameterType.String, "MyClass") {
                    parameter("key", ParameterType.String, "MyClass") {
                        parameter("name", ParameterType.String, "k1")
                    }
                    parameter("value", ParameterType.String, "MyClass") {
                        parameter("name", ParameterType.String, "v1")
                    }
                }
                parameter("[MyClass]", ParameterType.String, "MyClass") {
                    parameter("key", ParameterType.String, "MyClass") {
                        parameter("name", ParameterType.String, "k2")
                    }
                    parameter("value", ParameterType.String, "MyClass") {
                        parameter("name", ParameterType.String, "v2")
                    }
                }
            }
        }
    }

    @Test
    fun testShortIntArray() {
        val value = intArrayOf(10, 11, 12)
        val parameter = create("array", value)
        validate(parameter) {
            parameter("array", ParameterType.Iterable, "IntArray[3]") {
                parameter("[0]", ParameterType.Int32, 10)
                parameter("[1]", ParameterType.Int32, 11)
                parameter("[2]", ParameterType.Int32, 12)
            }
        }
    }

    @Test
    fun testLongIntArray() {
        val value = intArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
        val refToSelf = ref()
        val display = "IntArray[14]"
        val parameter = create("array", value)
        validate(parameter) {
            parameter("array", ParameterType.Iterable, display, refToSelf) {
                parameter("[0]", ParameterType.Int32, 10)
                parameter("[1]", ParameterType.Int32, 11)
                parameter("[2]", ParameterType.Int32, 12)
                parameter("[3]", ParameterType.Int32, 13)
                parameter("[4]", ParameterType.Int32, 14)
            }
        }

        // If we need to retrieve more array elements we call "expand" with the reference:
        validate(expand("array", value, refToSelf, 5, 5)!!) {
            parameter("array", ParameterType.Iterable, display, refToSelf, childStartIndex = 5) {
                parameter("[5]", ParameterType.Int32, 15)
                parameter("[6]", ParameterType.Int32, 16)
                parameter("[7]", ParameterType.Int32, 17)
                parameter("[8]", ParameterType.Int32, 18)
                parameter("[9]", ParameterType.Int32, 19)
            }
        }

        // Call "expand" again to retrieve more:
        validate(expand("array", value, refToSelf, 10, 5)!!) {
            // This time we reached the end of the array, and we do not get a reference to get more
            parameter("array", ParameterType.Iterable, display, childStartIndex = 10) {
                parameter("[10]", ParameterType.Int32, 20)
                parameter("[11]", ParameterType.Int32, 21)
                parameter("[12]", ParameterType.Int32, 22)
                parameter("[13]", ParameterType.Int32, 23)
            }
        }
    }

    @Test
    fun testListWithNullElement() {
        val value = listOf(
            "a",
            null,
            "b",
            "c",
            null,
            null,
            null,
            null,
            "d",
            null,
            "e",
            null,
            null,
            null,
            null,
            null,
            "f",
            null,
            "g",
            null
        )
        val parameter = create("array", value)
        val refToSelf = ref()
        val display = "List[20]"
        validate(parameter) {
            // Here we get all the available elements from the list.
            // There is no need to go back for more data, and the iterable does not have a
            // reference for doing so.
            parameter("array", ParameterType.Iterable, display, refToSelf) {
                parameter("[0]", ParameterType.String, "a")
                parameter("[2]", ParameterType.String, "b", index = 2)
                parameter("[3]", ParameterType.String, "c", index = 3)
                parameter("[8]", ParameterType.String, "d", index = 8)
                parameter("[10]", ParameterType.String, "e", index = 10)
            }
        }

        // Call "expand" to retrieve more elements:
        validate(expand("array", value, refToSelf, 11, 5)!!) {
            // This time we reached the end of the array, and we do not get a reference to get more
            parameter("array", ParameterType.Iterable, display) {
                parameter("[16]", ParameterType.String, "f", index = 16)
                parameter("[18]", ParameterType.String, "g", index = 18)
            }
        }
    }

    @Test
    fun testModifier() {
        validate(
            create(
                "modifier",
                Modifier
                    .background(Color.Blue)
                    .border(width = 5.dp, color = Color.Red)
                    .padding(2.0.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom)
                    .width(30.0.dp)
                    .paint(TestPainter(10f, 20f)),
                maxRecursions = 4
            )
        ) {
            parameter("modifier", ParameterType.String, "") {
                parameter("background", ParameterType.Color, Color.Blue.toArgb()) {
                    parameter("color", ParameterType.Color, Color.Blue.toArgb())
                    parameter("shape", ParameterType.String, "RectangleShape")
                }
                parameter("border", ParameterType.Color, Color.Red.toArgb()) {
                    parameter("width", ParameterType.DimensionDp, 5.0f)
                    parameter("color", ParameterType.Color, Color.Red.toArgb())
                    parameter("shape", ParameterType.String, "RectangleShape")
                }
                parameter("padding", ParameterType.DimensionDp, 2.0f)
                parameter("fillMaxWidth", ParameterType.String, "") {
                    parameter("fraction", ParameterType.Float, 1.0f)
                }
                parameter("wrapContentHeight", ParameterType.String, "") {
                    parameter("align", ParameterType.String, "Bottom")
                    parameter("unbounded", ParameterType.Boolean, false)
                }
                parameter("width", ParameterType.DimensionDp, 30.0f)
                parameter("paint", ParameterType.String, "") {
                    parameter("painter", ParameterType.String, "TestPainter") {
                        parameter("color", ParameterType.Color, Color.Red.toArgb())
                        parameter("height", ParameterType.Float, 20.0f)
                        parameter("intrinsicSize", ParameterType.String, "Size") {
                            parameter("height", ParameterType.Float, 20.0f)
                            parameter("maxDimension", ParameterType.Float, 20.0f)
                            parameter("minDimension", ParameterType.Float, 10.0f)
                            parameter("packedValue", ParameterType.Int64, 4692750812821061632L)
                            parameter("width", ParameterType.Float, 10.0f)
                        }
                        parameter("width", ParameterType.Float, 10.0f)
                        parameter("alpha", ParameterType.Float, 1.0f)
                        parameter("drawLambda", ParameterType.Lambda, null, index = 6)
                        parameter("layoutDirection", ParameterType.String, "Ltr", index = 8)
                        parameter("useLayer", ParameterType.Boolean, false, index = 9)
                    }
                    parameter("sizeToIntrinsics", ParameterType.Boolean, true)
                    parameter("alignment", ParameterType.String, "Center")
                    parameter("contentScale", ParameterType.String, "Inside")
                    parameter("alpha", ParameterType.Float, 1.0f)
                }
            }
        }
    }

    @Test
    fun testSingleModifier() {
        validate(create("modifier", Modifier.padding(2.0.dp))) {
            parameter("modifier", ParameterType.String, "") {
                parameter("padding", ParameterType.DimensionDp, 2.0f)
            }
        }
    }

    @Test
    fun testWrappedModifier() {
        fun Modifier.frame(color: Color) = inspectable(
            debugInspectorInfo {
                name = "frame"
                value = color
            }
        ) {
            background(color).border(width = 5.dp, color = color)
        }
        validate(create("modifier", Modifier.width(40.dp).frame(Color.Green).height(50.dp))) {
            parameter("modifier", ParameterType.String, "") {
                parameter("width", ParameterType.DimensionDp, 40.0f)
                parameter("frame", ParameterType.Color, Color.Green.toArgb())
                parameter("height", ParameterType.DimensionDp, 50.0f)
            }
        }
    }

    @Test
    fun testSingleModifierWithParameters() {
        validate(create("modifier", Modifier.padding(1.dp, 2.dp, 3.dp, 4.dp))) {
            parameter("modifier", ParameterType.String, "") {
                parameter("padding", ParameterType.String, "") {
                    parameter("start", ParameterType.DimensionDp, 1.0f)
                    parameter("top", ParameterType.DimensionDp, 2.0f)
                    parameter("end", ParameterType.DimensionDp, 3.0f)
                    parameter("bottom", ParameterType.DimensionDp, 4.0f)
                }
            }
        }
    }

    @Test
    fun testOffset() {
        validate(create("offset", Offset(1.0f, 5.0f))) {
            parameter("offset", ParameterType.String, Offset::class.java.simpleName) {
                parameter("x", ParameterType.DimensionDp, 0.5f)
                parameter("y", ParameterType.DimensionDp, 2.5f)
            }
        }
        validate(create("offset", Offset.Zero)) {
            parameter("offset", ParameterType.String, "Zero")
        }
    }

    @Test
    fun testRecursiveStructure() {
        val v1 = MyClass("v1")
        val v2 = MyClass("v2")
        v1.other = v2
        v2.other = v1
        v1.self = v1
        v2.self = v2
        val name = MyClass::class.java.simpleName
        validate(create("mine", v1)) {
            parameter("mine", ParameterType.String, name) {
                parameter("name", ParameterType.String, "v1")
                parameter("other", ParameterType.String, name) {
                    parameter("name", ParameterType.String, "v2")
                    // v2.other is expected to reference v1 which is already found
                    parameter("other", ParameterType.String, name, ref())

                    // v2.self is expected to reference v2 which is already found
                    parameter("self", ParameterType.String, name, ref(1))
                }
                // v1.self is expected to reference v1 which is already found
                parameter("self", ParameterType.String, name, ref())
            }
        }
    }

    @Test
    fun testMissingChildParameters() {
        val v1 = MyClass("v1")
        val v2 = MyClass("v2")
        val v3 = MyClass("v3")
        val v4 = MyClass("v4")
        val v5 = MyClass("v5")
        v1.self = v1
        v1.third = v2
        v2.other = v3
        v2.third = v1
        v3.other = v4
        v4.other = v5
        val name = MyClass::class.java.simpleName

        // Limit the recursions for this test to validate parameter nodes with missing children.
        val parameter = create("v1", v1, maxRecursions = 2)
        val v2ref = ref(3, 1)
        validate(parameter) {
            parameter("v1", ParameterType.String, name) {
                parameter("name", ParameterType.String, "v1")
                parameter("self", ParameterType.String, name, ref(), index = 2)
                parameter("third", ParameterType.String, name, index = 3) {
                    parameter("name", ParameterType.String, "v2")

                    // Expect the child elements for v2 to be missing from the parameter tree,
                    // which is indicated by the reference field being included for "other" here:
                    parameter("other", ParameterType.String, name, v2ref)
                    parameter("third", ParameterType.String, name, ref(), index = 3)
                }
            }
        }

        // If we need to retrieve the missing child nodes for v2 from above, we must
        // call "expand" with the reference:
        val v4ref = ref(3, 1, 1, 1)
        validate(expand("v1", v1, v2ref)!!) {
            parameter("other", ParameterType.String, name) {
                parameter("name", ParameterType.String, "v3")
                parameter("other", ParameterType.String, name) {
                    parameter("name", ParameterType.String, "v4")

                    // Expect the child elements for v4 to be missing from the parameter tree,
                    // which is indicated by the reference field being included for "other" here:
                    parameter("other", ParameterType.String, name, v4ref)
                }
            }
        }

        // If we need to retrieve the missing child nodes for v4 from above, we must
        // call "expand" with the reference:
        validate(expand("v1", v1, v4ref)!!) {
            parameter("other", ParameterType.String, name) {
                parameter("name", ParameterType.String, "v5")
            }
        }
    }

    @Test
    fun testDoNotRecurseInto() {
        runBlocking {
            assertThat(lookup(java.net.URL("http://domain.com")))
                .isEqualTo(ParameterType.String to "")
            assertThat(lookup(android.app.Notification()))
                .isEqualTo(ParameterType.String to "")
        }
    }

    @Test
    fun testShadow() {
        assertThat(lookup(Shadow.None)).isEqualTo(ParameterType.String to "None")
        validate(create("shadow", Shadow(Color.Cyan, Offset.Zero, 2.5f))) {
            parameter("shadow", ParameterType.String, Shadow::class.java.simpleName) {
                parameter("blurRadius", ParameterType.DimensionDp, 1.25f)
                parameter("color", ParameterType.Color, Color.Cyan.toArgb())
                parameter("offset", ParameterType.String, "Zero")
            }
        }
        validate(create("shadow", Shadow(Color.Blue, Offset(1.0f, 4.0f), 1.5f))) {
            parameter("shadow", ParameterType.String, Shadow::class.java.simpleName) {
                parameter("blurRadius", ParameterType.DimensionDp, 0.75f)
                parameter("color", ParameterType.Color, Color.Blue.toArgb())
                parameter("offset", ParameterType.String, Offset::class.java.simpleName) {
                    parameter("x", ParameterType.DimensionDp, 0.5f)
                    parameter("y", ParameterType.DimensionDp, 2.0f)
                }
            }
        }
    }

    @Test
    fun testShape() {
        assertThat(lookup(RectangleShape)).isEqualTo(ParameterType.String to "RectangleShape")
    }

    @Test
    fun testString() {
        assertThat(lookup("Hello")).isEqualTo(ParameterType.String to "Hello")
    }

    @Test
    fun testTextDecoration() {
        assertThat(lookup(TextDecoration.None)).isEqualTo(ParameterType.String to "None")
        assertThat(lookup(TextDecoration.LineThrough))
            .isEqualTo(ParameterType.String to "LineThrough")
        assertThat(lookup(TextDecoration.Underline))
            .isEqualTo(ParameterType.String to "Underline")
        assertThat(lookup(TextDecoration.LineThrough + TextDecoration.Underline))
            .isEqualTo(ParameterType.String to "LineThrough+Underline")
    }

    @Test
    fun testTextGeometricTransform() {
        validate(create("transform", TextGeometricTransform(2.0f, 1.5f))) {
            parameter(
                "transform", ParameterType.String,
                TextGeometricTransform::class.java.simpleName
            ) {
                parameter("scaleX", ParameterType.Float, 2.0f)
                parameter("skewX", ParameterType.Float, 1.5f)
            }
        }
    }

    @Test
    fun testTextIndent() {
        assertThat(lookup(TextIndent.None)).isEqualTo(ParameterType.String to "None")

        validate(create("textIndent", TextIndent(4.0.sp, 0.5.sp))) {
            parameter("textIndent", ParameterType.String, "TextIndent") {
                parameter("firstLine", ParameterType.DimensionSp, 4.0f)
                parameter("restLine", ParameterType.DimensionSp, 0.5f)
            }
        }
    }

    @Test
    fun testTextStyle() {
        val style = TextStyle(
            color = Color.Red,
            textDecoration = TextDecoration.Underline
        )
        validate(create("style", style)) {
            parameter("style", ParameterType.String, TextStyle::class.java.simpleName) {
                parameter("color", ParameterType.Color, Color.Red.toArgb())
                parameter("fontSize", ParameterType.String, "Unspecified", index = 1)
                parameter("letterSpacing", ParameterType.String, "Unspecified", index = 7)
                parameter("background", ParameterType.String, "Unspecified", index = 11)
                parameter("textDecoration", ParameterType.String, "Underline", index = 12)
                parameter("lineHeight", ParameterType.String, "Unspecified", index = 15)
            }
        }
    }

    @Test
    fun testTextUnit() {
        assertThat(lookup(TextUnit.Unspecified)).isEqualTo(ParameterType.String to "Unspecified")
        assertThat(lookup(12.0.sp)).isEqualTo(ParameterType.DimensionSp to 12.0f)
        assertThat(lookup(2.0.em)).isEqualTo(ParameterType.DimensionEm to 2.0f)
        assertThat(lookup(9.0f.sp)).isEqualTo(ParameterType.DimensionSp to 9.0f)
        assertThat(lookup(10.sp)).isEqualTo(ParameterType.DimensionSp to 10.0f)
        assertThat(lookup(26.0.sp)).isEqualTo(ParameterType.DimensionSp to 26.0f)
        assertThat(lookup(2.0f.em)).isEqualTo(ParameterType.DimensionEm to 2.0f)
        assertThat(lookup(1.em)).isEqualTo(ParameterType.DimensionEm to 1.0f)
        assertThat(lookup(3.0.em)).isEqualTo(ParameterType.DimensionEm to 3.0f)
    }

    @Test
    fun testVectorAssert() {
        assertThat(lookup(Icons.Filled.Call)).isEqualTo(ParameterType.String to "Filled.Call")
        assertThat(lookup(Icons.Rounded.Add)).isEqualTo(ParameterType.String to "Rounded.Add")
    }

    private fun create(
        name: String,
        value: Any,
        maxRecursions: Int = MAX_RECURSIONS,
        maxInitialIterableSize: Int = MAX_ITERABLE_SIZE
    ): NodeParameter {
        val parameter = factory.create(
            ROOT_ID,
            NODE_ID,
            ANCHOR_HASH,
            name,
            value,
            ParameterKind.Normal,
            PARAM_INDEX,
            maxRecursions,
            maxInitialIterableSize
        )

        // Check that factory.expand will return the exact same information as factory.create
        // for each parameter and parameter child. Punt if there are references.
        checkExpand(
            parameter,
            parameter.name,
            value,
            mutableListOf(),
            maxRecursions,
            maxInitialIterableSize
        )

        return parameter
    }

    private fun expand(
        name: String,
        value: Any?,
        reference: NodeParameterReference,
        startIndex: Int = 0,
        maxElements: Int = MAX_ITERABLE_SIZE,
        maxRecursions: Int = MAX_RECURSIONS,
        maxInitialIterableSize: Int = MAX_ITERABLE_SIZE
    ): NodeParameter? =
        factory.expand(
            ROOT_ID,
            NODE_ID,
            ANCHOR_HASH,
            name,
            value,
            reference,
            startIndex,
            maxElements,
            maxRecursions,
            maxInitialIterableSize
        )

    private fun lookup(value: Any): Pair<ParameterType, Any?> {
        val parameter = create("parameter", value)
        assertThat(parameter.elements).isEmpty()
        return Pair(parameter.type, parameter.value)
    }

    private fun ref(vararg reference: Int): NodeParameterReference =
        NodeParameterReference(NODE_ID, ANCHOR_HASH, ParameterKind.Normal, PARAM_INDEX, reference)

    private fun validate(
        parameter: NodeParameter,
        expected: ParameterValidationReceiver.() -> Unit = {}
    ) {
        val elements = ParameterValidationReceiver(listOf(parameter).listIterator())
        elements.expected()
        elements.checkFinished()
    }

    private fun checkExpand(
        parameter: NodeParameter,
        name: String,
        value: Any,
        indices: MutableList<Int>,
        maxRecursions: Int,
        maxInitialIterableSize: Int
    ) {
        factory.clearReferenceCache()
        val reference =
            NodeParameterReference(NODE_ID, ANCHOR_HASH, ParameterKind.Normal, PARAM_INDEX, indices)
        val expanded = expand(
            name,
            value,
            reference,
            maxRecursions = maxRecursions,
            maxInitialIterableSize = maxInitialIterableSize
        )
        if (parameter.value == null && indices.isNotEmpty()) {
            assertThat(expanded).isNull()
        } else {
            val hasReferences = expanded!!.checkEquals(parameter)
            if (!hasReferences) {
                parameter.elements.forEach { element ->
                    if (element.index >= 0) {
                        indices.add(element.index)
                        checkExpand(
                            element,
                            name,
                            value,
                            indices,
                            maxRecursions,
                            maxInitialIterableSize
                        )
                        indices.removeLast()
                    }
                }
            }
        }
    }
}

private class TestPainter(
    val width: Float,
    val height: Float
) : Painter() {

    var color = Color.Red

    override val intrinsicSize: Size
        get() = Size(width, height)

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        color = if (layoutDirection == LayoutDirection.Rtl) Color.Blue else Color.Red
        return true
    }

    override fun DrawScope.onDraw() {
        drawRect(color = color)
    }
}

class ParameterValidationReceiver(
    private val parameterIterator: ListIterator<NodeParameter>,
    private val trace: String = "",
    private val startIndex: Int = 0
) {
    fun parameter(
        name: String,
        type: ParameterType,
        value: Any?,
        ref: NodeParameterReference? = null,
        index: Int = -1,
        childStartIndex: Int = 0,
        block: ParameterValidationReceiver.() -> Unit = {}
    ) {
        val listIndex = startIndex + parameterIterator.nextIndex()
        val expectedIndex = if (index < 0) listIndex else index
        assertWithMessage("No such element found: $name").that(parameterIterator.hasNext()).isTrue()
        val parameter = parameterIterator.next()
        assertThat(parameter.name).isEqualTo(name)
        val msg = "$trace${parameter.name}"
        assertWithMessage(msg).that(parameter.type).isEqualTo(type)
        assertWithMessage(msg).that(parameter.index).isEqualTo(expectedIndex)
        assertWithMessage(msg).that(parameter.reference.toString()).isEqualTo(ref.toString())
        if (type != ParameterType.Lambda || value != null) {
            assertWithMessage(msg).that(parameter.value).isEqualTo(value)
        }
        val iterator = parameter.elements.listIterator()
        ParameterValidationReceiver(iterator, "$msg.", childStartIndex).apply {
            block()
            checkFinished(msg)
        }
    }

    fun checkFinished(trace: String = "") {
        if (parameterIterator.hasNext()) {
            val elementNames = mutableListOf<String>()
            while (parameterIterator.hasNext()) {
                elementNames.add(parameterIterator.next().name)
            }
            error("$trace: has more elements like: ${elementNames.joinToString()}")
        }
    }
}

@Suppress("unused")
class MyClass(private val name: String) {
    var other: MyClass? = null
    var self: MyClass? = null
    var third: MyClass? = null

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = name == (other as? MyClass)?.name
}

private fun NodeParameter.checkEquals(other: NodeParameter): Boolean {
    assertThat(other.name).isEqualTo(name)
    assertThat(other.type).isEqualTo(type)
    assertThat(other.value).isEqualTo(value)
    assertThat(other.reference.toString()).isEqualTo(reference.toString())
    assertThat(other.elements.size).isEqualTo(elements.size)
    var hasReferences = reference != null
    elements.forEachIndexed { i, element ->
        hasReferences = hasReferences or element.checkEquals(other.elements[i])
    }
    return hasReferences
}
