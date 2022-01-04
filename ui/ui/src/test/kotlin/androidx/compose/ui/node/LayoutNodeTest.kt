/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.node

import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsEntity
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LayoutNodeTest {
    // Ensure that attach and detach work properly
    @Test
    fun layoutNodeAttachDetach() {
        val node = LayoutNode()
        assertNull(node.owner)

        val owner = MockOwner()
        node.attach(owner)
        assertEquals(owner, node.owner)
        assertTrue(node.isAttached)

        assertEquals(1, owner.onAttachParams.count { it === node })

        node.detach()
        assertNull(node.owner)
        assertFalse(node.isAttached)
        assertEquals(1, owner.onDetachParams.count { it === node })
    }

    // Ensure that LayoutNode's children are ordered properly through add, remove, move
    @Test
    fun layoutNodeChildrenOrder() {
        val (node, child1, child2) = createSimpleLayout()
        assertEquals(2, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])
        assertEquals(0, child1.children.size)
        assertEquals(0, child2.children.size)

        node.removeAt(index = 0, count = 1)
        assertEquals(1, node.children.size)
        assertEquals(child2, node.children[0])

        node.insertAt(index = 0, instance = child1)
        assertEquals(2, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])

        node.removeAt(index = 0, count = 2)
        assertEquals(0, node.children.size)

        val child3 = LayoutNode()
        val child4 = LayoutNode()

        node.insertAt(0, child1)
        node.insertAt(1, child2)
        node.insertAt(2, child3)
        node.insertAt(3, child4)

        assertEquals(4, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])
        assertEquals(child3, node.children[2])
        assertEquals(child4, node.children[3])

        node.move(from = 3, count = 1, to = 0)
        assertEquals(4, node.children.size)
        assertEquals(child4, node.children[0])
        assertEquals(child1, node.children[1])
        assertEquals(child2, node.children[2])
        assertEquals(child3, node.children[3])

        node.move(from = 0, count = 2, to = 3)
        assertEquals(4, node.children.size)
        assertEquals(child2, node.children[0])
        assertEquals(child3, node.children[1])
        assertEquals(child4, node.children[2])
        assertEquals(child1, node.children[3])
    }

    // Ensure that attach of a LayoutNode connects all children
    @Test
    fun layoutNodeAttach() {
        val (node, child1, child2) = createSimpleLayout()

        val owner = MockOwner()
        node.attach(owner)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        assertEquals(1, owner.onAttachParams.count { it === node })
        assertEquals(1, owner.onAttachParams.count { it === child1 })
        assertEquals(1, owner.onAttachParams.count { it === child2 })
    }

    // Ensure that detach of a LayoutNode detaches all children
    @Test
    fun layoutNodeDetach() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = MockOwner()
        node.attach(owner)
        owner.onAttachParams.clear()
        node.detach()

        assertEquals(node, child1.parent)
        assertEquals(node, child2.parent)
        assertNull(node.owner)
        assertNull(child1.owner)
        assertNull(child2.owner)

        assertEquals(1, owner.onDetachParams.count { it === node })
        assertEquals(1, owner.onDetachParams.count { it === child1 })
        assertEquals(1, owner.onDetachParams.count { it === child2 })
    }

    // Ensure that dropping a child also detaches it
    @Test
    fun layoutNodeDropDetaches() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = MockOwner()
        node.attach(owner)

        node.removeAt(0, 1)
        assertEquals(owner, node.owner)
        assertNull(child1.owner)
        assertEquals(owner, child2.owner)

        assertEquals(0, owner.onDetachParams.count { it === node })
        assertEquals(1, owner.onDetachParams.count { it === child1 })
        assertEquals(0, owner.onDetachParams.count { it === child2 })
    }

    // Ensure that adopting a child also attaches it
    @Test
    fun layoutNodeAdoptAttaches() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = MockOwner()
        node.attach(owner)

        node.removeAt(0, 1)

        node.insertAt(1, child1)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        assertEquals(1, owner.onAttachParams.count { it === node })
        assertEquals(2, owner.onAttachParams.count { it === child1 })
        assertEquals(1, owner.onAttachParams.count { it === child2 })
    }

    @Test
    fun childAdd() {
        val node = LayoutNode()
        val owner = MockOwner()
        node.attach(owner)
        assertEquals(1, owner.onAttachParams.count { it === node })

        val child = LayoutNode()
        node.insertAt(0, child)
        assertEquals(1, owner.onAttachParams.count { it === child })
        assertEquals(1, node.children.size)
        assertEquals(node, child.parent)
        assertEquals(owner, child.owner)
    }

    @Test
    fun childCount() {
        val node = LayoutNode()
        assertEquals(0, node.children.size)
        node.insertAt(0, LayoutNode())
        assertEquals(1, node.children.size)
    }

    @Test
    fun childGet() {
        val node = LayoutNode()
        val child = LayoutNode()
        node.insertAt(0, child)
        assertEquals(child, node.children[0])
    }

    @Test
    fun noMove() {
        val (layout, child1, child2) = createSimpleLayout()
        layout.move(0, 0, 1)
        assertEquals(child1, layout.children[0])
        assertEquals(child2, layout.children[1])
    }

    @Test
    fun childRemove() {
        val node = LayoutNode()
        val owner = MockOwner()
        node.attach(owner)
        val child = LayoutNode()
        node.insertAt(0, child)
        node.removeAt(index = 0, count = 1)
        assertEquals(1, owner.onDetachParams.count { it === child })
        assertEquals(0, node.children.size)
        assertEquals(null, child.parent)
        assertNull(child.owner)
    }

    // Ensure that depth is as expected
    @Test
    fun depth() {
        val root = LayoutNode()
        val (child, grand1, grand2) = createSimpleLayout()
        root.insertAt(0, child)

        val owner = MockOwner()
        root.attach(owner)

        assertEquals(0, root.depth)
        assertEquals(1, child.depth)
        assertEquals(2, grand1.depth)
        assertEquals(2, grand2.depth)
    }

    // layoutNode hierarchy should be set properly when a LayoutNode is a child of a LayoutNode
    @Test
    fun directLayoutNodeHierarchy() {
        val layoutNode = LayoutNode()
        val childLayoutNode = LayoutNode()
        layoutNode.insertAt(0, childLayoutNode)

        assertNull(layoutNode.parent)
        assertEquals(layoutNode, childLayoutNode.parent)
        val layoutNodeChildren = layoutNode.children
        assertEquals(1, layoutNodeChildren.size)
        assertEquals(childLayoutNode, layoutNodeChildren[0])

        layoutNode.removeAt(index = 0, count = 1)
        assertNull(childLayoutNode.parent)
    }

    @Test
    fun testLayoutNodeAdd() {
        val (layout, child1, child2) = createSimpleLayout()
        val inserted = LayoutNode()
        layout.insertAt(0, inserted)
        val children = layout.children
        assertEquals(3, children.size)
        assertEquals(inserted, children[0])
        assertEquals(child1, children[1])
        assertEquals(child2, children[2])
    }

    @Test
    fun testLayoutNodeRemove() {
        val (layout, child1, _) = createSimpleLayout()
        val child3 = LayoutNode()
        val child4 = LayoutNode()
        layout.insertAt(2, child3)
        layout.insertAt(3, child4)
        layout.removeAt(index = 1, count = 2)

        val children = layout.children
        assertEquals(2, children.size)
        assertEquals(child1, children[0])
        assertEquals(child4, children[1])
    }

    @Test
    fun testMoveChildren() {
        val (layout, child1, child2) = createSimpleLayout()
        val child3 = LayoutNode()
        val child4 = LayoutNode()
        layout.insertAt(2, child3)
        layout.insertAt(3, child4)

        layout.move(from = 2, to = 1, count = 2)

        val children = layout.children
        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child3, children[1])
        assertEquals(child4, children[2])
        assertEquals(child2, children[3])

        layout.move(from = 1, to = 3, count = 2)

        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child2, children[1])
        assertEquals(child3, children[2])
        assertEquals(child4, children[3])
    }

    @Test
    fun testPxGlobalToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(MockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100
        val y0 = 10
        val x1 = 50
        val y1 = 80
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = Offset(250f, 300f)

        val expectedX = globalPosition.x - x0.toFloat() - x1.toFloat()
        val expectedY = globalPosition.y - y0.toFloat() - y1.toFloat()
        val expectedPosition = Offset(expectedX, expectedY)

        val result = node1.coordinates.windowToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxGlobalToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(MockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100
        val y0 = 10
        val x1 = 50
        val y1 = 80
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = Offset(250f, 300f)

        val expectedX = globalPosition.x - x0.toFloat() - x1.toFloat()
        val expectedY = globalPosition.y - y0.toFloat() - y1.toFloat()
        val expectedPosition = Offset(expectedX, expectedY)

        val result = node1.coordinates.windowToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(MockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100
        val y0 = 10
        val x1 = 50
        val y1 = 80
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = Offset(5f, 15f)

        val expectedX = localPosition.x + x0.toFloat() + x1.toFloat()
        val expectedY = localPosition.y + y0.toFloat() + y1.toFloat()
        val expectedPosition = Offset(expectedX, expectedY)

        val result = node1.coordinates.localToWindow(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxLocalToGlobal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(MockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100
        val y0 = 10
        val x1 = 50
        val y1 = 80
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = Offset(5f, 15f)

        val expectedX = localPosition.x + x0.toFloat() + x1.toFloat()
        val expectedY = localPosition.y + y0.toFloat() + y1.toFloat()
        val expectedPosition = Offset(expectedX, expectedY)

        val result = node1.coordinates.localToWindow(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobalUsesOwnerPosition() {
        val node = ZeroSizedLayoutNode()
        node.attach(MockOwner(IntOffset(20, 20)))
        node.place(100, 10)

        val result = node.coordinates.localToWindow(Offset.Zero)

        assertEquals(Offset(120f, 30f), result)
    }

    @Test
    fun testIntPxLocalToGlobalUsesOwnerPosition() {
        val node = ZeroSizedLayoutNode()
        node.attach(MockOwner(IntOffset(20, 20)))
        node.place(100, 10)

        val result = node.coordinates.localToWindow(Offset.Zero)

        assertEquals(Offset(120f, 30f), result)
    }

    @Test
    fun testChildToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(MockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x1 = 50
        val y1 = 80
        node0.place(100, 10)
        node1.place(x1, y1)

        val localPosition = Offset(5f, 15f)

        val expectedX = localPosition.x + x1.toFloat()
        val expectedY = localPosition.y + y1.toFloat()
        val expectedPosition = Offset(expectedX, expectedY)

        val result = node0.coordinates.localPositionOf(node1.coordinates, localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testLocalPositionOfWithSiblings() {
        val node0 = LayoutNode()
        node0.attach(MockOwner())
        val node1 = LayoutNode()
        val node2 = LayoutNode()
        node0.insertAt(0, node1)
        node0.insertAt(1, node2)
        node1.place(10, 20)
        node2.place(100, 200)

        val offset = node2.coordinates.localPositionOf(node1.coordinates, Offset(5f, 15f))
        assertEquals(Offset(-85f, -165f), offset)
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestorNoParent() {
        val owner = MockOwner()
        val node0 = LayoutNode()
        node0.attach(owner)
        val node1 = LayoutNode()
        node1.attach(owner)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            node1.coordinates.localPositionOf(node0.coordinates, Offset(5f, 15f))
        }
    }

    @Test
    fun testChildToLocalTheSameNode() {
        val node = LayoutNode()
        node.attach(MockOwner())
        val position = Offset(5f, 15f)

        val result = node.coordinates.localPositionOf(node.coordinates, position)

        assertEquals(position, result)
    }

    @Test
    fun testPositionRelativeToRoot() {
        val parent = ZeroSizedLayoutNode()
        parent.attach(MockOwner())
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        parent.place(-100, 10)
        child.place(50, 80)

        val actual = child.coordinates.positionInRoot()

        assertEquals(Offset(-50f, 90f), actual)
    }

    @Test
    fun testPositionRelativeToRootIsNotAffectedByOwnerPosition() {
        val parent = LayoutNode()
        parent.attach(MockOwner(IntOffset(20, 20)))
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        child.place(50, 80)

        val actual = child.coordinates.positionInRoot()

        assertEquals(Offset(50f, 80f), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithParent() {
        val parent = ZeroSizedLayoutNode()
        parent.attach(MockOwner())
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        parent.place(-100, 10)
        child.place(50, 80)

        val actual = parent.coordinates.localPositionOf(child.coordinates, Offset.Zero)

        assertEquals(Offset(50f, 80f), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithGrandParent() {
        val grandParent = ZeroSizedLayoutNode()
        grandParent.attach(MockOwner())
        val parent = ZeroSizedLayoutNode()
        val child = ZeroSizedLayoutNode()
        grandParent.insertAt(0, parent)
        parent.insertAt(0, child)
        grandParent.place(-7, 17)
        parent.place(23, -13)
        child.place(-3, 11)

        val actual = grandParent.coordinates.localPositionOf(child.coordinates, Offset.Zero)

        assertEquals(Offset(20f, -2f), actual)
    }

    // LayoutNode shouldn't allow adding beyond the count
    @Test
    fun testAddBeyondCurrent() {
        val node = LayoutNode()
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.insertAt(1, LayoutNode())
        }
    }

    // LayoutNode shouldn't allow adding below 0
    @Test
    fun testAddBelowZero() {
        val node = LayoutNode()
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.insertAt(-1, LayoutNode())
        }
    }

    // LayoutNode should error when removing at index < 0
    @Test
    fun testRemoveNegativeIndex() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.removeAt(-1, 1)
        }
    }

    // LayoutNode should error when removing at index > count
    @Test
    fun testRemoveBeyondIndex() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.removeAt(1, 1)
        }
    }

    // LayoutNode should error when removing at count < 0
    @Test
    fun testRemoveNegativeCount() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        Assert.assertThrows(IllegalArgumentException::class.java) {
            node.removeAt(0, -1)
        }
    }

    // LayoutNode should error when removing at count > entry count
    @Test
    fun testRemoveWithIndexBeyondSize() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.removeAt(0, 2)
        }
    }

    // LayoutNode should error when there aren't enough items
    @Test
    fun testRemoveWithIndexEqualToSize() {
        val node = LayoutNode()
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            node.removeAt(0, 1)
        }
    }

    // LayoutNode should allow removing two items
    @Test
    fun testRemoveTwoItems() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        node.insertAt(0, LayoutNode())
        node.removeAt(0, 2)
        assertEquals(0, node.children.size)
    }

    // The layout coordinates of a LayoutNode should be attached when
    // the layout node is attached.
    @Test
    fun coordinatesAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        val layoutModifier = Modifier.graphicsLayer { }
        layoutNode.modifier = layoutModifier
        assertFalse(layoutNode.coordinates.isAttached)
        assertFalse(layoutNode.coordinates.isAttached)
        layoutNode.attach(MockOwner())
        assertTrue(layoutNode.coordinates.isAttached)
        assertTrue(layoutNode.coordinates.isAttached)
        layoutNode.detach()
        assertFalse(layoutNode.coordinates.isAttached)
        assertFalse(layoutNode.coordinates.isAttached)
    }

    // The LayoutNodeWrapper should be reused when it has been replaced with the same type
    @Test
    fun layoutNodeWrapperSameWithReplacementModifier() {
        val layoutNode = LayoutNode()
        val layoutModifier = Modifier.graphicsLayer { }

        layoutNode.modifier = layoutModifier
        val oldLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper
        assertFalse(oldLayoutNodeWrapper.isAttached)

        layoutNode.attach(MockOwner())
        assertTrue(oldLayoutNodeWrapper.isAttached)

        layoutNode.modifier = Modifier.graphicsLayer { }
        val newLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper
        assertSame(newLayoutNodeWrapper, oldLayoutNodeWrapper)
    }

    // The LayoutNodeWrapper should be reused when it has been replaced with the same type,
    // even with multiple LayoutNodeWrappers for one modifier.
    @Test
    fun layoutNodeWrapperSameWithReplacementMultiModifier() {
        class TestModifier : DrawModifier, LayoutModifier {
            override fun ContentDrawScope.draw() {
                drawContent()
            }

            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ) = layout(0, 0) {}
        }

        val layoutNode = LayoutNode()

        layoutNode.modifier = TestModifier()
        val oldLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper
        val oldLayoutNodeWrapper2 = oldLayoutNodeWrapper.wrapped
        layoutNode.modifier = TestModifier()
        val newLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper
        val newLayoutNodeWrapper2 = newLayoutNodeWrapper.wrapped
        assertSame(newLayoutNodeWrapper, oldLayoutNodeWrapper)
        assertSame(newLayoutNodeWrapper2, oldLayoutNodeWrapper2)
    }

    // The LayoutNodeWrapper should be detached when it has been replaced.
    @Test
    fun layoutNodeWrapperAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        // 2 modifiers at the start
        val layoutModifier = Modifier.graphicsLayer { }.graphicsLayer { }

        layoutNode.modifier = layoutModifier
        val oldLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper
        val oldInnerLayoutNodeWrapper = oldLayoutNodeWrapper.wrapped
        assertFalse(oldLayoutNodeWrapper.isAttached)
        assertNotNull(oldInnerLayoutNodeWrapper)
        assertFalse(oldInnerLayoutNodeWrapper!!.isAttached)

        layoutNode.attach(MockOwner())
        assertTrue(oldLayoutNodeWrapper.isAttached)

        // only 1 modifier now, so one should be detached and the other can be reused
        layoutNode.modifier = Modifier.graphicsLayer()
        val newLayoutNodeWrapper = layoutNode.outerLayoutNodeWrapper

        // one can be reused, but we don't care which one
        val notReused = if (newLayoutNodeWrapper == oldLayoutNodeWrapper) {
            oldInnerLayoutNodeWrapper
        } else {
            oldLayoutNodeWrapper
        }
        assertTrue(newLayoutNodeWrapper.isAttached)
        assertFalse(notReused.isAttached)
    }

    @Test
    fun layoutNodeWrapperParentLayoutCoordinates() {
        val layoutNode = LayoutNode()
        val layoutNode2 = LayoutNode()
        val layoutModifier = Modifier.graphicsLayer { }
        layoutNode.modifier = layoutModifier
        layoutNode2.insertAt(0, layoutNode)
        layoutNode2.attach(MockOwner())

        assertEquals(
            layoutNode2.innerLayoutNodeWrapper,
            layoutNode.innerLayoutNodeWrapper.parentLayoutCoordinates
        )
        assertEquals(
            layoutNode2.innerLayoutNodeWrapper,
            layoutNode.outerLayoutNodeWrapper.parentLayoutCoordinates
        )
    }

    @Test
    fun layoutNodeWrapperParentCoordinates() {
        val layoutNode = LayoutNode()
        val layoutNode2 = LayoutNode()
        val layoutModifier = object : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                TODO("Not yet implemented")
            }
        }
        val drawModifier = Modifier.drawBehind { }
        layoutNode.modifier = layoutModifier.then(drawModifier)
        layoutNode2.insertAt(0, layoutNode)
        layoutNode2.attach(MockOwner())

        val layoutModifierWrapper = layoutNode.outerLayoutNodeWrapper

        assertEquals(
            layoutModifierWrapper,
            layoutNode.innerLayoutNodeWrapper.parentCoordinates
        )
        assertEquals(
            layoutNode2.innerLayoutNodeWrapper,
            layoutModifierWrapper.parentCoordinates
        )
    }

    @Test
    fun hitTest_pointerInBounds_pointerInputFilterHit() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifierImpl(pointerInputFilter)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(0f, 0f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_pointerInputFilterHit() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifierImpl(pointerInputFilter),
                DpSize(48.dp, 48.dp)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(-3f, 3f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_pointerInputFilterHit_horizontal() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1000, 1,
                PointerInputModifierImpl(pointerInputFilter),
                DpSize(48.dp, 48.dp)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(0f, 3f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_pointerInputFilterHit_vertical() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1000,
                PointerInputModifierImpl(pointerInputFilter),
                DpSize(48.dp, 48.dp)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(3f, 0f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_pointerInputFilterHit_nestedNodes() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val outerNode = LayoutNode(0, 0, 1, 1).apply { attach(MockOwner()) }
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl(pointerInputFilter),
            DpSize(48.dp, 48.dp)
        )
        outerNode.add(layoutNode)
        layoutNode.onNodePlaced()
        val hit = mutableListOf<PointerInputFilter>()

        outerNode.hitTest(Offset(-3f, 3f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInputFilterHit_outsideParent() {
        val outerPointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val outerNode = LayoutNode(
            0, 0, 10, 10,
            PointerInputModifierImpl(outerPointerInputFilter)
        ).apply { attach(MockOwner()) }
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode = LayoutNode(
            20, 20, 30, 30,
            PointerInputModifierImpl(pointerInputFilter)
        )
        outerNode.add(layoutNode)
        layoutNode.onNodePlaced()
        val hit = mutableListOf<PointerInputFilter>()

        outerNode.hitTest(Offset(25f, 25f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInputFilterHit_outsideParent_interceptOutOfBoundsChildEvents() {
        val outerPointerInputFilter: PointerInputFilter = mockPointerInputFilter(
            interceptChildEvents = true
        )
        val outerNode = LayoutNode(
            0, 0, 10, 10,
            PointerInputModifierImpl(outerPointerInputFilter)
        ).apply { attach(MockOwner()) }
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode = LayoutNode(
            20, 20, 30, 30,
            PointerInputModifierImpl(pointerInputFilter)
        )
        outerNode.add(layoutNode)
        layoutNode.onNodePlaced()
        val hit = mutableListOf<PointerInputFilter>()

        outerNode.hitTest(Offset(25f, 25f), hit)

        assertThat(hit).isEqualTo(listOf(outerPointerInputFilter, pointerInputFilter))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_closestHit() {
        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val layoutNode1 = LayoutNode(
            0, 0, 5, 5,
            PointerInputModifierImpl(pointerInputFilter1),
            DpSize(48.dp, 48.dp)
        )

        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val layoutNode2 = LayoutNode(
            6, 6, 11, 11,
            PointerInputModifierImpl(pointerInputFilter2),
            DpSize(48.dp, 48.dp)
        )
        val outerNode = LayoutNode(0, 0, 11, 11).apply { attach(MockOwner()) }
        outerNode.add(layoutNode1)
        outerNode.add(layoutNode2)
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()

        val hit = mutableListOf<PointerInputFilter>()

        // Hit closer to layoutNode1
        outerNode.hitTest(Offset(5.1f, 5.5f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))

        hit.clear()

        // Hit closer to layoutNode2
        outerNode.hitTest(Offset(5.9f, 5.5f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))

        hit.clear()

        // Hit closer to layoutNode1
        outerNode.hitTest(Offset(5.5f, 5.1f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))

        hit.clear()

        // Hit closer to layoutNode2
        outerNode.hitTest(Offset(5.5f, 5.9f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))

        hit.clear()

        // Hit inside layoutNode1
        outerNode.hitTest(Offset(4.9f, 4.9f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))

        hit.clear()

        // Hit inside layoutNode2
        outerNode.hitTest(Offset(6.1f, 6.1f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))
    }

    /**
     * When a child is in the minimum touch target area, but the parent is big enough to not
     * worry about minimum touch target, the child should still be able to be hit outside the
     * parent's bounds.
     */
    @Test
    fun hitTest_pointerInMinimumTouchTarget_inChild_closestHit() {
        test_pointerInMinimumTouchTarget_inChild_closestHit { outerNode, nodeWithChild, soloNode ->
            outerNode.add(nodeWithChild)
            outerNode.add(soloNode)
        }
    }

    /**
     * When a child is in the minimum touch target area, but the parent is big enough to not
     * worry about minimum touch target, the child should still be able to be hit outside the
     * parent's bounds. This is different from
     * [hitTest_pointerInMinimumTouchTarget_inChild_closestHit] because the node with the nested
     * child is after the other node.
     */
    @Test
    fun hitTest_pointerInMinimumTouchTarget_inChildOver_closestHit() {
        test_pointerInMinimumTouchTarget_inChild_closestHit { outerNode, nodeWithChild, soloNode ->
            outerNode.add(soloNode)
            outerNode.add(nodeWithChild)
        }
    }

    private fun test_pointerInMinimumTouchTarget_inChild_closestHit(
        block: (outerNode: LayoutNode, nodeWithChild: LayoutNode, soloNode: LayoutNode) -> Unit
    ) {
        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val layoutNode1 = LayoutNode(
            5, 5, 10, 10,
            PointerInputModifierImpl(pointerInputFilter1),
            DpSize(48.dp, 48.dp)
        )

        val pointerInputFilter2: PointerInputFilter =
            mockPointerInputFilter(interceptChildEvents = true)
        val layoutNode2 = LayoutNode(
            0, 0, 10, 10,
            PointerInputModifierImpl(pointerInputFilter2),
            DpSize(48.dp, 48.dp)
        )
        layoutNode2.add(layoutNode1)

        val pointerInputFilter3: PointerInputFilter = mockPointerInputFilter()
        val layoutNode3 = LayoutNode(
            12, 12, 17, 17,
            PointerInputModifierImpl(pointerInputFilter3),
            DpSize(48.dp, 48.dp)
        )

        val outerNode = LayoutNode(0, 0, 20, 20).apply { attach(MockOwner()) }
        block(outerNode, layoutNode2, layoutNode3)
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()
        layoutNode3.onNodePlaced()

        val hit = mutableListOf<PointerInputFilter>()

        // Hit outside of layoutNode2, but near layoutNode1
        outerNode.hitTest(Offset(10.1f, 10.1f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2, pointerInputFilter1))

        hit.clear()

        // Hit closer to layoutNode3
        outerNode.hitTest(Offset(11.9f, 11.9f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter3))
    }

    @Test
    fun hitTest_pointerInMinimumTouchTarget_closestHitWithOverlap() {
        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val layoutNode1 = LayoutNode(
            0, 0, 5, 5, PointerInputModifierImpl(pointerInputFilter1),
            DpSize(48.dp, 48.dp)
        )

        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val layoutNode2 = LayoutNode(
            4, 4, 9, 9,
            PointerInputModifierImpl(pointerInputFilter2),
            DpSize(48.dp, 48.dp)
        )
        val outerNode = LayoutNode(0, 0, 9, 9).apply { attach(MockOwner()) }
        outerNode.add(layoutNode1)
        outerNode.add(layoutNode2)
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()

        val hit = mutableListOf<PointerInputFilter>()

        // Hit layoutNode1
        outerNode.hitTest(Offset(3.95f, 3.95f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))

        hit.clear()

        // Hit layoutNode2
        outerNode.hitTest(Offset(4.05f, 4.05f), hit, true)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))
    }

    @Test
    fun hitTestSemantics_pointerInMinimumTouchTarget_pointerInputFilterHit() {
        val semanticsConfiguration = SemanticsConfiguration()
        val semanticsModifier = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                semanticsModifier,
                DpSize(48.dp, 48.dp)
            ).apply {
                attach(MockOwner())
            }
        val hit = HitTestResult<SemanticsEntity>()

        layoutNode.hitTestSemantics(Offset(-3f, 3f), hit)

        assertThat(hit).hasSize(1)
        assertThat(hit[0].modifier).isEqualTo(semanticsModifier)
    }

    @Test
    fun hitTestSemantics_pointerInMinimumTouchTarget_pointerInputFilterHit_nestedNodes() {
        val semanticsConfiguration = SemanticsConfiguration()
        val semanticsModifier = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val outerNode = LayoutNode(0, 0, 1, 1).apply { attach(MockOwner()) }
        val layoutNode = LayoutNode(0, 0, 1, 1, semanticsModifier, DpSize(48.dp, 48.dp))
        outerNode.add(layoutNode)
        layoutNode.onNodePlaced()
        val hit = HitTestResult<SemanticsEntity>()

        layoutNode.hitTestSemantics(Offset(-3f, 3f), hit)

        assertThat(hit).hasSize(1)
        assertThat(hit[0].modifier).isEqualTo(semanticsModifier)
    }

    @Test
    fun hitTestSemantics_pointerInMinimumTouchTarget_closestHit() {
        val semanticsConfiguration = SemanticsConfiguration()
        val semanticsModifier1 = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val semanticsModifier2 = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val layoutNode1 = LayoutNode(0, 0, 5, 5, semanticsModifier1, DpSize(48.dp, 48.dp))
        val layoutNode2 = LayoutNode(6, 6, 11, 11, semanticsModifier2, DpSize(48.dp, 48.dp))
        val outerNode = LayoutNode(0, 0, 11, 11).apply { attach(MockOwner()) }
        outerNode.add(layoutNode1)
        outerNode.add(layoutNode2)
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()

        // Hit closer to layoutNode1
        val hit1 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(5.1f, 5.5f), hit1, true)

        assertThat(hit1).hasSize(1)
        assertThat(hit1[0].modifier).isEqualTo(semanticsModifier1)

        // Hit closer to layoutNode2
        val hit2 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(5.9f, 5.5f), hit2, true)

        assertThat(hit2).hasSize(1)
        assertThat(hit2[0].modifier).isEqualTo(semanticsModifier2)

        // Hit closer to layoutNode1
        val hit3 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(5.5f, 5.1f), hit3, true)

        assertThat(hit3).hasSize(1)
        assertThat(hit3[0].modifier).isEqualTo(semanticsModifier1)

        // Hit closer to layoutNode2
        val hit4 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(5.5f, 5.9f), hit4, true)

        assertThat(hit4).hasSize(1)
        assertThat(hit4[0].modifier).isEqualTo(semanticsModifier2)

        // Hit inside layoutNode1
        val hit5 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(4.9f, 4.9f), hit5, true)

        assertThat(hit5).hasSize(1)
        assertThat(hit5[0].modifier).isEqualTo(semanticsModifier1)

        // Hit inside layoutNode2
        val hit6 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(6.1f, 6.1f), hit6, true)

        assertThat(hit6).hasSize(1)
        assertThat(hit6[0].modifier).isEqualTo(semanticsModifier2)
    }

    @Test
    fun hitTestSemantics_pointerInMinimumTouchTarget_closestHitWithOverlap() {
        val semanticsConfiguration = SemanticsConfiguration()
        val semanticsModifier1 = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val semanticsModifier2 = object : SemanticsModifier {
            override val id: Int = 1
            override val semanticsConfiguration: SemanticsConfiguration = semanticsConfiguration
        }
        val layoutNode1 = LayoutNode(0, 0, 5, 5, semanticsModifier1, DpSize(48.dp, 48.dp))
        val layoutNode2 = LayoutNode(4, 4, 9, 9, semanticsModifier2, DpSize(48.dp, 48.dp))
        val outerNode = LayoutNode(0, 0, 11, 11).apply { attach(MockOwner()) }
        outerNode.add(layoutNode1)
        outerNode.add(layoutNode2)
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()

        // Hit layoutNode1
        val hit1 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(3.95f, 3.95f), hit1, true)

        assertThat(hit1).hasSize(1)
        assertThat(hit1[0].modifier).isEqualTo(semanticsModifier1)

        // Hit layoutNode2
        val hit2 = HitTestResult<SemanticsEntity>()
        outerNode.hitTestSemantics(Offset(4.05f, 4.05f), hit2, true)

        assertThat(hit2).hasSize(1)
        assertThat(hit2[0].modifier).isEqualTo(semanticsModifier2)
    }

    @Test
    fun hitTest_pointerOutOfBounds_nothingHit() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifierImpl(pointerInputFilter)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(-1f, -1f), hit)
        layoutNode.hitTest(Offset(0f, -1f), hit)
        layoutNode.hitTest(Offset(1f, -1f), hit)

        layoutNode.hitTest(Offset(-1f, 0f), hit)
        // 0, 0 would hit
        layoutNode.hitTest(Offset(1f, 0f), hit)

        layoutNode.hitTest(Offset(-1f, 1f), hit)
        layoutNode.hitTest(Offset(0f, 1f), hit)
        layoutNode.hitTest(Offset(1f, 1f), hit)

        assertThat(hit).isEmpty()
    }

    @Test
    fun hitTest_pointerOutOfBounds_nothingHit_extendedBounds() {
        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifierImpl(pointerInputFilter),
                minimumTouchTargetSize = DpSize(4.dp, 8.dp)
            ).apply {
                attach(MockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(Offset(-3f, -5f), hit)
        layoutNode.hitTest(Offset(0f, -5f), hit)
        layoutNode.hitTest(Offset(3f, -5f), hit)

        layoutNode.hitTest(Offset(-3f, 0f), hit)
        // 0, 0 would hit
        layoutNode.hitTest(Offset(3f, 0f), hit)

        layoutNode.hitTest(Offset(-3f, 5f), hit)
        layoutNode.hitTest(Offset(0f, 5f), hit)
        layoutNode.hitTest(Offset(-3f, 5f), hit)

        assertThat(hit).isEmpty()
    }

    @Test
    fun hitTest_nestedOffsetNodesHits3_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(3)
    }

    @Test
    fun hitTest_nestedOffsetNodesHits2_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(2)
    }

    @Test
    fun hitTest_nestedOffsetNodesHits1_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(1)
    }

    private fun hitTest_nestedOffsetNodes_allHitInCorrectOrder(numberOfChildrenHit: Int) {
        // Arrange

        val childPointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val middlePointerInputFilter: PointerInputFilter = mockPointerInputFilter()
        val parentPointerInputFilter: PointerInputFilter = mockPointerInputFilter()

        val childLayoutNode =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifierImpl(
                    childPointerInputFilter
                )
            )
        val middleLayoutNode: LayoutNode =
            LayoutNode(
                100, 100, 400, 400,
                PointerInputModifierImpl(
                    middlePointerInputFilter
                )
            ).apply {
                insertAt(0, childLayoutNode)
            }
        val parentLayoutNode: LayoutNode =
            LayoutNode(
                0, 0, 500, 500,
                PointerInputModifierImpl(
                    parentPointerInputFilter
                )
            ).apply {
                insertAt(0, middleLayoutNode)
                attach(MockOwner())
            }
        middleLayoutNode.onNodePlaced()
        childLayoutNode.onNodePlaced()

        val offset = when (numberOfChildrenHit) {
            3 -> Offset(250f, 250f)
            2 -> Offset(150f, 150f)
            1 -> Offset(50f, 50f)
            else -> throw IllegalStateException()
        }

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parentLayoutNode.hitTest(offset, hit)

        // Assert.

        when (numberOfChildrenHit) {
            3 ->
                assertThat(hit)
                    .isEqualTo(
                        listOf(
                            parentPointerInputFilter,
                            middlePointerInputFilter,
                            childPointerInputFilter
                        )
                    )
            2 ->
                assertThat(hit)
                    .isEqualTo(
                        listOf(
                            parentPointerInputFilter,
                            middlePointerInputFilter
                        )
                    )
            1 ->
                assertThat(hit)
                    .isEqualTo(
                        listOf(
                            parentPointerInputFilter
                        )
                    )
            else -> throw IllegalStateException()
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -------------
     *  |     |     |
     *  |  t  |     |
     *  |     |     |
     *  |-----|     |
     *  |           |
     *  |     |-----|
     *  |     |     |
     *  |     |  t  |
     *  |     |     |
     *  -------------
     *
     * Where there is one child in the top right and one in the bottom left, and 2 pointers where
     * one in the top left and one in the bottom right.
     */
    @Test
    fun hitTest_2PointersOver2DifferentPointerInputModifiers_resultIsCorrect() {

        // Arrange

        val childPointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val childPointerInputFilter2: PointerInputFilter = mockPointerInputFilter()

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 50, 50,
                PointerInputModifierImpl(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 100, 100,
                PointerInputModifierImpl(
                    childPointerInputFilter2
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(MockOwner())
        }
        childLayoutNode1.onNodePlaced()
        childLayoutNode2.onNodePlaced()

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  | t      |    |
     *  |        |    |
     *  |  |-------|  |
     *  |  | t     |  |
     *  |  |       |  |
     *  |  |       |  |
     *  |--|  |-------|
     *  |  |  | t     |
     *  |  |  |       |
     *  |  |  |       |
     *  |  |--|       |
     *  |     |       |
     *  ---------------
     *
     * There are 3 staggered children and 3 pointers, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space in child 3 that
     * overlaps child 2.
     */
    @Test
    fun hitTest_3DownOnOverlappingPointerInputModifiers_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val childPointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val childPointerInputFilter3: PointerInputFilter = mockPointerInputFilter()

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 100, 100,
                PointerInputModifierImpl(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 150, 150,
                PointerInputModifierImpl(
                    childPointerInputFilter2
                )
            )

        val childLayoutNode3 =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifierImpl(
                    childPointerInputFilter3
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 200, 200).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            insertAt(2, childLayoutNode3)
            attach(MockOwner())
        }
        childLayoutNode1.onNodePlaced()
        childLayoutNode2.onNodePlaced()
        childLayoutNode3.onNodePlaced()

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)
        val offset3 = Offset(125f, 125f)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter3))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  |             |
     *  |      t      |
     *  |             |
     *  |  |-------|  |
     *  |  |       |  |
     *  |  |   t   |  |
     *  |  |       |  |
     *  |  |-------|  |
     *  |             |
     *  |      t      |
     *  |             |
     *  ---------------
     *
     * There are 2 children with one over the other and 3 pointers: the first is on background
     * child, the second is on the foreground child, and the third is again on the background child.
     */
    @Test
    fun hitTest_3DownOnFloatingPointerInputModifierV_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val childPointerInputFilter2: PointerInputFilter = mockPointerInputFilter()

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 150,
            PointerInputModifierImpl(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            25, 50, 75, 100,
            PointerInputModifierImpl(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(MockOwner())
        }
        childLayoutNode1.onNodePlaced()
        childLayoutNode2.onNodePlaced()

        val offset1 = Offset(50f, 25f)
        val offset2 = Offset(50f, 75f)
        val offset3 = Offset(50f, 125f)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter1))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -----------------
     *  |               |
     *  |   |-------|   |
     *  |   |       |   |
     *  | t |   t   | t |
     *  |   |       |   |
     *  |   |-------|   |
     *  |               |
     *  -----------------
     *
     * There are 2 children with one over the other and 3 pointers: the first is on background
     * child, the second is on the foreground child, and the third is again on the background child.
     */
    @Test
    fun hitTest_3DownOnFloatingPointerInputModifierH_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val childPointerInputFilter2: PointerInputFilter = mockPointerInputFilter()

        val childLayoutNode1 = LayoutNode(
            0, 0, 150, 100,
            PointerInputModifierImpl(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 25, 100, 75,
            PointerInputModifierImpl(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(MockOwner())
        }
        childLayoutNode2.onNodePlaced()
        childLayoutNode1.onNodePlaced()

        val offset1 = Offset(25f, 50f)
        val offset2 = Offset(75f, 50f)
        val offset3 = Offset(125f, 50f)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter1))
    }

    /**
     * This test creates a layout of this shape:
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are clipped by their parent LayoutNode. 4
     * touches touch just inside the parent LayoutNode and inside the child LayoutNodes. 8
     * touches touch just outside the parent LayoutNode but inside the child LayoutNodes.
     *
     * Because layout node bounds are not used to clip pointer input hit testing, all pointers
     * should hit.
     */
    @Test
    fun hitTest_4DownInClippedAreaOfLnsWithPims_resultIsCorrect() {

        // Arrange

        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter3: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter4: PointerInputFilter = mockPointerInputFilter()

        val layoutNode1 = LayoutNode(
            -1, -1, 1, 1,
            PointerInputModifierImpl(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            2, -1, 4, 1,
            PointerInputModifierImpl(
                pointerInputFilter2
            )
        )
        val layoutNode3 = LayoutNode(
            -1, 2, 1, 4,
            PointerInputModifierImpl(
                pointerInputFilter3
            )
        )
        val layoutNode4 = LayoutNode(
            2, 2, 4, 4,
            PointerInputModifierImpl(
                pointerInputFilter4
            )
        )

        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
            insertAt(2, layoutNode3)
            insertAt(3, layoutNode4)
            attach(MockOwner())
        }
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()
        layoutNode3.onNodePlaced()
        layoutNode4.onNodePlaced()

        val offsetsThatHit1 =
            listOf(
                Offset(0f, 1f),
                Offset(1f, 0f),
                Offset(1f, 1f)
            )

        val offsetsThatHit2 =
            listOf(
                Offset(3f, 0f),
                Offset(3f, 1f),
                Offset(4f, 1f)
            )

        val offsetsThatHit3 =
            listOf(
                Offset(0f, 3f),
                Offset(1f, 3f),
                Offset(1f, 4f)
            )

        val offsetsThatHit4 =
            listOf(
                Offset(3f, 3f),
                Offset(3f, 4f),
                Offset(4f, 3f)
            )

        val hit = mutableListOf<PointerInputFilter>()

        // Act and Assert

        offsetsThatHit1.forEach {
            hit.clear()
            parentLayoutNode.hitTest(it, hit)
            assertThat(hit).isEqualTo(listOf(pointerInputFilter1))
        }

        offsetsThatHit2.forEach {
            hit.clear()
            parentLayoutNode.hitTest(it, hit)
            assertThat(hit).isEqualTo(listOf(pointerInputFilter2))
        }

        offsetsThatHit3.forEach {
            hit.clear()
            parentLayoutNode.hitTest(it, hit)
            assertThat(hit).isEqualTo(listOf(pointerInputFilter3))
        }

        offsetsThatHit4.forEach {
            hit.clear()
            parentLayoutNode.hitTest(it, hit)
            assertThat(hit).isEqualTo(listOf(pointerInputFilter4))
        }
    }

    @Test
    fun hitTest_pointerOn3NestedPointerInputModifiers_allPimsHitInCorrectOrder() {

        // Arrange.

        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter3: PointerInputFilter = mockPointerInputFilter()

        val modifier =
            PointerInputModifierImpl(
                pointerInputFilter1
            ) then PointerInputModifierImpl(
                pointerInputFilter2
            ) then PointerInputModifierImpl(
                pointerInputFilter3
            )

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        ).apply {
            attach(MockOwner())
        }

        val offset1 = Offset(50f, 75f)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(
            listOf(
                pointerInputFilter1,
                pointerInputFilter2,
                pointerInputFilter3
            )
        )
    }

    @Test
    fun hitTest_pointerOnDeeplyNestedPointerInputModifier_pimIsHit() {

        // Arrange.

        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()

        val layoutNode1 =
            LayoutNode(
                1, 5, 500, 500,
                PointerInputModifierImpl(
                    pointerInputFilter
                )
            )
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            insertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            insertAt(0, layoutNode3)
        }.apply {
            attach(MockOwner())
        }
        layoutNode3.onNodePlaced()
        layoutNode2.onNodePlaced()
        layoutNode1.onNodePlaced()
        val offset1 = Offset(499f, 499f)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode4.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerOnComplexPointerAndLayoutNodePath_pimsHitInCorrectOrder() {

        // Arrange.

        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter3: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter4: PointerInputFilter = mockPointerInputFilter()

        val layoutNode1 = LayoutNode(
            1, 6, 500, 500,
            PointerInputModifierImpl(
                pointerInputFilter1
            ) then PointerInputModifierImpl(
                pointerInputFilter2
            )
        )
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3 =
            LayoutNode(
                3, 8, 500, 500,
                PointerInputModifierImpl(
                    pointerInputFilter3
                ) then PointerInputModifierImpl(
                    pointerInputFilter4
                )
            ).apply {
                insertAt(0, layoutNode2)
            }

        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            insertAt(0, layoutNode4)
        }.apply {
            attach(MockOwner())
        }
        layoutNode4.onNodePlaced()
        layoutNode3.onNodePlaced()
        layoutNode2.onNodePlaced()
        layoutNode1.onNodePlaced()

        val offset1 = Offset(499f, 499f)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode5.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(
            listOf(
                pointerInputFilter3,
                pointerInputFilter4,
                pointerInputFilter1,
                pointerInputFilter2
            )
        )
    }

    @Test
    fun hitTest_pointerOnFullyOverlappingPointerInputModifiers_onlyTopPimIsHit() {

        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()

        val layoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl(
                pointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
            attach(MockOwner())
        }
        layoutNode1.onNodePlaced()
        layoutNode2.onNodePlaced()

        val offset = Offset(50f, 50f)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parentLayoutNode.hitTest(offset, hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))
    }

    @Test
    fun hitTest_pointerOnPointerInputModifierInLayoutNodeWithNoSize_nothingHit() {

        val pointerInputFilter: PointerInputFilter = mockPointerInputFilter()

        val layoutNode = LayoutNode(
            0, 0, 0, 0,
            PointerInputModifierImpl(
                pointerInputFilter
            )
        ).apply {
            attach(MockOwner())
        }

        val offset = Offset.Zero

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode.hitTest(offset, hit)

        // Assert.

        assertThat(hit).isEmpty()
    }

    @Test
    fun hitTest_zIndexIsAccounted() {

        val pointerInputFilter1: PointerInputFilter = mockPointerInputFilter()
        val pointerInputFilter2: PointerInputFilter = mockPointerInputFilter()

        val parent = LayoutNode(
            0, 0, 2, 2
        ).apply {
            attach(
                MockOwner().apply {
                    measureIteration = 1L
                }
            )
        }
        parent.insertAt(
            0,
            LayoutNode(
                0, 0, 2, 2,
                PointerInputModifierImpl(
                    pointerInputFilter1
                ).zIndex(1f)
            )
        )
        parent.insertAt(
            1,
            LayoutNode(
                0, 0, 2, 2,
                PointerInputModifierImpl(
                    pointerInputFilter2
                )
            )
        )
        parent.remeasure()
        parent.replace()

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parent.hitTest(Offset(1f, 1f), hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))
    }

    @Test
    fun onRequestMeasureIsNotCalledOnDetachedNodes() {
        val root = LayoutNode()

        val node1 = LayoutNode()
        root.add(node1)
        val node2 = LayoutNode()
        node1.add(node2)

        val owner = MockOwner()
        root.attach(owner)
        owner.onAttachParams.clear()
        owner.onRequestMeasureParams.clear()

        // Dispose
        root.removeAt(0, 1)

        assertFalse(node1.isAttached)
        assertFalse(node2.isAttached)
        assertEquals(0, owner.onRequestMeasureParams.count { it === node1 })
        assertEquals(0, owner.onRequestMeasureParams.count { it === node2 })
    }

    @Test
    fun modifierMatchesWrapperWithIdentity() {
        val modifier1 = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        val modifier2 = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(1, 1)
            }
        }

        val root = LayoutNode()
        root.modifier = modifier1.then(modifier2)

        val wrapper1 = root.outerLayoutNodeWrapper
        val wrapper2 = root.outerLayoutNodeWrapper.wrapped

        assertEquals(modifier1, (wrapper1 as ModifiedLayoutNode).modifier)
        assertEquals(modifier2, (wrapper2 as ModifiedLayoutNode).modifier)

        root.modifier = modifier2.then(modifier1)

        assertEquals(wrapper2, root.outerLayoutNodeWrapper)
        assertEquals(wrapper1, root.outerLayoutNodeWrapper.wrapped)
        assertEquals(
            modifier1,
            (root.outerLayoutNodeWrapper.wrapped as ModifiedLayoutNode).modifier
        )
        assertEquals(
            modifier2,
            (root.outerLayoutNodeWrapper as ModifiedLayoutNode).modifier
        )
    }

    @Test
    fun measureResultAndPositionChangesCallOnLayoutChange() {
        val node = LayoutNode(20, 20, 100, 100)
        val owner = MockOwner()
        node.attach(owner)
        node.innerLayoutNodeWrapper.measureResult = object : MeasureResult {
            override val width = 50
            override val height = 50
            override val alignmentLines: Map<AlignmentLine, Int> get() = mapOf()
            override fun placeChildren() {}
        }
        assertEquals(1, owner.layoutChangeCount)
        node.place(0, 0)
        assertEquals(2, owner.layoutChangeCount)
    }

    @Test
    fun layerParamChangeCallsOnLayoutChange() {
        val node = LayoutNode(20, 20, 100, 100, Modifier.graphicsLayer())
        val owner = MockOwner()
        node.attach(owner)
        assertEquals(0, owner.layoutChangeCount)
        node.innerLayoutNodeWrapper.onLayerBlockUpdated { scaleX = 0.5f }
        assertEquals(1, owner.layoutChangeCount)
        repeat(2) {
            node.innerLayoutNodeWrapper.onLayerBlockUpdated { scaleX = 1f }
        }
        assertEquals(2, owner.layoutChangeCount)
        node.innerLayoutNodeWrapper.onLayerBlockUpdated(null)
        assertEquals(3, owner.layoutChangeCount)
    }

    @Test
    fun reuseModifiersThatImplementMultipleModifierInterfaces() {
        val drawAndLayoutModifier: Modifier = object : DrawModifier, LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.placeRelative(IntOffset.Zero)
                }
            }

            override fun ContentDrawScope.draw() {
                drawContent()
            }
        }
        val a = Modifier.then(EmptyLayoutModifier()).then(drawAndLayoutModifier)
        val b = Modifier.then(EmptyLayoutModifier()).then(drawAndLayoutModifier)
        val node = LayoutNode(20, 20, 100, 100)
        val owner = MockOwner()
        node.attach(owner)
        node.modifier = a
        assertEquals(3, node.getModifierInfo().size)
        node.modifier = b
        assertEquals(3, node.getModifierInfo().size)
    }

    @Test
    fun layoutNodeWrapper_alpha() {
        val root = LayoutNode().apply { this.modifier = Modifier.drawBehind {} }
        val layoutNode1 = LayoutNode().apply {
            this.modifier = Modifier.graphicsLayer { }.graphicsLayer { }.drawBehind {}
        }
        val layoutNode2 = LayoutNode().apply { this.modifier = Modifier.drawBehind {} }
        val owner = MockOwner()

        root.insertAt(0, layoutNode1)
        layoutNode1.insertAt(0, layoutNode2)
        root.attach(owner)

        // provide alpha to the graphics layer
        layoutNode1.outerLayoutNodeWrapper.wrapped!!.onLayerBlockUpdated {
            alpha = 0f
        }
        layoutNode1.outerLayoutNodeWrapper.wrapped!!.wrapped!!.onLayerBlockUpdated {
            alpha = 0.5f
        }

        assertFalse(layoutNode1.outerLayoutNodeWrapper.isTransparent())
        assertTrue(layoutNode1.innerLayoutNodeWrapper.isTransparent())
        assertTrue(layoutNode2.outerLayoutNodeWrapper.isTransparent())
        assertTrue(layoutNode2.innerLayoutNodeWrapper.isTransparent())
    }

    private fun createSimpleLayout(): Triple<LayoutNode, LayoutNode, LayoutNode> {
        val layoutNode = ZeroSizedLayoutNode()
        val child1 = ZeroSizedLayoutNode()
        val child2 = ZeroSizedLayoutNode()
        layoutNode.insertAt(0, child1)
        layoutNode.insertAt(1, child2)
        return Triple(layoutNode, child1, child2)
    }

    private fun ZeroSizedLayoutNode() = LayoutNode(0, 0, 0, 0)

    private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
        PointerInputModifier
}

