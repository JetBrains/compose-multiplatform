package org.jetbrains.compose.splitpane

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy

class SplitPaneState(
    val splitterState: SplitterState,
    enabled: Boolean
) {
    private var _moveEnabled = mutableStateOf(enabled, structuralEqualityPolicy())

    var moveEnabled: Boolean
        get() = _moveEnabled.value
        set(newValue) {
            _moveEnabled.value = newValue
        }

}