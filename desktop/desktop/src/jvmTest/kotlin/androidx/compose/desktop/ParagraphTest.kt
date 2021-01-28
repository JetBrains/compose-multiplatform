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

package androidx.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("ui/ui-desktop/paragraph")

    private val text1 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
            " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
            " minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip" +
            " ex ea commodo consequat. Duis aute irure dolor in reprehenderit in" +
            " voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur" +
            " sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt" +
            " mollit anim id est laborum."

    private val text2 =
        "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = when {\n" +
            "  size < 2 -> this\n" +
            "  else -> {\n" +
            "    val pivot = first()\n" +
            "    val (smaller, greater) = drop(1).partition { it <= pivot }\n" +
            "    smaller.quickSort() + pivot + greater.quickSort()\n" +
            "   }\n" +
            "}"

    val fontFamily = FontFamily(
        Font("NotoSans-Regular.ttf"),
        Font("NotoSans-Italic.ttf", style = FontStyle.Italic)
    )

    // TODO(b/168298835)
    @Ignore
    @Test
    fun paragraphBasics() {
        val window = TestComposeWindow(width = 1024, height = 768)

        window.setContent {
            ProvideTextStyle(TextStyle(fontFamily = fontFamily)) {
                Column(Modifier.fillMaxSize().background(Color.White), Arrangement.SpaceEvenly) {
                    Text(
                        text = "Привет! 你好! Desktop Compose",
                        color = Color.Black,
                        modifier = Modifier
                            .background(Color.Blue)
                            .preferredHeight(56.dp)
                            .wrapContentSize(Alignment.Center)
                    )

                    Text(
                        text = with(AnnotatedString.Builder("The quick ")) {
                            pushStyle(SpanStyle(color = Color(0xff964B00)))
                            append("brown fox")
                            pop()
                            append(" 🦊 ate a ")
                            pushStyle(SpanStyle(fontSize = 30.sp))
                            append("zesty hamburgerfons")
                            pop()
                            append(" 🍔.\nThe 👩‍👩‍👧‍👧 laughed.")
                            addStyle(SpanStyle(color = Color.Green), 25, 35)
                            toAnnotatedString()
                        },
                        color = Color.Black
                    )

                    Text(
                        text = text1
                    )

                    Text(
                        text = text2,
                        modifier = Modifier.padding(10.dp),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        screenshotRule.snap(window.surface)
    }
}