private class EmptyLayoutModifier : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(IntOffset.Zero)
        }
    }
}

@OptIn(InternalCoreApi::class)
private class MockOwner(
    val position: IntOffset = IntOffset.Zero,
    override val root: LayoutNode = LayoutNode()
) : Owner {
    val onRequestMeasureParams = mutableListOf<LayoutNode>()
    val onAttachParams = mutableListOf<LayoutNode>()
    val onDetachParams = mutableListOf<LayoutNode>()
    var layoutChangeCount = 0

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

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofill: Autofill?
        get() = TODO("Not yet implemented")
    override val density: Density
        get() = Density(1f)
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
    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader
        get() = TODO("Not yet implemented")
    override val fontFamilyResolver: FontFamily.Resolver
        get() = TODO("Not yet implemented")
    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr
    override var showLayoutBounds: Boolean = false
    override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        onRequestMeasureParams += layoutNode
        if (affectsLookahead) {
            layoutNode.markLookaheadMeasurePending()
        }
        layoutNode.markMeasurePending()
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            layoutNode.markLookaheadLayoutPending()
        }
        layoutNode.markLayoutPending()
    }

    override fun onAttach(node: LayoutNode) {
        onAttachParams += node
    }

    override fun onDetach(node: LayoutNode) {
        onDetachParams += node
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset =
        localPosition + position.toOffset()

    override fun calculateLocalPosition(positionInWindow: Offset): Offset =
        positionInWindow - position.toOffset()

    override fun requestFocus(): Boolean = false

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
    }

    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        listener()
    }

    override fun onEndApplyChanges() {
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        return object : OwnedLayer {
            override fun updateLayerProperties(
                scaleX: Float,
                scaleY: Float,
                alpha: Float,
                translationX: Float,
                translationY: Float,
                shadowElevation: Float,
                rotationX: Float,
                rotationY: Float,
                rotationZ: Float,
                cameraDistance: Float,
                transformOrigin: TransformOrigin,
                shape: Shape,
                clip: Boolean,
                renderEffect: RenderEffect?,
                ambientShadowColor: Color,
                spotShadowColor: Color,
                layoutDirection: LayoutDirection,
                density: Density
            ) {
            }

            override fun isInLayer(position: Offset) = true

            override fun move(position: IntOffset) {
            }

            override fun resize(size: IntSize) {
            }

            override fun drawLayer(canvas: Canvas) {
                drawBlock(canvas)
            }

            override fun updateDisplayList() {
            }

            override fun invalidate() {
            }

            override fun destroy() {
            }

            override fun mapBounds(rect: MutableRect, inverse: Boolean) {
            }

            override fun reuseLayer(
                drawBlock: (Canvas) -> Unit,
                invalidateParentLayer: () -> Unit
            ) {
            }

            override fun mapOffset(point: Offset, inverse: Boolean) = point
        }
    }

    override fun onSemanticsChange() {
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        layoutChangeCount++
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        TODO("Not yet implemented")
    }

    override var measureIteration: Long = 0
    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")

    override val sharedDrawScope = LayoutNodeDrawScope()
}

