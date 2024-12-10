/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.core.tests.values
import org.jetbrains.compose.web.css.VisibilityStyle
import org.jetbrains.compose.web.css.visibility
import org.jetbrains.compose.web.css.value
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSVisibilityTests {

    @Test
    fun stylesDisplay() = runTest {
        val enumValues = VisibilityStyle.values()

        composition {
            enumValues.forEach { displayStyle ->
                Div(
                    {
                        style {
                            visibility(displayStyle)
                        }
                    }
                )
            }
        }

        enumValues.forEach { visibilityStyle ->
            assertEquals(
                visibilityStyle.value,
                (nextChild()).style.visibility
            )
        }
    }

}
