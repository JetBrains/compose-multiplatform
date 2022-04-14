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

package androidx.compose.ui.text.font

/**
 * Font loading strategy for a [Font] in a [FontListFontFamily].
 *
 * This controls how font loading resolves when displaying text in Compose.
 *
 * For more information about font family resolution see [FontFamily].
 */
@kotlin.jvm.JvmInline
value class FontLoadingStrategy private constructor(val value: Int) {
    override fun toString(): String {
        return when (this) {
            Blocking -> "Blocking"
            OptionalLocal -> "Optional"
            Async -> "Async"
            else -> "Invalid(value=$value)"
        }
    }

    companion object {
        /**
         * Resolving this font will always block until the font loads.
         *
         * This means that the first frame that uses this font will always display using the desired
         * font, and text will never reflow.
         *
         * This should typically be used for fonts that stored on-device. It is acceptable to block
         * the first frame of an application to read a font from disk.
         *
         * This should typically not be used for fonts that are fetched from a remote source such as
         * over http, as it will block all rendering until the font loads. Instead use [Async].
         */
        val Blocking = FontLoadingStrategy(0)

        /**
         * Resolving this font is best-effort and will attempt to load from a local resource that
         * MAY be available.
         *
         * Resolving this font will always block until the font resolves, and text will never
         * reflow.
         *
         * An [OptionalLocal] font describes a font that is installed locally, such as an optional
         * system installed font. Resolution of an [OptionalLocal] font is expected to fail whenever
         * the resource is not available, which will fallback to the next font in the [FontFamily].
         *
         * Typical usages involve following the [OptionalLocal] font with the same font loaded from
         * a remote [Async] source. It may also be followed by other [OptionalLocal] fonts or
         * [Blocking] fonts that have less specific styling to avoid async delay.
         *
         * Apps should expect that [OptionalLocal] fonts will fail to load on devices where the
         * font is not available.
         **
         * This should typically not be used for fonts that are fetched from a remote source such as
         * over http, as it will block all rendering until the font loads. Instead use [Async].
         */
        val OptionalLocal = FontLoadingStrategy(1)

        /**
         * Loading this font will never block, and will load on a background thread.
         *
         * During loading, the app will not block but instead will find the next available font that
         * is immediately available ([Blocking] or [OptionalLocal]). When the font finishes loading,
         * text will reflow with the resolved typeface.
         *
         * If no fallback fonts are available immediately, the platform default typeface will be
         * used to draw text during loading.
         *
         * If loading fails, the failure is stored permanently, and future uses of the font will
         * always use the fallback.
         *
         * This should typically not be used for fonts that are stored on-device and needed for the
         * first frame. Instead use [Blocking] or [OptionalLocal].
         *
         * This should always be used for fonts that are fetched from a remote source such as over
         * http.
         */
        val Async = FontLoadingStrategy(2)
    }
}
