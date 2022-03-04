/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import kotlin.reflect.KProperty

internal fun <T : Any> requiredDslProperty(missingMessage: String) = RequiredPropertyDelegate<T>(missingMessage)

class RequiredPropertyDelegate<T>(val missingMessage: String) {
    var realValue: T? = null
    operator fun setValue(
        ref: Any,
        property: KProperty<*>,
        newValue: T
    ) {
        realValue = newValue
    }

    operator fun getValue(
        ref: Any,
        property: KProperty<*>
    ): T {
        return realValue ?: error(missingMessage)
    }
}
