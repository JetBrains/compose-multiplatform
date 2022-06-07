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

package androidx.compose.foundation.demos.text

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Preview
@Composable
fun FontFamilyDemo() {
    LazyColumn {
        item {
            TagLine(tag = "Async font loading")
            AsyncFontFamilyDemo()
        }
    }
}

@Preview
@OptIn(ExperimentalTextApi::class)
@Composable
fun AsyncFontFamilyDemo() {

    var recreateFontFamily by remember { mutableStateOf(0) }
    var showW800 by remember {
        mutableStateOf(false)
    }

    // define three font fallback chains
    // 1. (Normal, Normal) has an optional font and two async fonts
    // 2. (W200, Normal) has an async font
    // 3. (W800, Normal) has an async font, followed by an optional font that fails to load
    // During loading, font chains 2 and 3 fall off the end and use the platform default font
    // During loading, font chain 1 uses a blocking font as a fallback

    // this would typically be defined outside of composition, but we're doing it here to allow font
    // loading to restart. The DemoFonts above do not specify equals or  hashcode, so new instances
    // are never equal for caching.
    val fontFamily = remember(recreateFontFamily) {
        FontFamily(
            // first font fails to load but is optional
            Font(
                DeviceFontFamilyName("A font that is not installed, on any device"),
                FontWeight.Normal,
                FontStyle.Normal
            ),
            // second font loads with a delay, is fallback for (Normal, Normal)
            DemoAsyncFont(
                FontWeight.Normal,
                FontStyle.Normal,
                delay = 2_000L,
                typeface = Typeface.create("cursive", Typeface.NORMAL)
            ),
            // third (Normal, Normal) font is never matched, as previous font correctly loads, if
            // previous font failed to load it would match next for (Normal, Normal)
            DemoAsyncFont(FontWeight.Normal, FontStyle.Normal, delay = 500L),
            // this is the fallback used during loading for (Normal, Normal)
            DemoBlockingFont(FontWeight.Normal, FontStyle.Normal, Typeface.SERIF),

            // This font matches (W200, Normal), and will load on first use
            DemoAsyncFont(
                FontWeight.W200,
                FontStyle.Normal,
                delay = 500L,
                Typeface.create("cursive", Typeface.NORMAL)
            ),

            // This font matches (W800, Normal), and will load on first use
            DemoAsyncFont(
                FontWeight.W800,
                FontStyle.Normal,
                delay = 500L,
                typeface = Typeface.create("cursive", Typeface.BOLD)
            ),
            // Fallback for (W800, Normal)
            Font(
                DeviceFontFamilyName("A font that is not installed, on any device"),
                FontWeight.W800,
                FontStyle.Normal
            )
        )
    }
    Column {
        Text("This demo will load the fonts using fallback chains. Demo fonts descriptions" +
            " are defined to not cache between loads of this screen, but typically will cache in" +
            " production usage.",
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "(Normal, Normal) text with async loading with fallback (2000ms)",
            fontFamily = fontFamily,
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        Text(
            "(W200, Normal) text async loading, same FontFamily (500ms)",
            fontFamily = fontFamily,
            fontWeight = FontWeight.W200
        )
        if (showW800) {
            // font for W800 won't load until it's displayed the first time
            Text(
                "(W800, Normal) text async loading with fallback, same FontFamily " +
                    "(500ms from first display)",
                fontFamily = fontFamily,
                fontWeight = FontWeight.W800
            )
        }

        TextField(
            value = "(W200, Normal) 500ms",
            onValueChange = {},
            textStyle = TextStyle.Default.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.W200
            )
        )
        Button(onClick = {
            showW800 = !showW800
        }) {
            Text("Toggle W800 text")
        }

        Button(onClick = {
            recreateFontFamily++
        }) {
            Text("Restart font loading")
        }
    }
}

// To avoid using real network-based fonts, declare some Demo- font classes to implement FontFamily
// demos

// example of defining custom font typeface resolver for use in a FontFamily
// this is typically done to add _new_ types of font resources to Compose
object ExampleAsyncFontTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        return when (font) {
            is DemoOptionalFont -> null // all optional fonts fail
            is DemoBlockingFont -> font.typeface
            is DemoAsyncFont -> throw IllegalStateException("Async fonts don't load via blocking")
            else -> throw IllegalStateException("Unsupported font type")
        }
    }

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont
    ): Typeface {
        // delayed fonts take the specified delay
        font as DemoAsyncFont
        delay(font.delay)
        return font.typeface
    }
}

class DemoAsyncFont(
    override val weight: FontWeight,
    override val style: FontStyle,
    val delay: Long,
    val typeface: Typeface = Typeface.MONOSPACE
) : AndroidFont(Async, ExampleAsyncFontTypefaceLoader)

@OptIn(ExperimentalTextApi::class)
class DemoOptionalFont(
    override val weight: FontWeight,
    override val style: FontStyle,
) : AndroidFont(OptionalLocal, ExampleAsyncFontTypefaceLoader)

@OptIn(ExperimentalTextApi::class)
class DemoBlockingFont(
    override val weight: FontWeight,
    override val style: FontStyle,
    val typeface: Typeface
) : AndroidFont(OptionalLocal, ExampleAsyncFontTypefaceLoader)
