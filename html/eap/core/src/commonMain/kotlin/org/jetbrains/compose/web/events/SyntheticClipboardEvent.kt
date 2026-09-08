/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.clipboard.ClipboardEvent
import kotlinx.browser.dom.events.EventTarget

class SyntheticClipboardEvent internal constructor(
    nativeEvent: ClipboardEvent
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val clipboardData: DataTransfer? = nativeEvent.clipboardData

    fun getData(format: String): String? {
        return clipboardData?.getData(format)
    }

    fun setData(format: String, data: String) {
        clipboardData?.setData(format, data)
    }
}
