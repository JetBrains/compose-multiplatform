/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.core.tests.values
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.value
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSDisplayTests {

    @Test
    fun stylesDisplay() = runTest {
        val enumValues = DisplayStyle.values()

        composition {
            enumValues.forEach { displayStyle ->
                Div(
                    {
                        style {
                            display(displayStyle)
                        }
                    }
                )
            }
        }

        enumValues.forEach { displayStyle ->
            assertEquals(
                displayStyle.value,
                (nextChild()).style.display
            )
        }
    }

}
