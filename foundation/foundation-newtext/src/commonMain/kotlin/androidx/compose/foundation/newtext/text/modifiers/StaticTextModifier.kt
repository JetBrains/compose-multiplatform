/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalComposeUiApi::class)
internal class StaticTextModifier(
    params: TextInlineContentLayoutDrawParams
) : DelegatingNode(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {

    private val drawLayout = delegated { TextInlineContentLayoutDrawModifier(params) }

    private var text: AnnotatedString = params.text

    override val semanticsConfiguration: SemanticsConfiguration
        get() = SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = false
            it.isClearingSemantics = false
            it.text = text
            it.getTextLayoutResult { textLayoutResult ->
               val result = drawLayout.layoutOrNull
               if (result == null) {
                   return@getTextLayoutResult false
               } else {
                   textLayoutResult.add(result)
                   return@getTextLayoutResult true
               }
            }
        }

    fun update(params: TextInlineContentLayoutDrawParams) {
        text = params.text
        drawLayout.update(params)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return drawLayout.minIntrinsicWidthNonExtension(this, measurable, height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return drawLayout.minIntrinsicHeightNonExtension(this, measurable, width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return drawLayout.maxIntrinsicWidthNonExtension(this, measurable, height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return drawLayout.maxIntrinsicHeightNonExtension(this, measurable, width)
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return drawLayout.measureNonExtension(this, measurable, constraints)
    }

    override fun ContentDrawScope.draw() {
        drawLayout.drawNonExtension(this)
    }
}