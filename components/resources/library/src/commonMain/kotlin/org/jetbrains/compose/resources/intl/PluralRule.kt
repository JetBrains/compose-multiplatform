/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.intl

internal class PluralRule(val category: PluralCategory, private val constraint: PluralCondition) {
    fun appliesTo(n: Int): Boolean {
        return constraint.isFulfilled(n)
    }

    // add appliesTo(n: Double) or appliesTo(n: Decimal) as needed
}