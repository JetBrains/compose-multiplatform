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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.fastAny

/**
 * Set font variation settings.
 *
 * To learn more about the font variation settings, see the list supported by
 * [fonts.google.com](https://fonts.google.com/variablefonts#axis-definitions).
 */
@ExperimentalTextApi
object FontVariation {
    /**
     * A collection of settings to apply to a single font.
     *
     * Settings must be unique on [Setting.axisName]
     */
    @Immutable
    class Settings(vararg settings: Setting) {
        /**
         * All settings, unique by [FontVariation.Setting.axisName]
         */
        val settings: List<Setting>

        /**
         * True if density is required to resolve any of these settings
         *
         * If false, density will not affect the result of any [Setting.toVariationValue].
         */
        internal val needsDensity: Boolean

        init {
            this.settings = ArrayList(settings
                .groupBy { it.axisName }
                .flatMap { (key, value) ->
                    require(value.size == 1) {
                        "'$key' must be unique. Actual [ [${value.joinToString()}]"
                    }
                    value
                })

            needsDensity = this.settings.fastAny { it.needsDensity }
        }
    }

    /**
     * Represents a single point in a variation, such as 0.7 or 100
     */
    @Immutable
    interface Setting {
        /**
         * Convert a value to a final value for use as a font variation setting.
         *
         * If [needsDensity] is false, density may be null
         *
         * @param density to resolve from Compose types to feature-specific ranges.
         */
        fun toVariationValue(density: Density?): Float

        /**
         * True if this setting requires density to resolve
         *
         * When false, may toVariationValue may be called with null or any Density
         */
        val needsDensity: Boolean

        /**
         * The font variation axis, such as 'wdth' or 'ital'
         */
        val axisName: String
    }

    @Immutable
    private class SettingFloat(
        override val axisName: String,
        val value: Float
    ) : Setting {
        override fun toVariationValue(density: Density?): Float = value
        override val needsDensity: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SettingFloat) return false

