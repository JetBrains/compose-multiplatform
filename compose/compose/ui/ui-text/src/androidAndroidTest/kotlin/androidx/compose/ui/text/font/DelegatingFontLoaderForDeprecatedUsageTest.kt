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

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalTextApi::class)
class DelegatingFontLoaderForDeprecatedUsageTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    @Suppress("DEPRECATION")
    fun cacheKeysAreUnique() {
        val mock = mock<Font.ResourceLoader>()

        val bridge1 = createFontFamilyResolver(mock, context)
        val bridge2 = createFontFamilyResolver(mock, context)

        val loader = AsyncTestTypefaceLoader()
        val font = BlockingFauxFont(loader, Typeface.SERIF)
        val fontFamily = FontFamily(font)

        bridge1.resolve(fontFamily)
        assertThat(loader.blockingRequests).hasSize(1)

        bridge2.resolve(fontFamily)
        assertThat(loader.blockingRequests).hasSize(2)
    }

    @Test
    @Suppress("DEPRECATION")
    fun cacheKeysAreStable_onSameInstance() {
        val mock = mock<Font.ResourceLoader>()

        val bridge = createFontFamilyResolver(mock, context)

        val loader = AsyncTestTypefaceLoader()
        val font = BlockingFauxFont(loader, Typeface.SERIF)
        val fontFamily = FontFamily(font)

        bridge.resolve(fontFamily)
        assertThat(loader.blockingRequests).hasSize(1)

        bridge.resolve(fontFamily)
        assertThat(loader.blockingRequests).hasSize(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadBlocking_delegatesToAndroidFont() {
        val mock = mock<Font.ResourceLoader>()

        val bridge = createFontFamilyResolver(mock, context)

        val loader = AsyncTestTypefaceLoader()
        val font = BlockingFauxFont(loader, Typeface.SERIF)
        val fontFamily = FontFamily(font)

        bridge.resolve(fontFamily)
        assertThat(loader.blockingRequests).hasSize(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadOptional_delegatesToAndroidFont() {
        val mock = mock<Font.ResourceLoader>()

        val bridge = createFontFamilyResolver(mock, context)

        val loader = AsyncTestTypefaceLoader()
        val font = OptionalFauxFont(loader, Typeface.SERIF)
        val fontFamily = FontFamily(font)

        bridge.resolve(fontFamily)
        assertThat(loader.optionalRequests).hasSize(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadAsync_delegatesToAndroidFont() {
        val mock = mock<Font.ResourceLoader>()

        val bridge = createFontFamilyResolver(mock, context)

        val loader = AsyncTestTypefaceLoader()
        val font = AsyncFauxFont(loader)
        val fontFamily = FontFamily(font)

        bridge.resolve(fontFamily)
        loader.completeOne(font, Typeface.SERIF)
        assertThat(loader.completedAsyncRequests).hasSize(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadBlocking_delegatesToFontResourceLoader() {
        val resourceFont = Font(/* resId */ 3)
        val subject = TrackingLoader()
        val bridge = createFontFamilyResolver(subject, context)

        bridge.resolve(resourceFont.toFontFamily())
        assertThat(subject.loads).containsExactly(resourceFont)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadOptional_delegatesToFontResourceLoader() {
        val resourceFont = Font(
            resId = 3,
            loadingStrategy = FontLoadingStrategy.OptionalLocal
        )
        val subject = TrackingLoader()
        val bridge = createFontFamilyResolver(subject, context)

        bridge.resolve(resourceFont.toFontFamily())
        assertThat(subject.loads).containsExactly(resourceFont)
    }

    @Test
    @Suppress("DEPRECATION")
    fun loadAsync_delegatesToFontResourceLoader() {
        val resourceFont = Font(
            resId = 3,
            loadingStrategy = FontLoadingStrategy.Async
        )
        val subject = TrackingLoader()
        val bridge = createFontFamilyResolver(subject, context)

        bridge.resolve(resourceFont.toFontFamily())
        assertThat(subject.loads).containsExactly(resourceFont)
    }

    @Suppress("DEPRECATION")
    private class TrackingLoader : Font.ResourceLoader {
        val loads = mutableListOf<Font>()

        @Deprecated(
            "Replaced by FontFamily.Resolver, this method should not be called",
            replaceWith = ReplaceWith("FontFamily.Resolver.resolve(font, )")
        )
        @Suppress("DEPRECATION")
        override fun load(font: Font): Any {
            loads.add(font)
            return Typeface.SERIF
        }
    }
}