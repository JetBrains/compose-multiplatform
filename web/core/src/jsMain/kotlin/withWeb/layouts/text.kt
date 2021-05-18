/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Text as TextNode
import androidx.compose.web.elements.Span
import org.jetbrains.compose.web.ui.Styles
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.asStyleBuilderApplier
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier
import org.jetbrains.compose.common.core.graphics.Color
import androidx.compose.web.css.color
import androidx.compose.web.css.fontSize
import androidx.compose.web.css.Color.RGB
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.TextUnitType
import androidx.compose.web.css.em
import androidx.compose.web.css.px

@Composable
actual fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    Span(
        style = modifier.asStyleBuilderApplier() {
            color(RGB(color.red, color.green, color.blue))
            when (size.unitType) {
                TextUnitType.Em -> fontSize(size.value.em)
                TextUnitType.Sp -> fontSize(size.value.px)
            }
        },
        attrs = modifier.asAttributeBuilderApplier() {
            classes(Styles.textClass)
        }
    ) {
        TextNode(text)
    }
}
