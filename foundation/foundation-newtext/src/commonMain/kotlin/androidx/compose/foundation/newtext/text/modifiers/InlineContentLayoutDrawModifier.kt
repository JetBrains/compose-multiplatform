package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Modifier that does Layout and Draw for [InlineContentLayoutDrawParams]
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class InlineContentLayoutDrawModifier(
    params: InlineContentLayoutDrawParams
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode {
    private lateinit var lastTextDelegate: MultiParagraphPlaceholderLayoutCache
    private var lastTextLayoutResult: TextLayoutResult? by mutableStateOf(null)
    private var textDelegateDirty = true

    private var params: InlineContentLayoutDrawParams = params
        set(value) {
            textDelegateDirty = true
            field = value
            // TODO: calculate if only draw changed
            invalidateLayout()
        }

    private fun invalidateDraw() {
    }

    private fun invalidateLayout() {
        forceRemeasure()
    }

    fun update(params: InlineContentLayoutDrawParams) {
        this.params = params
    }

    private fun getCurrentTextDelegate(density: Density): MultiParagraphPlaceholderLayoutCache {
        return if (!textDelegateDirty) {
            lastTextDelegate
        } else {
            val textDelegate = MultiParagraphPlaceholderLayoutCache(params, density)
            lastTextDelegate = textDelegate
            textDelegateDirty = false
            textDelegate
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val td = getCurrentTextDelegate(this)

        // TODO: Detect if this is only a constraints change and compare intrinsic and old/new
        //  constraints

        // Otherwise, we expect that all restarts lead to a new text layout
        val textLayoutResult = td.layout(constraints, layoutDirection)

        params.onTextLayout?.let { onTextLayout ->
            onTextLayout(textLayoutResult)
            // TODO: figure out how to expose this to selection here
        }

        // first share the placeholders
        params.onPlaceholderLayout?.invoke(textLayoutResult.placeholderRects)

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable = measurable.measure(
            Constraints.fixed(
                textLayoutResult.size.width,
                textLayoutResult.size.height
            )
        )

        lastTextLayoutResult = textLayoutResult

        return layout(
            textLayoutResult.size.width,
            textLayoutResult.size.height,
            mapOf(
                FirstBaseline to textLayoutResult.firstBaseline.roundToInt(),
                LastBaseline to textLayoutResult.lastBaseline.roundToInt()
            )
        ) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td.minIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td.maxIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    override fun ContentDrawScope.draw() {
        drawIntoCanvas { canvas ->
            lastTextLayoutResult?.let { textLayout ->
                TextPainter.paint(canvas, textLayout)
            }
            drawContent()
        }
    }
}