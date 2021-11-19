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

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import java.io.File

/**
 * Create a Font declaration from a file in the assets directory. The content of the [File] is
 * read during construction.
 *
 * @param assetManager Android AssetManager
 * @param path full path starting from the assets directory (i.e. dir/myfont.ttf for
 * assets/dir/myfont.ttf).
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
fun Font(
    assetManager: AssetManager,
    path: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidAssetFont(assetManager, path, weight, style)

/**
 * Create a Font declaration from a file. The content of the [File] is read during construction.
 *
 * @param file the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
@Suppress("StreamFiles")
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidFileFont(file, weight, style)

/**
 * Create a Font declaration from a [ParcelFileDescriptor]. The content of the
 * [ParcelFileDescriptor] is read during construction.
 *
 * @param fileDescriptor the file descriptor for the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@RequiresApi(26)
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
fun Font(
    fileDescriptor: ParcelFileDescriptor,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidFileDescriptorFont(fileDescriptor, weight, style)

/**
 * Font for use on Android.
 *
 * All [AndroidFont] produce an [android.graphics.Typeface] which may be used to draw text on
 * Android. This is the main low-level API for introducing a new Font description to Compose on
 * Android for both blocking and async load.
 *
 * You may subclass this to add new types of font descriptions that may be used in
 * [FontListFontFamily]. For example, you can add a [FontLoadingStrategy.Blocking] font that
 * returns a Typeface from a local resource not supported by an existing [Font]. Or, you can create
 * an [FontLoadingStrategy.Async] font that loads a font file from your server.
 */
abstract class AndroidFont @OptIn(ExperimentalTextApi::class) constructor(
    final override val loadingStrategy: FontLoadingStrategy
) : Font {

    /**
     * A loader that knows how to load this [AndroidFont].
     *
     * This may be shared between several fonts.
     */
    abstract val typefaceLoader: TypefaceLoader

    /**
     * Loader for loading an [AndroidFont] and producing an [android.graphics.Typeface].
     *
     * This interface is not intended to be used by application developers for text display. To load
     * a typeface for display use [FontFamily.Companion.Resolver].
     *
     * [TypefaceLoader] allows the introduction of new types of font descriptors for use in
     * [FontListFontFamily]. A [TypefaceLoader] allows a new subclass of [AndroidFont] to be used by
     * normal compose text rendering methods.
     *
     * Examples of new types of fonts that [TypefaceLoader] can add:
     * - [FontLoadingStrategy.Blocking] [Font] that loads Typeface from a local resource not
     * supported by an existing font
     * - [FontLoadingStrategy.OptionalLocal] [Font] that "loads" a platform Typeface only available
     * on some devices.
     * - [FontLoadingStrategy.Async] [Font] that loads a font from a backend via a network request.
     *
     * During resolution from [FontFamily.Companion.Resolver], an [AndroidFont] subclass will be
     * queried for an appropriate loader by [Font.Companion.AndroidResourceLoader].
     *
     * The loader attached to an instance of an [AndroidFont] is only required to be able to load
     * that instance, though it is advised to create one loader for all instances of the same
     * subclass and share them between [AndroidFont] instances to avoid allocations or allow
     * caching.
     */
    interface TypefaceLoader {
        /**
         * Immediately load the font in a blocking manner such that it will be available in the next
         * frame.
         *
         * This is used to load [FontLoadingStrategy.Blocking] and
         * [FontLoadingStrategy.OptionalLocal] fonts which must resolve font loading immediately
         * with no fallback. This method is allowed to perform small amounts of I/O to load a font
         * file from disk.
         *
         * This method should never perform expensive I/O operations, such as loading from a remote
         * source. To fetch from a remote source, require [FontLoadingStrategy.Async] and provide
         * [loadAsync].
         *
         * Subclasses may choose to cache or precompute this result.
         *
         * This method may throw a [RuntimeException] if the font fails to load, though it is
         * preferred to return null if the font is [FontLoadingStrategy.OptionalLocal] for
         * performance.
         *
         * It is possible for [load] to be called for the same instance of [AndroidFont] in
         * parallel. Implementations should support parallel concurrent loads, or de-dup.
         *
         * @param context current Android context for loading the font
         * @param font the font to load which contains this loader as [AndroidFont.typefaceLoader]
         * @return [android.graphics.Typeface] for loaded font, or null if the font fails to load
         * @throws RuntimeException subclass may optionally be thrown if the font fails to load
         */
        fun load(context: Context, font: AndroidFont): Typeface?

        /**
         * Asynchronously load the font, from either local or remote sources.
         *
         * This will be called for [FontLoadingStrategy.Async] fonts during async fallback
         * resolution, and all loaders should ensure this method is implemented for all
         * [AndroidFont] subtypes that allow [FontLoadingStrategy.Async].
         *
         * This method is always called in a timeout context and must return it's final value within
         * 15 seconds. If the Typeface is not resolved within 15 seconds, the async load is
         * cancelled and considered a permanent failure and will not be retried by normal font
         * resolution.
         *
         * Subclasses may choose to cache this result.
         *
         * Subclasses should not precompute this result (load the font prior to first the first call
         * of [loadAsync]).
         *
         * Compose does not know what resources are required to satisfy a font async load.
         * Subclasses implementing [FontLoadingStrategy.Async] behavior should ensure requests are
         * de-duped for the same resource.
         *
         * It is possible for the same [AndroidFont] to call [loadAsync] in parallel, and
         * implementations should either de-dupe or handle multithreaded execution.
         *
         * @param context current Android context for loading the font
         * @param font the font to load which contains this loader as [AndroidFont.typefaceLoader]
         * @return [android.graphics.Typeface] for loaded font, or null if not available
         * @throws RuntimeException subclass if font should be loadable and load fails
         *
         */
        suspend fun loadAsync(context: Context, font: AndroidFont): Typeface?
    }
}

