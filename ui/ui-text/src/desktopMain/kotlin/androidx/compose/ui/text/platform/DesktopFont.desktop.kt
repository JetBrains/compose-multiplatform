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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.Cache
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ExpireAfterAccessCache
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastForEach
import java.io.File
import java.security.MessageDigest
import org.jetbrains.skia.Data
import org.jetbrains.skia.makeFromFile
import org.jetbrains.skia.Typeface as SkTypeface

actual sealed class PlatformFont : Font {
    actual abstract val identity: String
    internal actual val cacheKey: String
        get() = "${this::class.qualifiedName}|$identity"
}

internal actual val GenericFontFamiliesMapping by lazy {
    when (Platform.Current) {
        Platform.Windows ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Arial"),
                FontFamily.Serif.name to listOf("Times New Roman"),
                FontFamily.Monospace.name to listOf("Consolas"),
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
        Platform.MacOS ->
            mapOf(
                FontFamily.SansSerif.name to listOf(
                    "Helvetica Neue",
                    "Helvetica"
                ),
                FontFamily.Serif.name to listOf("Times"),
                FontFamily.Monospace.name to listOf("Courier"),
                FontFamily.Cursive.name to listOf("Apple Chancery")
            )
        Platform.Linux ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Noto Sans", "DejaVu Sans"),
                FontFamily.Serif.name to listOf("Noto Serif", "DejaVu Serif", "Times New Roman"),
                FontFamily.Monospace.name to listOf("Noto Sans Mono", "DejaVu Sans Mono"),
                // better alternative?
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
        Platform.Unknown ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Arial"),
                FontFamily.Serif.name to listOf("Times New Roman"),
                FontFamily.Monospace.name to listOf("Consolas"),
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
    }
}

/**
 * Defines a Font using resource name.
 *
 * @param name The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */

class ResourceFont internal constructor(
    val name: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : PlatformFont() {
    override val identity
        get() = name

    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceFont

        if (name != other.name) return false
        if (weight != other.weight) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResourceFont(name='$name', weight=$weight, style=$style)"
    }
}

/**
 * Creates a Font using resource name.
 *
 * @param resource The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
fun Font(
    resource: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = ResourceFont(resource, weight, style)

/**
 * Defines a Font using file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
class FileFont internal constructor(
    val file: File,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
) : PlatformFont() {
    override val identity
        get() = file.toString()

    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileFont

        if (file != other.file) return false
        if (weight != other.weight) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileFont(file=$file, weight=$weight, style=$style)"
    }
}

/**
 * Creates a Font using file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = FileFont(file, weight, style)

internal actual fun FontListFontFamily.makeAlias(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fonts.fastForEach { font ->
        when (font) {
            is PlatformFont -> {
                digest.update(font.identity.toByteArray())
            }
        }
    }
    return "-compose-${digest.digest().toHexString()}"
}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

internal actual fun loadFromTypefacesCache(font: Font): SkTypeface {
    if (font !is PlatformFont) {
        throw IllegalArgumentException("Unsupported font type: $font")
    }
    return typefacesCache.get(font.cacheKey) {
        when (font) {
            is ResourceFont -> typefaceResource(font.name)
            is FileFont -> SkTypeface.makeFromFile(font.file.toString())
            is LoadedFont -> SkTypeface.makeFromData(Data.makeFromBytes(font.data))
        }
    }
}

internal actual val typefacesCache: Cache<String, SkTypeface> =
    ExpireAfterAccessCache<String, SkTypeface>(
        60_000_000_000 // 1 minute
    )

private fun typefaceResource(resourceName: String): SkTypeface {
    val contextClassLoader = Thread.currentThread().contextClassLoader!!
    val resource = contextClassLoader.getResourceAsStream(resourceName)
        ?: (::typefaceResource.javaClass).getResourceAsStream(resourceName)
        ?: error("Can't load font from $resourceName")

    val bytes = resource.use { it.readAllBytes() }
    return SkTypeface.makeFromData(Data.makeFromBytes(bytes))
}

private enum class Platform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        val Current by lazy {
            val name = System.getProperty("os.name")
            when {
                name.startsWith("Linux") -> Linux
                name.startsWith("Win") -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
        }
    }
}
