import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.renderComposable
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

class RenderComposableTests {
    @Test
    fun compCount() = MainScope().promise {
        var count = 0
        renderComposable(document.createElement("div")) {
            count++
        }
        delay(1000)
        assertEquals(1, count)
    }
}