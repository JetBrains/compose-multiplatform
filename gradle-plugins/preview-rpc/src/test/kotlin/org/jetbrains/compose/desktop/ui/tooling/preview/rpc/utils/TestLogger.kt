/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils

import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewLogger

internal class TestLogger : PreviewLogger() {
    private val myMessages = arrayListOf<String>()
    val messages: List<String>
        get() = myMessages

    override val isEnabled: Boolean
        get() = true

    override fun log(s: String) {
        myMessages.add(s)
    }
}