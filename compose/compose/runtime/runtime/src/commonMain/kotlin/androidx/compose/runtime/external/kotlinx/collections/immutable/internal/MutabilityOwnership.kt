/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.internal

/**
 * The mutability ownership token of a persistent collection builder.
 *
 * Used to mark persistent data structures, that are owned by a collection builder and can be mutated by it.
 */
internal class MutabilityOwnership