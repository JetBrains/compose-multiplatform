/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

internal data class DeltaCounter(var count: Int = 0) {
    operator fun plusAssign(that: Int) { count += that }
}