            if (axisName != other.axisName) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = axisName.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }

        override fun toString(): String {
            return "FontVariation.Setting(axisName='$axisName', value=$value)"
        }
    }

    @Immutable
    private class SettingTextUnit(
        override val axisName: String,
        val value: TextUnit
    ) : Setting {
        override fun toVariationValue(density: Density?): Float {
            // we don't care about pixel density as 12sp is the same "visual" size on all devices
            // instead we only care about font scaling, which changes visual size
            requireNotNull(density) { "density must not be null" }
            return value.value * density.fontScale
        }

        override val needsDensity: Boolean = true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SettingTextUnit) return false

            if (axisName != other.axisName) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = axisName.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }

        override fun toString(): String {
            return "FontVariation.Setting(axisName='$axisName', value=$value)"
        }
    }

    @Immutable
    private class SettingInt(
        override val axisName: String,
        val value: Int
    ) : Setting {
        override fun toVariationValue(density: Density?): Float = value.toFloat()
        override val needsDensity: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SettingInt) return false

            if (axisName != other.axisName) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = axisName.hashCode()
            result = 31 * result + value
            return result
        }

        override fun toString(): String {
            return "FontVariation.Setting(axisName='$axisName', value=$value)"
        }
    }

    /**
     * Create a font variation setting for any axis supported by a font.
     *
     * ```
     * val setting = FontVariation.Setting('wght', 400f);
     * ```
     *
     * You should typically not use this in app-code directly, instead define a method for each
     * setting supported by your app/font.
     *
     * If you had a setting `fzzt` that set a variation setting called fizzable between 1 and 11,
     * define a function like this:
     *
     * ```
     * fun FontVariation.fizzable(fiz: Int): FontVariation.Setting {
     *    require(fiz in 1..11) { "'fzzt' must be in 1..11" }
     *    return Setting("fzzt", fiz.toFloat())
     * ```
     *
     * @param name axis name, must be 4 characters
     * @param value value for axis, not validated and directly passed to font
     */
    fun Setting(name: String, value: Float): Setting {
        require(name.length == 4) {
            "Name must be exactly four characters. Actual: '$name'"
        }
        return SettingFloat(name, value)
    }

    /**
     * Italic or upright, equivalent to [FontStyle]
     *
     * 'ital', 0.0f is upright, and 1.0f is italic.
     *
     * A platform _may_ provide automatic setting of `ital` on font load. When supported, `ital` is
     * automatically applied based on [FontStyle] if platform and the loaded font support 'ital'.
     *
     * Automatic mapping is done via [Settings]\([FontWeight], [FontStyle]\)
     *
     * To override this behavior provide an explicit FontVariation.italic to a [Font] that supports
     * variation settings.
     *
     * @param value [0.0f, 1.0f]
     */
    fun italic(value: Float): Setting {
        require(value in 0.0f..1.0f) {
            "'ital' must be in 0.0f..1.0f. Actual: $value"
        }
        return SettingFloat("ital", value)
    }

    /**
     * Optical size is how "big" a font appears to the eye.
     *
     * It should be set by a ratio from a font size.
     *
     * Adapt the style to specific text sizes. At smaller sizes, letters typically become optimized
     * for more legibility. At larger sizes, optimized for headlines, with more extreme weights and
     * widths.
     *
     * A Platform _may_ choose to support automatic optical sizing. When present, this will set the
     * optical size based on the font size.
     *
     * To override this behavior provide an explicit FontVariation.opticalSizing to a [Font] that
     * supports variation settings.
     *
     * @param textSize font-size at the expected display, must be in sp
     */
    fun opticalSizing(textSize: TextUnit): Setting {
        require(textSize.isSp) {
            "'opsz' must be provided in sp units"
        }
        return SettingTextUnit("opsz", textSize)
    }

    /**
     * Adjust the style from upright to slanted, also known to typographers as an 'oblique' style.
     *
     * Rarely, slant can work in the other direction, called a 'backslanted' or 'reverse oblique'
     * style.
     *
     * 'slnt', values as an angle, 0f is upright.
     *
     * @param value -90f to 90f, represents an angle
     */
    fun slant(value: Float): Setting {
        require(value in -90f..90f) {
            "'slnt' must be in -90f..90f. Actual: $value"
        }
        return SettingFloat("slnt", value)
    }

    /**
     * Width of the type.
     *
     * Adjust the style from narrower to wider, by varying the proportions of counters, strokes,
     * spacing and kerning, and other aspects of the type. This typically changes the typographic
     * color in a subtle way, and so may be used in conjunction with Width and Grade axes.
     *
     * 'wdth', such as 10f
     *
     * @param value > 0.0f represents the width
     */
    fun width(value: Float): Setting {
        require(value > 0.0f) {
            "'wdth' must be strictly > 0.0f. Actual: $value"
        }
        return SettingFloat("wdth", value)
    }

    /**
     * Weight, equivalent to [FontWeight]
     *
     * Setting weight always causes visual text reflow, to make text "bolder" or "thinner" without
     * reflow see [grade]
     *
     * Adjust the style from lighter to bolder in typographic color, by varying stroke weights,
     * spacing and kerning, and other aspects of the type. This typically changes overall width,
     * and so may be used in conjunction with Width and Grade axes.
     *
     * This is equivalent to [FontWeight], and platforms _may_ support automatically setting 'wghts'
     * from [FontWeight] during font load.
     *
     * Setting this does not change [FontWeight]. If an explicit value and [FontWeight] disagree,
     * the weight specified by `wght` will be shown if the font supports it.
     *
     * Automatic mapping is done via [Settings]\([FontWeight], [FontStyle]\)
     *
     * @param value weight, in 1..1000
     */
    fun weight(value: Int): Setting {
        require(value in 1..1000) {
            "'wght' value must be in [1, 1000]. Actual: $value"
        }
        return SettingInt("wght", value)
    }

    /**
     * Change visual weight of text without text reflow.
     *
     * Finesse the style from lighter to bolder in typographic color, without any changes overall
     * width, line breaks or page layout. Negative grade makes the style lighter, while positive
     * grade makes it bolder. The units are the same as in the Weight axis.
     *
     * Visual appearance of text with weight and grade set is similar to text with
     *
     * ```
     * weight = (weight + grade)
     * ```
     *
     * @param value grade, in -1000..1000
     */
    fun grade(value: Int): Setting {
        require(value in -1000..1000) {
            "'GRAD' must be in -1000..1000"
        }
        return SettingInt("GRAD", value)
    }

    /**
     * Variation settings to configure a font with [FontWeight] and [FontStyle]
     *
     * @param weight to set 'wght' with [weight]\([FontWeight.weight])
     * @param style to set 'ital' with [italic]\([FontStyle.value])
     * @param settings other settings to apply, must not contain 'wght' or 'ital'
     * @return settings that configure [FontWeight] and [FontStyle] on a font that supports
     * 'wght' and 'ital'
     */
    fun Settings(
        weight: FontWeight,
        style: FontStyle,
        vararg settings: Setting
    ): Settings {
        return Settings(weight(weight.weight), italic(style.value.toFloat()), *settings)
    }
}