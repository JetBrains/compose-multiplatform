/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.android.tools.idea

object AndroidTextUtils {
    fun generateCommaSeparatedList(items: Collection<String?>, lastSeparator: String): String {
        val n = items.size
        if (n == 0) {
            return ""
        }
        var i = 0
        val result = StringBuilder()
        for (word in items) {
            result.append(word)
            if (i < n - 2) {
                result.append(", ")
            } else if (i == n - 2) {
                result.append(" ").append(lastSeparator).append(" ")
            }
            i++
        }
        return result.toString()
    }
}