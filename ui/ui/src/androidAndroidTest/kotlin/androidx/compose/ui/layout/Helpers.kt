/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth
import kotlin.math.max
import kotlin.math.min

@Suppress("UNCHECKED_CAST")
internal fun createDelegate(
    root: LayoutNode,
    firstMeasureCompleted: Boolean = true
): MeasureAndLayoutDelegate {
    val delegate = MeasureAndLayoutDelegate(root)
    root.attach(FakeOwner(delegate))
    if (firstMeasureCompleted) {
        delegate.updateRootConstraints(
            defaultRootConstraints()
        )
        Truth.assertThat(delegate.measureAndLayout()).isTrue()
    }
    return delegate
}

@OptIn(ExperimentalComposeUiApi::class)
private class FakeOwner(
    val delegate: MeasureAndLayoutDelegate
) : Owner {
    override val measureIteration: Long
        get() = delegate.measureIteration

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            delegate.requestLookaheadRemeasure(layoutNode)
        } else {
            delegate.requestRemeasure(layoutNode)
        }
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        delegate.measureAndLayout()
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        delegate.measureAndLayout(layoutNode, constraints)
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
        delegate.forceMeasureTheSubtree(layoutNode)
    }

    override val snapshotObserver: OwnerSnapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onEndApplyChanges() {
        TODO("Not yet implemented")
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {}

    @OptIn(InternalCoreApi::class)
    override var showLayoutBounds: Boolean = false

    override fun onAttach(node: LayoutNode) {}
    override fun onDetach(node: LayoutNode) {}

    override val root: LayoutNode
        get() = TODO("Not yet implemented")
    override val sharedDrawScope: LayoutNodeDrawScope
        get() = TODO("Not yet implemented")
    override val rootForTest: RootForTest
        get() = TODO("Not yet implemented")
    override val hapticFeedBack: HapticFeedback
        get() = TODO("Not yet implemented")
    override val inputModeManager: InputModeManager
        get() = TODO("Not yet implemented")
    override val clipboardManager: ClipboardManager
        get() = TODO("Not yet implemented")
    override val accessibilityManager: AccessibilityManager
        get() = TODO("Not yet implemented")
    override val textToolbar: TextToolbar
        get() = TODO("Not yet implemented")
    override val density: Density
        get() = TODO("Not yet implemented")
    override val textInputService: TextInputService
        get() = TODO("Not yet implemented")
    override val pointerIconService: PointerIconService
        get() = TODO("Not yet implemented")
    override val focusManager: FocusManager
        get() = TODO("Not yet implemented")
    override val windowInfo: WindowInfo
        get() = TODO("Not yet implemented")

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override val fontLoader: Font.ResourceLoader
        get() = TODO("Not yet implemented")
    override val fontFamilyResolver: FontFamily.Resolver
        get() = TODO("Not yet implemented")
    override val layoutDirection: LayoutDirection
        get() = TODO("Not yet implemented")
    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")
    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")
    override val autofill: Autofill
        get() = TODO("Not yet implemented")

    override fun createLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) =
        TODO("Not yet implemented")

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) = TODO("Not yet implemented")

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        TODO("Not yet implemented")
    }

    override fun calculatePositionInWindow(localPosition: Offset) = TODO("Not yet implemented")
    override fun calculateLocalPosition(positionInWindow: Offset) = TODO("Not yet implemented")
    override fun requestFocus() = TODO("Not yet implemented")
    override fun onSemanticsChange() = TODO("Not yet implemented")
    override fun getFocusDirection(keyEvent: KeyEvent) = TODO("Not yet implemented")
}

internal fun defaultRootConstraints() = Constraints(maxWidth = 100, maxHeight = 100)

internal fun assertNotRemeasured(node: LayoutNode, block: (LayoutNode) -> Unit) {
    val measuresCountBefore = node.measuresCount
    block(node)
    Truth.assertThat(node.measuresCount).isEqualTo(measuresCountBefore)
}

internal fun assertRemeasured(
    node: LayoutNode,
    times: Int = 1,
    withDirection: LayoutDirection? = null,
    block: (LayoutNode) -> Unit
) {
    val measuresCountBefore = node.measuresCount
    block(node)
    Truth.assertThat(node.measuresCount).isEqualTo(measuresCountBefore + times)
    if (withDirection != null) {
        Truth.assertThat(node.measuredWithLayoutDirection).isEqualTo(withDirection)
    }
    assertMeasuredAndLaidOut(node)
}

