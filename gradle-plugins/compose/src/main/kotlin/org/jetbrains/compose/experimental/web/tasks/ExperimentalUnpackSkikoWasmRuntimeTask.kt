/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.web.tasks

import org.jetbrains.compose.internal.DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB
import org.jetbrains.compose.web.tasks.UnpackSkikoWasmRuntimeTask

@Deprecated(
    message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB,
    replaceWith = ReplaceWith("UnpackSkikoWasmRuntimeTask")
)
abstract class ExperimentalUnpackSkikoWasmRuntimeTask : UnpackSkikoWasmRuntimeTask()
