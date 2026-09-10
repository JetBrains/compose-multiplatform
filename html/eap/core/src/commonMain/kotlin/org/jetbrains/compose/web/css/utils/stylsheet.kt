/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css.utils

import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.dom.stringPresentation


fun StyleSheet.serializeRules(): List<String> {
    return cssRules.serializeRules()
}

internal fun CSSRuleDeclarationList.serializeRules(): List<String> =
    map { it.stringPresentation(indent = " ", delimiter = "") }
