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

package androidx.compose.ui.text.input

/**
 *
 * Workaround to bypass bug https://youtrack.jetbrains.com/issue/KT-52336/Differs-on-JVM-and-Native-in-stringBuilder-append-charArray-0-1
 * On JVM and Android this function work's as StringBuilder.append(char[], int offset, int len)
 */
internal expect fun StringBuilder.appendPartOfCharArray(charArray: CharArray, offset: Int, len: Int)
