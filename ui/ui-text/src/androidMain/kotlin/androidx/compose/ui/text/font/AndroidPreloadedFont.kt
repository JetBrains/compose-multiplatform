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

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastMap
import java.io.File

@OptIn(ExperimentalTextApi::class)
internal sealed class AndroidPreloadedFont @OptIn(ExperimentalTextApi::class) constructor(
    final override val weight: FontWeight,
    final override val style: FontStyle,
    variationSettings: FontVariation.Settings
) : AndroidFont(
    FontLoadingStrategy.Blocking,
    AndroidPreloadedFontTypefaceLoader,
    variationSettings
) {
    abstract val cacheKey: String?
    internal abstract fun doLoad(context: Context?): Typeface?

    private var didInitWithContext: Boolean = false
    // subclasses MUST initialize this by calling doLoad(null) - after overriding doLoad as final
    internal var typeface: Typeface? = null

    internal fun loadCached(context: Context): Typeface? {
        if (!didInitWithContext && typeface == null) {
            typeface = doLoad(context)
        }
        didInitWithContext = true
        return typeface
    }
}

private object AndroidPreloadedFontTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? =
        (font as? AndroidPreloadedFont)?.loadCached(context)

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Nothing {
        throw UnsupportedOperationException("All preloaded fonts are blocking.")
    }
}

@OptIn(ExperimentalTextApi::class) /* FontVariation.Settings */
internal class AndroidAssetFont constructor(
    val assetManager: AssetManager,
    val path: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings
) : AndroidPreloadedFont(weight, style, variationSettings) {

    override fun doLoad(context: Context?): Typeface? {
        return if (Build.VERSION.SDK_INT >= 26) {
            TypefaceBuilderCompat.createFromAssets(assetManager, path, context, variationSettings)
        } else {
            Typeface.createFromAsset(assetManager, path)
        }
    }

    init {
        typeface = doLoad(null)
    }

    override val cacheKey: String = "asset:$path"

    override fun toString(): String {
        return "Font(assetManager, path=$path, weight=$weight, style=$style)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AndroidAssetFont) return false

        if (path != other.path) return false
        if (variationSettings != other.variationSettings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + variationSettings.hashCode()
        return result
    }
}

@OptIn(ExperimentalTextApi::class)
internal class AndroidFileFont constructor(
    val file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings
) : AndroidPreloadedFont(weight, style, variationSettings) {

    override fun doLoad(context: Context?): Typeface? {
        return if (Build.VERSION.SDK_INT >= 26) {
            TypefaceBuilderCompat.createFromFile(file, context, variationSettings)
        } else {
            Typeface.createFromFile(file)
        }
    }

    init {
        typeface = doLoad(null)
    }

    override val cacheKey: String? = null
    override fun toString(): String {
        return "Font(file=$file, weight=$weight, style=$style)"
    }
}

@RequiresApi(26)
@OptIn(ExperimentalTextApi::class)
internal class AndroidFileDescriptorFont constructor(
    val fileDescriptor: ParcelFileDescriptor,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings
) : AndroidPreloadedFont(weight, style, variationSettings) {

    override fun doLoad(context: Context?): Typeface? {
        return if (Build.VERSION.SDK_INT >= 26) {
            TypefaceBuilderCompat.createFromFileDescriptor(
                fileDescriptor,
                context,
                variationSettings
            )
        } else {
            throw IllegalArgumentException("Cannot create font from file descriptor for SDK < 26")
        }
    }

    init {
        typeface = doLoad(null)
    }

    override val cacheKey: String? = null
    override fun toString(): String {
        return "Font(fileDescriptor=$fileDescriptor, weight=$weight, style=$style)"
    }
}

@RequiresApi(api = 26)
private object TypefaceBuilderCompat {
    @ExperimentalTextApi
    @DoNotInline
    fun createFromAssets(
        assetManager: AssetManager,
        path: String,
        context: Context?,
        variationSettings: FontVariation.Settings
    ): Typeface? {
        if (context == null) {
            return null
        }
        return Typeface.Builder(assetManager, path)
            .setFontVariationSettings(variationSettings.toVariationSettings(context))
            .build()
    }

    @ExperimentalTextApi
    @DoNotInline
    fun createFromFile(
        file: File,
        context: Context?,
        variationSettings: FontVariation.Settings
    ): Typeface? {
        if (context == null) {
            return null
        }
        return Typeface.Builder(file)
            .setFontVariationSettings(variationSettings.toVariationSettings(context))
            .build()
    }

    @ExperimentalTextApi
    @DoNotInline
    fun createFromFileDescriptor(
        fileDescriptor: ParcelFileDescriptor,
        context: Context?,
        variationSettings: FontVariation.Settings,
    ): Typeface? {
        if (context == null) {
            return null
        }
        return Typeface.Builder(fileDescriptor.fileDescriptor)
            .setFontVariationSettings(variationSettings.toVariationSettings(context))
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @ExperimentalTextApi
    private fun FontVariation.Settings.toVariationSettings(
        context: Context?
    ): Array<FontVariationAxis> {
        val density = if (context != null) {
            Density(context)
        } else if (!needsDensity) {
            // we don't need density, so make a fake one and be on with it
            Density(1f, 1f)
        } else {
            // cannot reach
            throw IllegalStateException("Required density, but not provided")
        }
        return settings.fastMap { setting ->
            FontVariationAxis(setting.axisName, setting.toVariationValue(density))
        }.toTypedArray()
    }
}
