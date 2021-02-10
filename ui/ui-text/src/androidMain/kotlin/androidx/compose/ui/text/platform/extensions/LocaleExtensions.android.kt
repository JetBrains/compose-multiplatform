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

package androidx.compose.ui.text.platform.extensions

import androidx.annotation.RequiresApi
import androidx.compose.ui.text.intl.AndroidLocale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList

internal fun Locale.toJavaLocale(): java.util.Locale = (platformLocale as AndroidLocale).javaLocale

@RequiresApi(api = 24)
internal fun LocaleList.toAndroidLocaleList(): android.os.LocaleList =
    android.os.LocaleList(*map { it.toJavaLocale() }.toTypedArray())
