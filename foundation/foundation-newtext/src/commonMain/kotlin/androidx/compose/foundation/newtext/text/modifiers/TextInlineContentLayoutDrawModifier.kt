package androidx.compose.foundation.newtext.text.modifiers

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
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayout
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Modifier that does Layout and Draw for [TextInlineContentLayoutDrawParams]
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class TextInlineContentLayoutDrawModifier(
    params: TextInlineContentLayoutDrawParams
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode {
    private var layoutCache: MultiParagraphLayoutCache? = null
    private var textDelegateDirty = true

    private var params: TextInlineContentLayoutDrawParams = params
        set(value) {
            layoutCache?.let { cache ->
                if (cache.equalForLayout(value) || cache.equalForCallbacks(value)) {
                    textDelegateDirty = true
                    invalidateLayout()
                }
            }
            field = value
            // if we set params, always redraw.
            invalidateDraw()
        }

    fun update(params: TextInlineContentLayoutDrawParams) {
        this.params = params
    }

    private fun getOrUpdateTextDelegateInLayout(
        density: Density
    ): MultiParagraphLayoutCache {
        val localLayoutCache = layoutCache
        return if (!textDelegateDirty && localLayoutCache != null) {
            localLayoutCache
        } else {
            val textDelegate = MultiParagraphLayoutCache(params, density)
            this.layoutCache = textDelegate
            textDelegateDirty = false
            textDelegate
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val td = getOrUpdateTextDelegateInLayout(this)

        // Otherwise, we expect that all restarts lead to a new text layout
        val didChangeLayout = td.layoutWithConstraints(constraints, layoutDirection)
        val textLayoutResult = td.layout

        if (didChangeLayout) {
            invalidateDraw()
            params.onTextLayout?.let { onTextLayout ->
                onTextLayout(textLayoutResult)
            }
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

        return layout(
            textLayoutResult.size.width,
            textLayoutResult.size.height,
            mapOf(
                FirstBaseline to textLayoutResult.firstBaseline.roundToInt(),
                LastBaseline to textLayoutResult.lastBaseline.roundToInt()
            )
        ) {
            // this is effictively graphicsLayer
            placeable.placeWithLayer(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getOrUpdateTextDelegateInLayout(this)
        return td.minIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return getOrUpdateTextDelegateInLayout(this)
            .intrinsicHeightAt(width, layoutDirection)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getOrUpdateTextDelegateInLayout(this)
        return td.maxIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return getOrUpdateTextDelegateInLayout(this)
            .intrinsicHeightAt(width, layoutDirection)
    }

    override fun ContentDrawScope.draw() {
        drawIntoCanvas { canvas ->
            layoutCache?.layout?.let { textLayout ->
                TextPainter.paint(canvas, textLayout)
            }
            drawContent()
        }
    }
}