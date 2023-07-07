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

package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable

/**
 * The IME configuration options for [TextInputService]. It is not guaranteed if IME
 * will comply with the options provided here.
 *
 * @param singleLine informs the IME that the text field is single line and IME
 * should not show return key.
 * @param capitalization informs the IME whether to automatically capitalize characters,
 * words or sentences. Only applicable to only text based [KeyboardType]s such as
 * [KeyboardType.Text], [KeyboardType.Ascii]. It will not be applied to [KeyboardType]s such as
 * [KeyboardType.Number] or [KeyboardType.Decimal].
 * @param autoCorrect informs the IME whether to enable auto correct. Only applicable to
 * text based [KeyboardType]s such as [KeyboardType.Email], [KeyboardType.Uri]. It will not be
 * applied to [KeyboardType]s such as [KeyboardType.Number] or [KeyboardType.Decimal]. Most of IME
 * implementations ignore this value for [KeyboardType]s such as [KeyboardType.Text].
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * When [singleLine] is false, the IME might show return key rather than the action requested here.
 */
@Immutable
class ImeOptions(
    val singleLine: Boolean = false,
    val capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    val autoCorrect: Boolean = true,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val imeAction: ImeAction = ImeAction.Default
) {
    companion object {
        /**
         * Default [ImeOptions]. Please see parameter descriptions for default values.
         */
        val Default = ImeOptions()
    }

    fun copy(
        singleLine: Boolean = this.singleLine,
        capitalization: KeyboardCapitalization = this.capitalization,
        autoCorrect: Boolean = this.autoCorrect,
        keyboardType: KeyboardType = this.keyboardType,
        imeAction: ImeAction = this.imeAction
    ): ImeOptions {
        return ImeOptions(
            singleLine = singleLine,
            capitalization = capitalization,
            autoCorrect = autoCorrect,
            keyboardType = keyboardType,
            imeAction = imeAction
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImeOptions) return false

        if (singleLine != other.singleLine) return false
        if (capitalization != other.capitalization) return false
        if (autoCorrect != other.autoCorrect) return false
        if (keyboardType != other.keyboardType) return false
        if (imeAction != other.imeAction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = singleLine.hashCode()
        result = 31 * result + capitalization.hashCode()
        result = 31 * result + autoCorrect.hashCode()
        result = 31 * result + keyboardType.hashCode()
        result = 31 * result + imeAction.hashCode()
        return result
    }

    override fun toString(): String {
        return "ImeOptions(singleLine=$singleLine, capitalization=$capitalization, " +
            "autoCorrect=$autoCorrect, keyboardType=$keyboardType, imeAction=$imeAction)"
    }
}