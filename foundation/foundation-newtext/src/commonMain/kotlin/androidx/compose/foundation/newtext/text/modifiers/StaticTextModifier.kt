package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ObserverNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.invalidateLayout
import androidx.compose.ui.node.invalidateMeasurements
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Modifier that does Layout and Draw for [StaticTextLayoutDrawParams]
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class StaticTextModifier(
    params: StaticTextLayoutDrawParams
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode,
    SemanticsModifierNode, ObserverNode {
    private var baselineCache: Map<AlignmentLine, Int>? = null
    private var layoutCache: MultiParagraphLayoutCache? = null
    private var textDelegateDirty = true

    internal var params: StaticTextLayoutDrawParams = params
        set(value) {
            validate(value)
            field = value
            layoutCache?.let { cache ->
                val diff = cache.diff(value)
                if (diff.hasSemanticsDiffs) {
                    _semanticsConfiguration = null
                    invalidateSemantics()
                }
                if (diff.hasLayoutDiffs || diff.hasCallbackDiffs) {
                    textDelegateDirty = true
                    invalidateMeasurements()
                }
                if (diff.anyDiffs) {
                    // if anything changed we redraw
                    invalidateDraw()
                }
                return
            }

            // if no layout has happened, 🎶 just invalidate everything 🎶
            // we don't expect to hit this often, but if we do don't keep anything around from
            // the previous params and restart all passes ⚽
            _semanticsConfiguration = null
            textDelegateDirty = true
            invalidateSemantics()
            invalidateMeasurements()
            invalidateDraw()
        }

    private var _semanticsConfiguration: SemanticsConfiguration? = null

    private var semanticsTextLayoutResult: ((MutableList<TextLayoutResult>) -> Boolean)? = null

    private fun generateSemantics(text: AnnotatedString): SemanticsConfiguration {
        var localSemanticsTextLayoutResult = semanticsTextLayoutResult
        if (localSemanticsTextLayoutResult == null) {
            localSemanticsTextLayoutResult = { textLayoutResult ->
                val layout = layoutCache?.layoutOrNull?.also {
                    textLayoutResult.add(it)
                }
                layout != null
            }
            semanticsTextLayoutResult = localSemanticsTextLayoutResult
        }
        return SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = false
            it.isClearingSemantics = false
            it.text = text
            it.getTextLayoutResult(action = localSemanticsTextLayoutResult)
        }
    }

    override val semanticsConfiguration: SemanticsConfiguration
        get() {
            var localSemantics = _semanticsConfiguration
            if (localSemantics == null) {
                localSemantics = generateSemantics(params.text)
                _semanticsConfiguration = localSemantics
            }
            return localSemantics
        }

    private fun validate(params: StaticTextLayoutDrawParams) {
        validateMinMaxLines(params.minLines, params.maxLines)
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

    fun measureNonExtension(
        measureScope: MeasureScope,
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return measureScope.measure(measurable, constraints)
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val td = getOrUpdateTextDelegateInLayout(this)

        val didChangeLayout = td.layoutWithConstraints(constraints, layoutDirection)
        val textLayoutResult = td.layout

        observeReads {
            textLayoutResult.multiParagraph.intrinsics.hasStaleResolvedFonts
        }

        if (didChangeLayout) {
            invalidateLayer()
            params.onTextLayout?.invoke(textLayoutResult)
            params.selectionController?.updateTextLayout(textLayoutResult)
            baselineCache = mapOf(
                FirstBaseline to textLayoutResult.firstBaseline.roundToInt(),
                LastBaseline to textLayoutResult.lastBaseline.roundToInt()
            )
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
            baselineCache!!
        ) {
            // this is basically a graphicsLayer
            placeable.place(0, 0)
        }
    }

    fun minIntrinsicWidthNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return intrinsicMeasureScope.minIntrinsicWidth(measurable, height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getOrUpdateTextDelegateInLayout(this)
        return td.minIntrinsicWidth
    }

    fun minIntrinsicHeightNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return intrinsicMeasureScope.minIntrinsicHeight(measurable, width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return getOrUpdateTextDelegateInLayout(this)
            .intrinsicHeightAt(width, layoutDirection)
    }

    fun maxIntrinsicWidthNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return intrinsicMeasureScope.maxIntrinsicWidth(measurable, height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getOrUpdateTextDelegateInLayout(this)
        return td.maxIntrinsicWidth
    }

    fun maxIntrinsicHeightNonExtension(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return intrinsicMeasureScope.maxIntrinsicHeight(measurable, width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return getOrUpdateTextDelegateInLayout(this)
            .intrinsicHeightAt(width, layoutDirection)
    }

    fun drawNonExtension(
        contentDrawScope: ContentDrawScope
    ) {
        return contentDrawScope.draw()
    }

    override fun ContentDrawScope.draw() {
        params.selectionController?.draw(this)
        drawIntoCanvas { canvas ->
            TextPainter.paint(canvas, requireNotNull(layoutCache?.layout))
        }
        if (!params.placeholders.isNullOrEmpty()) {
            drawContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        params.selectionController?.updateGlobalPosition(coordinates)
    }

    override fun onObservedReadsChanged() {
        invalidateLayout()
    }
}