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

package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * This annotation is applied to functions on the LiveLiteral classes created by the Compose
 * Compiler. It is intended to be used to provide information useful to tooling.
 *
 * @param key The unique identifier for the literal.
 * @param offset The startOffset of the literal in the source file at the time of compilation.
 */
@ComposeCompilerApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LiveLiteralInfo(
    val key: String,
    val offset: Int
)

/**
 * This annotation is applied to LiveLiteral classes by the Compose Compiler. It is intended to
 * be used to provide information useful to tooling.
 *
 * @param file The file path of the file the associate LiveLiterals class was produced for
 */
@ComposeCompilerApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LiveLiteralFileInfo(
    val file: String
)

private val liveLiteralCache = HashMap<String, MutableState<Any?>>()

@InternalComposeApi
@ComposeCompilerApi
var isLiveLiteralsEnabled: Boolean = false
    private set

/**
 * When called, all live literals will start to use their values backed by [MutableState].
 *
 * Caution: This API is intended to be used by tooling only. Use at your own risk.
 */
@InternalComposeApi
fun enableLiveLiterals() {
    isLiveLiteralsEnabled = true
}

/**
 * Constructs a [State] object identified by the provided global [key] and initialized to the
 * provided [value]. This value may then be updated from tooling with the
 * [updateLiveLiteralValue] API. Only a single [State] object will be created for any given [key].
 *
 * Caution: This API is intended to be used by tooling only. Use at your own risk.
 */
@InternalComposeApi
@ComposeCompilerApi
fun <T> liveLiteral(key: String, value: T): State<T> {
    @Suppress("UNCHECKED_CAST")
    return liveLiteralCache.getOrPut(key) {
        mutableStateOf<Any?>(value)
    } as State<T>
}

/**
 * Updates the value of a [State] object that was created by [liveLiteral] with the same key.
 */
@InternalComposeApi
fun updateLiveLiteralValue(key: String, value: Any?) {
    var needToUpdate = true
    val stateObj = liveLiteralCache.getOrPut(key) {
        needToUpdate = false
        mutableStateOf<Any?>(value)
    }
    if (needToUpdate) {
        stateObj.value = value
    }
}