@OptIn(ExperimentalTextApi::class)
internal abstract class AndroidPreloadedFont : AndroidFont(Blocking) {
    abstract val typefaceInternal: Typeface?
    override val typefaceLoader: TypefaceLoader = AndroidPreloadedFontTypefaceLoader
    abstract val cacheKey: String?
}

private object AndroidPreloadedFontTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun load(context: Context, font: AndroidFont): Typeface? =
        (font as? AndroidPreloadedFont)?.typefaceInternal

    override suspend fun loadAsync(context: Context, font: AndroidFont): Typeface? =
        load(context, font)
}

@OptIn(ExperimentalTextApi::class)
private class AndroidAssetFont constructor(
    val assetManager: AssetManager,
    val path: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidPreloadedFont() {
    override val typefaceInternal: Typeface? = Typeface.createFromAsset(assetManager, path)
    override val cacheKey: String = "asset:$path"

    override fun toString(): String {
        return "Font(assetManager, path=$path, weight=$weight, style=$style)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndroidAssetFont

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

@OptIn(ExperimentalTextApi::class)
private class AndroidFileFont constructor(
    val file: File,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidPreloadedFont() {
    override val typefaceInternal: Typeface? = Typeface.createFromFile(file)
    override val cacheKey: String? = null

    override fun toString(): String {
        return "Font(file=$file, weight=$weight, style=$style)"
    }
}

@OptIn(ExperimentalTextApi::class)
@RequiresApi(26)
private class AndroidFileDescriptorFont constructor(
    val fileDescriptor: ParcelFileDescriptor,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidPreloadedFont() {
    override val typefaceInternal = if (Build.VERSION.SDK_INT >= 26) {
        AndroidFileDescriptorHelper.create(fileDescriptor)
    } else {
        throw IllegalArgumentException("Cannot create font from file descriptor for SDK < 26")
    }
    override val cacheKey: String? = null

    override fun toString(): String {
        return "Font(fileDescriptor=$fileDescriptor, weight=$weight, style=$style)"
    }
}

@RequiresApi(26)
private object AndroidFileDescriptorHelper {
    @RequiresApi(26)
    @DoNotInline
    fun create(fileDescriptor: ParcelFileDescriptor): Typeface {
        return Typeface.Builder(fileDescriptor.fileDescriptor).build()
    }
}
