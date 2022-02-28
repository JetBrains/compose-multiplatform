/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

internal enum class RuntimeCompressionLevel(internal val id: Int) {
    // For ID values see the docs on "--compress" https://docs.oracle.com/javase/9/tools/jlink.htm

    NO_COMPRESSION(0),
    CONSTANT_STRING_SHARING(1),
    ZIP(2)
}