/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString

private val sharedAlias = sharedContent

fun renderSharedLambda(): String = composeHtmlToString(content = sharedAlias)

fun renderSharedFunction(): String = composeHtmlToString(content = ::SharedPage)

fun renderSharedCall(): String = composeHtmlToString { SharedPage() }

private fun renderForwarded(content: @Composable () -> Unit): String = composeHtmlToString(content = content)

fun renderForwardedShared(): String = renderForwarded(sharedContent)
