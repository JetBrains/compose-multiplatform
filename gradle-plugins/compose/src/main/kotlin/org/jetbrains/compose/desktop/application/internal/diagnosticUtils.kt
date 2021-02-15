package org.jetbrains.compose.desktop.application.internal

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception

internal fun Exception.stacktraceToString(): String =
    StringWriter().also { w ->
        PrintWriter(w).use { pw -> printStackTrace(pw) }
    }.toString()