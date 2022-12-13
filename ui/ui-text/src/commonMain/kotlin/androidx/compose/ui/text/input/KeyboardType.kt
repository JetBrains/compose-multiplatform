/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.runtime.Stable

/**
 * Values representing the different available Keyboard Types.
 */
@kotlin.jvm.JvmInline
value class KeyboardType internal constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            Text -> "Text"
            Ascii -> "Ascii"
            Number -> "Number"
            Phone -> "Phone"
            Uri -> "Uri"
            Email -> "Email"
            Password -> "Password"
            NumberPassword -> "NumberPassword"
            Decimal -> "Decimal"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * A keyboard type used to request an IME that shows regular keyboard.
         */
        @Stable
        val Text: KeyboardType = KeyboardType(1)

        /**
         * A keyboard type used to request an IME that is capable of inputting ASCII characters.
         */
        @Stable
        val Ascii: KeyboardType = KeyboardType(2)

        /**
         * A keyboard type used to request an IME that is capable of inputting digits. IME may
         * provide inputs other than digits but it is not guaranteed.
         *
         * @see KeyboardType.Decimal
         */
        @Stable
        val Number: KeyboardType = KeyboardType(3)

        /**
         * A keyboard type used to request an IME that is capable of inputting phone numbers.
         */
        @Stable
        val Phone: KeyboardType = KeyboardType(4)

        /**
         * A keyboard type used to request an IME that is capable of inputting URIs.
         */
        @Stable
        val Uri: KeyboardType = KeyboardType(5)

        /**
         * A keyboard type used to request an IME that is capable of inputting email addresses.
         */
        @Stable
        val Email: KeyboardType = KeyboardType(6)

        /**
         * A keyboard type used to request an IME that is capable of inputting password.
         */
        @Stable
        val Password: KeyboardType = KeyboardType(7)

        /**
         * A keyboard type used to request an IME that is capable of inputting number password.
         */
        @Stable
        val NumberPassword: KeyboardType = KeyboardType(8)

        /**
         * A keyboard type used to request an IME that is capable of inputting decimals.
         * IME should explicitly provide a decimal separator as input, which is not assured by
         * [KeyboardType.Number].
         */
        @Stable
        val Decimal: KeyboardType = KeyboardType(9)
    }
}
