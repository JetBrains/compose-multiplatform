/*
 * Copyright 2023 The Android Open Source Project
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

internal actual fun strongDirectionType(codePoint: Int): StrongDirectionType =
    codePoint.getDirectionality().toStrongDirectionType()

/**
 * Get the Unicode directionality of a character.
 */
private fun Int.getDirectionality(): CharDirectionality =
    CharDirectionality.valueOf(Character.getDirectionality(this).toInt())

/**
 * Get strong (R, L or AL) direction type.
 * See https://www.unicode.org/reports/tr9/
 */
private fun CharDirectionality.toStrongDirectionType() = when (this) {
    CharDirectionality.LEFT_TO_RIGHT -> StrongDirectionType.Ltr

    CharDirectionality.RIGHT_TO_LEFT,
    CharDirectionality.RIGHT_TO_LEFT_ARABIC -> StrongDirectionType.Rtl

    else -> StrongDirectionType.None
}
