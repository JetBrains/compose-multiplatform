package org.jetbrains.codeviewer.util

import androidx.compose.runtime.MutableState

fun <T> MutableState<T>.afterSet(
    action: (T) -> Unit
) = object : MutableState<T> by this {
    override var value: T
        get() = this@afterSet.value
        set(value) {
            this@afterSet.value = value
            action(value)
        }
}