internal fun assertRelaidOut(node: LayoutNode, times: Int = 1, block: (LayoutNode) -> Unit) {
    val layoutsCountBefore = node.layoutsCount
    block(node)
    Truth.assertThat(node.layoutsCount).isEqualTo(layoutsCountBefore + times)
    assertMeasuredAndLaidOut(node)
}

internal fun assertNotRelaidOut(node: LayoutNode, block: (LayoutNode) -> Unit) {
    val layoutsCountBefore = node.layoutsCount
    block(node)
    Truth.assertThat(node.layoutsCount).isEqualTo(layoutsCountBefore)
}

internal fun assertMeasureRequired(node: LayoutNode) {
    Truth.assertThat(node.measurePending).isTrue()
}

internal fun assertMeasuredAndLaidOut(node: LayoutNode) {
    Truth.assertThat(node.layoutState).isEqualTo(LayoutNode.LayoutState.Idle)
    Truth.assertThat(node.layoutPending).isFalse()
    Truth.assertThat(node.measurePending).isFalse()
}

internal fun assertLayoutRequired(node: LayoutNode) {
    Truth.assertThat(node.layoutPending).isTrue()
}

internal fun assertRemeasured(
    modifier: SpyLayoutModifier,
    block: () -> Unit
) {
    val measuresCountBefore = modifier.measuresCount
    block()
    Truth.assertThat(modifier.measuresCount).isEqualTo(measuresCountBefore + 1)
}

internal fun assertNotRemeasured(
    modifier: SpyLayoutModifier,
    block: () -> Unit
) {
    val measuresCountBefore = modifier.measuresCount
    block()
    Truth.assertThat(modifier.measuresCount).isEqualTo(measuresCountBefore)
}

internal fun assertRelaidOut(
    modifier: SpyLayoutModifier,
    block: () -> Unit
) {
    val layoutsCountBefore = modifier.layoutsCount
    block()
    Truth.assertThat(modifier.layoutsCount).isEqualTo(layoutsCountBefore + 1)
}

internal fun root(block: LayoutNode.() -> Unit = {}): LayoutNode {
    return node(block)
}

internal fun node(block: LayoutNode.() -> Unit = {}): LayoutNode {
    return LayoutNode().apply {
        measurePolicy = MeasureInMeasureBlock()
        block.invoke(this)
    }
}

internal fun LayoutNode.add(child: LayoutNode) = insertAt(children.count(), child)

internal fun LayoutNode.measureInLayoutBlock() {
    measurePolicy = MeasureInLayoutBlock()
}

internal fun LayoutNode.doNotMeasure() {
    measurePolicy = NoMeasure()
}

internal fun LayoutNode.queryAlignmentLineDuringMeasure() {
    (measurePolicy as SmartMeasurePolicy).queryAlignmentLinesDuringMeasure = true
}

internal fun LayoutNode.runDuringMeasure(block: () -> Unit) {
    (measurePolicy as SmartMeasurePolicy).preMeasureCallback = block
}

internal fun LayoutNode.runDuringLayout(block: () -> Unit) {
    (measurePolicy as SmartMeasurePolicy).preLayoutCallback = block
}

internal val LayoutNode.first: LayoutNode get() = children.first()
internal val LayoutNode.second: LayoutNode get() = children[1]
internal val LayoutNode.measuresCount: Int
    get() = (measurePolicy as SmartMeasurePolicy).measuresCount
internal val LayoutNode.layoutsCount: Int
    get() = (measurePolicy as SmartMeasurePolicy).layoutsCount
internal var LayoutNode.wrapChildren: Boolean
    get() = (measurePolicy as SmartMeasurePolicy).wrapChildren
    set(value) {
        (measurePolicy as SmartMeasurePolicy).wrapChildren = value
    }
internal val LayoutNode.measuredWithLayoutDirection: LayoutDirection
    get() = (measurePolicy as SmartMeasurePolicy).measuredLayoutDirection!!
internal var LayoutNode.size: Int?
    get() = (measurePolicy as SmartMeasurePolicy).size
    set(value) {
        (measurePolicy as SmartMeasurePolicy).size = value
    }