private fun LayoutNode.hitTest(
    pointerPosition: Offset,
    hitPointerInputFilters: MutableList<PointerInputFilter>,
    isTouchEvent: Boolean = false
) {
    val hitTestResult = HitTestResult<PointerInputFilter>()
    hitTest(pointerPosition, hitTestResult, isTouchEvent)
    hitPointerInputFilters.addAll(hitTestResult)
}

private fun LayoutNode(
    x: Int,
    y: Int,
    x2: Int,
    y2: Int,
    modifier: Modifier = Modifier,
    minimumTouchTargetSize: DpSize = DpSize.Zero
) = LayoutNode().apply {
    this.viewConfiguration = TestViewConfiguration(minimumTouchTargetSize = minimumTouchTargetSize)
    this.modifier = modifier
    measurePolicy = object : LayoutNode.NoIntrinsicsMeasurePolicy("not supported") {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult =
            layout(x2 - x, y2 - y) {
                measurables.forEach { it.measure(constraints).place(0, 0) }
            }
    }
    attach(MockOwner())
    markMeasurePending()
    remeasure(Constraints())
    var wrapper: LayoutNodeWrapper? = outerLayoutNodeWrapper
    while (wrapper != null) {
        wrapper.measureResult = innerLayoutNodeWrapper.measureResult
        wrapper = (wrapper as? LayoutNodeWrapper)?.wrapped
    }
    place(x, y)
    detach()
}

private fun mockPointerInputFilter(
    interceptChildEvents: Boolean = false
): PointerInputFilter = object : PointerInputFilter() {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
    }

    override fun onCancel() {
    }

    override val interceptOutOfBoundsChildEvents: Boolean
        get() = interceptChildEvents
}
