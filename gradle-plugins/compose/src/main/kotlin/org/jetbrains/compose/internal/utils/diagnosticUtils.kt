/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception

internal fun Exception.stacktraceToString(): String =
    StringWriter().also { w ->
        PrintWriter(w).use { pw -> printStackTrace(pw) }
    }.toString()