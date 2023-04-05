/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.DisposableEffectScope
import org.w3c.dom.Element

interface DOMScope<out TElement : Element> {
    val DisposableEffectScope.scopeElement: TElement
}