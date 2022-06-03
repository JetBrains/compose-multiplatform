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

package androidx.compose.foundation.text

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.AndroidFont.TypefaceLoader
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextControllerTest {
    @get:Rule
    val rule = createComposeRule()

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @OptIn(ExperimentalCoroutinesApi::class, InternalFoundationTextApi::class)
    @Test
    fun asyncTextResolution_causesRedraw() {
        val loadDeferred = CompletableDeferred<Unit>()
        val drawChannel = Channel<Unit>(capacity = Channel.UNLIMITED)
        val drawCount = AtomicInteger(0)
        val textDelegate = makeTextDelegate(loadDeferred)

        val subject = TextController(TextState(textDelegate, 17L))

        val modifiers = Modifier.fillMaxSize() then subject.modifiers then Modifier.drawBehind {
            drawCount.incrementAndGet()
            drawChannel.trySend(Unit)
        }

        rule.setContent {
            Layout(modifiers, subject.measurePolicy)
        }
        rule.waitForIdle()
        runBlocking {
            // empty the draw channel here. sentContent already ensured that draw ran.
            // we just need this for sequencing AtomicInteger read/write/read later
            while (!drawChannel.isEmpty) {
                drawChannel.receive()
            }
        }
        val initialCount = drawCount.get()
        assertThat(initialCount).isGreaterThan(0)

        // this may take a while to make compose non-idle, so wait for drawChannel explicit sync
        loadDeferred.complete(Unit)
        runBlocking { drawChannel.receive() }
        rule.waitForIdle()

        assertThat(drawCount.get()).isGreaterThan(initialCount)
    }

    @OptIn(InternalFoundationTextApi::class)
    private fun makeTextDelegate(onFontFinishedLoad: CompletableDeferred<Unit>): TextDelegate {
        val typefaceLoader = object : TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                TODO("Not yet implemented")
            }

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                onFontFinishedLoad.await()
                return Typeface.create("cursive", 0)
            }
        }
        val asyncFont = object : AndroidFont(FontLoadingStrategy.Async, typefaceLoader) {
            override val weight: FontWeight
                get() = FontWeight.Normal
            override val style: FontStyle
                get() = FontStyle.Normal
        }

        return TextDelegate(
            AnnotatedString("til"),
            TextStyle.Default.copy(fontFamily = asyncFont.toFontFamily()),
            density = Density(1f, 1f),
            fontFamilyResolver = createFontFamilyResolver(context)
        )
    }
}