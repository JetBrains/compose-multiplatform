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
