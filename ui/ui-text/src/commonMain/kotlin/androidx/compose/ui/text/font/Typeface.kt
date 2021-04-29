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

package androidx.compose.ui.text.font

/**
 * A class that can be used for changing the font used in text.
 */
interface Typeface {

    // TODO Unused, not tested public function
    /**
     * The font family used for creating this Typeface. If a platform Typeface was used, will
     * return null.
     *
     */
    val fontFamily: FontFamily?
}
