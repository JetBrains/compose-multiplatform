/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Input

@Composable
fun <T> FormControlPrecedence(type: InputType<T>, value: String, selected: Boolean) {
    Input(type = type) {
        value(value)
        defaultValue("default")
        if (selected) checked(true) else checked(false)
        onInput { }
    }
}