internal var LayoutNode.childrenDirection: LayoutDirection?
    get() = (measurePolicy as SmartMeasurePolicy).childrenLayoutDirection
    set(value) {
        (measurePolicy as SmartMeasurePolicy).childrenLayoutDirection = value
    }
internal var LayoutNode.shouldPlaceChildren: Boolean
    get() = (measurePolicy as SmartMeasurePolicy).shouldPlaceChildren
    set(value) {
        (measurePolicy as SmartMeasurePolicy).shouldPlaceChildren = value
    }

internal val TestAlignmentLine = HorizontalAlignmentLine(::min)

internal abstract class SmartMeasurePolicy : LayoutNode.NoIntrinsicsMeasurePolicy("") {
    var measuresCount = 0
        protected set
    var layoutsCount = 0
        protected set
    open var wrapChildren = false
    open var queryAlignmentLinesDuringMeasure = false
    var preMeasureCallback: (() -> Unit)? = null
    var preLayoutCallback: (() -> Unit)? = null
    var measuredLayoutDirection: LayoutDirection? = null
        protected set
    var childrenLayoutDirection: LayoutDirection? = null

    // child size is used when null
    var size: Int? = null
    var shouldPlaceChildren = true
}

internal class MeasureInMeasureBlock : SmartMeasurePolicy() {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        measuresCount++
        preMeasureCallback?.invoke()
        preMeasureCallback = null
        val childConstraints = if (size == null) {
            constraints
        } else {
            val size = size!!
            constraints.copy(maxWidth = size, maxHeight = size)
        }
        val placeables = measurables.map {
            it.measure(childConstraints)
        }
        if (queryAlignmentLinesDuringMeasure) {
            placeables.forEach { it[TestAlignmentLine] }
        }
        var maxWidth = 0
        var maxHeight = 0
        if (!wrapChildren) {
            maxWidth = childConstraints.maxWidth
            maxHeight = childConstraints.maxHeight
        } else {
            placeables.forEach { placeable ->
                maxWidth = max(placeable.width, maxWidth)
                maxHeight = max(placeable.height, maxHeight)
            }
        }
        return layout(maxWidth, maxHeight) {
            layoutsCount++
            preLayoutCallback?.invoke()
            preLayoutCallback = null
            if (shouldPlaceChildren) {
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

internal class MeasureInLayoutBlock : SmartMeasurePolicy() {

    override var wrapChildren: Boolean
        get() = false
        set(value) {
            if (value) {
                throw IllegalArgumentException("MeasureInLayoutBlock always fills the parent size")
            }
        }

    override var queryAlignmentLinesDuringMeasure: Boolean
        get() = false
        set(value) {
            if (value) {
                throw IllegalArgumentException(
                    "MeasureInLayoutBlock cannot query alignment " +
                        "lines during measure"
                )
            }
        }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        measuresCount++
        preMeasureCallback?.invoke()
        preMeasureCallback = null
        val childConstraints = if (size == null) {
            constraints
        } else {
            val size = size!!
            constraints.copy(maxWidth = size, maxHeight = size)
        }
        return layout(childConstraints.maxWidth, childConstraints.maxHeight) {
            preLayoutCallback?.invoke()
            preLayoutCallback = null
            layoutsCount++
            measurables.forEach {
                val placeable = it.measure(childConstraints)
                if (shouldPlaceChildren) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

internal class NoMeasure : SmartMeasurePolicy() {

    override var queryAlignmentLinesDuringMeasure: Boolean
        get() = false
        set(value) {
            if (value) {
                throw IllegalArgumentException(
                    "MeasureInLayoutBlock cannot query alignment " +
                        "lines during measure"
                )
            }
        }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        measuresCount++
        preMeasureCallback?.invoke()
        preMeasureCallback = null

        val width = size ?: if (!wrapChildren) constraints.maxWidth else constraints.minWidth
        val height = size ?: if (!wrapChildren) constraints.maxHeight else constraints.minHeight
        return layout(width, height) {
            layoutsCount++
            preLayoutCallback?.invoke()
            preLayoutCallback = null
        }
    }
}

internal class SpyLayoutModifier : LayoutModifier {
    var measuresCount = 0
    var layoutsCount = 0

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        measuresCount++
        return layout(constraints.maxWidth, constraints.maxHeight) {
            layoutsCount++
            measurable.measure(constraints).placeRelative(0, 0)
        }
    }
}
