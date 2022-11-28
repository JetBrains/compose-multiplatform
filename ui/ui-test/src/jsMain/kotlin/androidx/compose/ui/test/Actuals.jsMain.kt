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

package androidx.compose.ui.test

import androidx.compose.runtime.NoLiveLiterals

// This is a copy of compose/runtime/runtime/src/jsMain/kotlin/androidx/compose/runtime/ActualJs.js.kt

private var nextHash = 1
private const val IDENTITY_HASHCODE_FIELD = "kotlinIdentityHashcodeValue$"

@NoLiveLiterals
private fun memoizeIdentityHashCode(instance: Any?): Int {
    val value = nextHash++

    val descriptor = js("new Object()")
    descriptor.value = value
    descriptor.writable = false
    descriptor.configurable = false
    descriptor.enumerable = false

    js("Object").defineProperty(instance, IDENTITY_HASHCODE_FIELD, descriptor)

    return value
}

/**
 * Returns the hash code for the given object that is unique across all currently allocated objects.
 * The hash code for the null reference is zero.
 *
 * Can be negative, and near Int.MAX_VALUE, so it can overflow if used as part of calculations.
 * For example, don't use this:
 * ```
 * val comparison = identityHashCode(midVal) - identityHashCode(leftVal)
 * if (comparison < 0) ...
 * ```
 * Use this instead:
 * ```
 * if (identityHashCode(midVal) < identityHashCode(leftVal)) ...
 * ```
 */
internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }

    val hashCode = instance.asDynamic()[IDENTITY_HASHCODE_FIELD]
    if (hashCode != null) {
        return hashCode
    }

    return when (jsTypeOf(instance)) {
        "object", "function" -> memoizeIdentityHashCode(instance)
        else -> throw IllegalArgumentException(
            "identity hash code for ${jsTypeOf(instance)} is not supported"
        )
    }
}
