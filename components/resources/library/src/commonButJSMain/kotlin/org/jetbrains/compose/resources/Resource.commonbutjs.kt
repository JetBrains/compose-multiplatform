/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.coroutines.runBlocking

internal actual fun isSyncResourceLoadingSupported() = true

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = runBlocking { readBytes() }

