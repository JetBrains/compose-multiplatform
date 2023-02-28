/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.newtext.text.DefaultMinLines
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.invalidateMeasurements
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
internal class TextStringSimpleNode(
    private var text: String,
    private var style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, SemanticsModifierNode {
    private var baselineCache: Map<AlignmentLine, Int>? = null

    private var _layoutCache: ParagraphLayoutCache? = null
    private val layoutCache: ParagraphLayoutCache
        get() {
            if (_layoutCache == null) {
                _layoutCache = ParagraphLayoutCache(
                    text,
                    style,
                    fontFamilyResolver,
                    overflow,
                    softWrap,
                    maxLines,
                    minLines,
                )
            }
            return _layoutCache!!
        }

    private fun getLayoutCache(density: Density): ParagraphLayoutCache {
        return layoutCache.also { it.density = density }
    }

    fun updateText(text: String): Boolean {
        if (this.text == text) return false
        this.text = text
        return true
    }

    fun updateLayoutRelatedArgs(
        style: TextStyle,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow
    ): Boolean {
        var changed = false
        if (this.style != style) {
            this.style = style
            changed = true
        }

        if (this.minLines != minLines) {
            this.minLines = minLines
            changed = true
        }

        if (this.maxLines != maxLines) {
            this.maxLines != maxLines
            changed = true
        }

        if (this.softWrap != softWrap) {
            this.softWrap = softWrap
            changed = true
        }

        if (this.fontFamilyResolver != fontFamilyResolver) {
            this.fontFamilyResolver = fontFamilyResolver
            changed = true
        }

        if (this.overflow != overflow) {
            this.overflow = overflow
            changed = true
        }

        return changed
    }

    fun doInvalidations(
        textChanged: Boolean,
        layoutChanged: Boolean
    ) {
        if (textChanged) {
            _semanticsConfiguration = null
            invalidateSemantics()
        }

        if (textChanged || layoutChanged) {
            layoutCache.update(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines
            )
            invalidateMeasurements()
            invalidateDraw()
        }
    }

    private var _semanticsConfiguration: SemanticsConfiguration? = null

    private var semanticsTextLayoutResult: ((MutableList<TextLayoutResult>) -> Boolean)? = null

    private fun generateSemantics(text: String): SemanticsConfiguration {
        var localSemanticsTextLayoutResult = semanticsTextLayoutResult
        if (localSemanticsTextLayoutResult == null) {
            localSemanticsTextLayoutResult = { textLayoutResult ->
                val layout = layoutCache.slowCreateTextLayoutResultOrNull()?.also {
                    textLayoutResult.add(it)
                }
                layout != null
                false
            }
            semanticsTextLayoutResult = localSemanticsTextLayoutResult
        }
        return SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = false
            it.isClearingSemantics = false
            it.text = AnnotatedString(text)
            it.getTextLayoutResult(action = localSemanticsTextLayoutResult)
        }
    }

    override val semanticsConfiguration: SemanticsConfiguration
        get() {
            var localSemantics = _semanticsConfiguration
            if (localSemantics == null) {
                localSemantics = generateSemantics(text)
                _semanticsConfiguration = localSemantics
            }
            return localSemantics
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val layoutCache = getLayoutCache(this)

        val didChangeLayout = layoutCache.layoutWithConstraints(constraints, layoutDirection)
        // ensure measure restarts when hasStaleResolvedFonts by reading in measure
        layoutCache.observeFontChanges
        val paragraph = layoutCache.paragraph!!
        val layoutSize = layoutCache.layoutSize

        if (didChangeLayout) {
            invalidateLayer()
            baselineCache = mapOf(
                FirstBaseline to paragraph.firstBaseline.roundToInt(),
                LastBaseline to paragraph.lastBaseline.roundToInt()
            )
        }

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable = measurable.measure(
            Constraints.fixed(
                layoutSize.width,
                layoutSize.height
            )
        )

        return layout(
            layoutSize.width,
            layoutSize.height,
            baselineCache!!
        ) {
            // this is basically a graphicsLayer
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return getLayoutCache(this).minIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeightAt(width, layoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = getLayoutCache(this).maxIntrinsicWidth

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = getLayoutCache(this).intrinsicHeightAt(width, layoutDirection)

    @OptIn(ExperimentalTextApi::class)
    override fun ContentDrawScope.draw() {
        val localParagraph = requireNotNull(layoutCache.paragraph)
        drawIntoCanvas { canvas ->
            val willClip = layoutCache.didOverflow
            if (willClip) {
                val width = layoutCache.layoutSize.width.toFloat()
                val height = layoutCache.layoutSize.height.toFloat()
                val bounds = Rect(Offset.Zero, Size(width, height))
                canvas.save()
                canvas.clipRect(bounds)
            }
            try {
                val textDecoration = style.textDecoration ?: TextDecoration.None
                val shadow = style.shadow ?: Shadow.None
                val drawStyle = style.drawStyle ?: Fill
                val brush = style.brush
                if (brush != null) {
                    val alpha = style.alpha
                    localParagraph.paint(
                        canvas = canvas,
                        brush = brush,
                        alpha = alpha,
                        shadow = shadow,
                        drawStyle = drawStyle,
                        textDecoration = textDecoration
                    )
                } else {
                    val color = style.color
                    localParagraph.paint(
                        canvas = canvas,
                        color = color,
                        shadow = shadow,
                        textDecoration = textDecoration,
                        drawStyle = drawStyle
                    )
                }
            } finally {
                if (willClip) {
                    canvas.restore()
                }
            }
        }
    }
}
