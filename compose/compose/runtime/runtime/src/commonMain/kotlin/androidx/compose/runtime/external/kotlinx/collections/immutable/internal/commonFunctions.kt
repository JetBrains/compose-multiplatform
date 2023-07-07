/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.internal

internal fun assert(@Suppress("UNUSED_PARAMETER") condition: Boolean) {}

@Suppress("NO_ACTUAL_FOR_EXPECT") // implemented by protected property in JVM
internal expect var AbstractMutableList<*>.modCount: Int