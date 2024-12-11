/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import okhttp3.MediaType.Companion.toMediaType

internal object Json {
    val mediaType = "application/json".toMediaType()
}