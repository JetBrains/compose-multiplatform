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

package androidx.compose.ui.text

import android.graphics.Typeface
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.AndroidParagraphIntrinsics
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntrinsicsAsyncTypefaceTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun hasStaleResolvedFonts_falseByDefault() {
        val subject = paragraphIntrinsics(
            "Text",
            createFontFamilyResolver(context),
            null
        )
        Snapshot.withMutableSnapshot {
            assertThat(subject.hasStaleResolvedFonts).isFalse()
        }
    }

    @OptIn(ExperimentalTextApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun hasStaleResolvedFonts_trueOnTypefaceUpdate_mainTypeface() {
        val loader = AsyncTestTypefaceLoader()
        val asyncFauxFont = AsyncFauxFont(loader)
        val fontFamily = asyncFauxFont.toFontFamily()

        runTest(UnconfinedTestDispatcher()) {
            val resolverJob = Job(coroutineContext[Job])
            val resolverContext = coroutineContext + resolverJob
            val fontFamilyResolver = createFontFamilyResolver(context, resolverContext)
            val subject = paragraphIntrinsics(
                "Text",
                fontFamilyResolver,
                fontFamily
            )

            val result = async {
                snapshotFlow { subject.hasStaleResolvedFonts }
                    .take(2)
                    .toList()
            }

            Snapshot.withMutableSnapshot {
                loader.completeOne(asyncFauxFont, Typeface.MONOSPACE)
            }

            assertThat(result.await()).isEqualTo(listOf(false, true))
            resolverJob.cancel()
        }
    }

    @OptIn(ExperimentalTextApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun hasStaleResolvedFonts_trueOnTypefaceUpdate_spanTypeface() {
        val loader = AsyncTestTypefaceLoader()
        val asyncFauxFont = AsyncFauxFont(loader)
        val fontFamily = asyncFauxFont.toFontFamily()

        runTest(UnconfinedTestDispatcher()) {
            val resolverJob = Job(coroutineContext[Job])
            val resolverContext = coroutineContext + resolverJob
            val fontFamilyResolver = createFontFamilyResolver(context, resolverContext)

            val spanStyle = SpanStyle(fontFamily = fontFamily)
            val styles = listOf(AnnotatedString.Range(spanStyle, 0, 1))
            val subject = paragraphIntrinsics(
                "Text",
                fontFamilyResolver,
                null,
                styles
            )

            val result = async {
                snapshotFlow { subject.hasStaleResolvedFonts }
                    .take(2)
                    .toList()
            }

            Snapshot.withMutableSnapshot {
                loader.completeOne(asyncFauxFont, Typeface.MONOSPACE)
            }

            assertThat(result.await()).isEqualTo(listOf(false, true))
            resolverJob.cancel()
        }
    }

    private fun paragraphIntrinsics(
        text: String,
        fontFamilyResolver: FontFamily.Resolver,
        fontFamily: FontFamily?,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf()
    ): AndroidParagraphIntrinsics {
        return AndroidParagraphIntrinsics(
            text,
            TextStyle.Default.copy(
                fontFamily = fontFamily,
            ),
            spanStyles,
            listOf(),
            fontFamilyResolver,
            Density(1f)
        )
    }
}
