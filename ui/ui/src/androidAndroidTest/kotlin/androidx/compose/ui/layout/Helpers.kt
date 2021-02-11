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

import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import kotlin.math.max
import kotlin.math.min

@Suppress("UNCHECKED_CAST")
internal fun createDelegate(
    root: LayoutNode,
    firstMeasureCompleted: Boolean = true
): MeasureAndLayoutDelegate {
    val delegate = MeasureAndLayoutDelegate(root)
    root.attach(
        mock {
            on { measureIteration } doAnswer {
                delegate.measureIteration
            }
            on { onRequestMeasure(any()) } doAnswer {
                delegate.requestRemeasure(it.arguments[0] as LayoutNode)
                Unit
            }
            on { measureAndLayout() } doAnswer {
                delegate.measureAndLayout()
                Unit
            }
            on { snapshotObserver } doAnswer {
                OwnerSnapshotObserver { it.invoke() }
            }
        }
    )
    if (firstMeasureCompleted) {
        delegate.updateRootConstraints(
            defaultRootConstraints()
        )
        Truth.assertThat(delegate.measureAndLayout()).isTrue()
    }
    return delegate
}

internal fun defaultRootConstraints() = Constraints(maxWidth = 100, maxHeight = 100)

internal fun assertNotRemeasured(node: LayoutNode, block: (LayoutNode) -> Unit) {
    val measuresCountBefore = node.measuresCount
    block(node)
    Truth.assertThat(node.measuresCount).isEqualTo(measuresCountBefore)
    assertMeasuredAndLaidOut(node)
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
    assertMeasuredAndLaidOut(node)
}

internal fun assertMeasureRequired(node: LayoutNode) {
    Truth.assertThat(node.layoutState).isEqualTo(LayoutNode.LayoutState.NeedsRemeasure)
}

internal fun assertMeasuredAndLaidOut(node: LayoutNode) {
    Truth.assertThat(node.layoutState).isEqualTo(LayoutNode.LayoutState.Ready)
}

internal fun assertLayoutRequired(node: LayoutNode) {
    Truth.assertThat(node.layoutState).isEqualTo(LayoutNode.LayoutState.NeedsRelayout)
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
