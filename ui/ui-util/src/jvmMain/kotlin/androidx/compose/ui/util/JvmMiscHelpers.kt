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

package androidx.compose.ui.util

actual fun Any?.identityHashCode(): Int = if (this == null) 0 else System.identityHashCode(this)

actual fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)

actual fun StringBuilder.deleteAt(index: Int): StringBuilder {
    this.deleteCharAt(index)
    return this
}

actual fun Any.nativeClass(): Any = this.javaClass

actual typealias TreeSet<T> = java.util.TreeSet<T>
