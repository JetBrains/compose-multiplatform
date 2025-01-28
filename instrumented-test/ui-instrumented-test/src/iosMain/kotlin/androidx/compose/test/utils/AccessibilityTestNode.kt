/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.utils

import androidx.compose.ui.unit.*
import kotlinx.cinterop.CValue
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.UIKit.*
import platform.darwin.NSIntegerMax
import platform.darwin.NSObject
import kotlin.test.assertTrue

/**
 * Constructs an accessibility tree representation of the UI hierarchy starting from the window.
 *
 * This function traverses the accessibility elements and their children to build a structured
 * node tree with information about accessibility properties, allowing for analysis and testing
 * of the accessibility features of the UI.
 *
 * @return The root node of the accessibility tree representing the current UI hierarchy,
 * or null if the tree cannot be constructed.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun UIKitInstrumentedTest.getAccessibilityTree(): AccessibilityTestNode {
    fun buildNode(element: NSObject, level: Int): AccessibilityTestNode {
        val children = mutableListOf<AccessibilityTestNode>()
        val elements = element.accessibilityElements()

        if (elements != null) {
            elements.forEach {
                children.add(buildNode(it as NSObject, level = level + 1))
            }
        } else {
            val count = element.accessibilityElementCount()
            if (count == NSIntegerMax) {
                when {
                    element is UIView -> {
                        element.subviews.mapNotNull {
                            children.add(buildNode(it as UIView, level = level + 1))
                        }
                    }
                }
            } else if (count > 0) {
                (0 until count).mapNotNull {
                    val child = element.accessibilityElementAtIndex(it) as NSObject
                    children.add(buildNode(child, level = level + 1))
                }
            } else if (element is UIView) {
                element.subviews.mapNotNull {
                    children.add(buildNode(it as UIView, level = level + 1))
                }
            }
        }

        return AccessibilityTestNode(
            isAccessibilityElement = element.isAccessibilityElement,
            identifier = (element as? UIAccessibilityElement)?.accessibilityIdentifier,
            label = element.accessibilityLabel,
            value = element.accessibilityValue,
            frame = element.accessibilityFrame.toDpRect(),
            children = children,
            traits = allAccessibilityTraits.keys.filter {
                element.accessibilityTraits and it != 0.toULong()
            },
            element = element
        ).also { node ->
            children.forEach { it.parent = node }
        }
    }

    return buildNode(appDelegate.window!!, 0)
}

private val allAccessibilityTraits = mapOf(
    UIAccessibilityTraitNone to "UIAccessibilityTraitNone",
    UIAccessibilityTraitButton to "UIAccessibilityTraitButton",
    UIAccessibilityTraitLink to "UIAccessibilityTraitLink",
    UIAccessibilityTraitHeader to "UIAccessibilityTraitHeader",
    UIAccessibilityTraitSearchField to "UIAccessibilityTraitSearchField",
    UIAccessibilityTraitImage to "UIAccessibilityTraitImage",
    UIAccessibilityTraitSelected to "UIAccessibilityTraitSelected",
    UIAccessibilityTraitPlaysSound to "UIAccessibilityTraitPlaysSound",
    UIAccessibilityTraitKeyboardKey to "UIAccessibilityTraitKeyboardKey",
    UIAccessibilityTraitStaticText to "UIAccessibilityTraitStaticText",
    UIAccessibilityTraitSummaryElement to "UIAccessibilityTraitSummaryElement",
    UIAccessibilityTraitNotEnabled to "UIAccessibilityTraitNotEnabled",
    UIAccessibilityTraitUpdatesFrequently to "UIAccessibilityTraitUpdatesFrequently",
    UIAccessibilityTraitStartsMediaSession to "UIAccessibilityTraitStartsMediaSession",
    UIAccessibilityTraitAdjustable to "UIAccessibilityTraitAdjustable",
    UIAccessibilityTraitAllowsDirectInteraction to "UIAccessibilityTraitAllowsDirectInteraction",
    UIAccessibilityTraitCausesPageTurn to "UIAccessibilityTraitCausesPageTurn",
    UIAccessibilityTraitTabBar to "UIAccessibilityTraitTabBar",
    UIAccessibilityTraitToggleButton to "UIAccessibilityTraitToggleButton",
    UIAccessibilityTraitSupportsZoom to "UIAccessibilityTraitSupportsZoom"
)

/**
 * Represents a node in an accessibility tree, which is used for testing accessibility features
 * within a UI hierarchy. This class captures various accessibility properties of UI components
 * and structures them into a tree.
 */
internal data class AccessibilityTestNode(
    var isAccessibilityElement: Boolean? = null,
    var identifier: String? = null,
    var label: String? = null,
    var value: String? = null,
    var frame: DpRect? = null,
    var children: List<AccessibilityTestNode>? = null,
    var traits: List<UIAccessibilityTraits>? = null,
    var element: NSObject? = null,
    var parent: AccessibilityTestNode? = null,
) {
    fun node(builder: AccessibilityTestNode.() -> Unit) {
        children = (children ?: emptyList()) + AccessibilityTestNode().apply(builder)
    }

    fun traits(vararg trait: UIAccessibilityTraits) {
        traits = (traits ?: emptyList()) + trait
    }

    fun validate(actualNode: AccessibilityTestNode?) {
        isAccessibilityElement?.let {
            assertEquals(it, actualNode?.isAccessibilityElement)
        }
        identifier?.let {
            assertEquals(it, actualNode?.identifier)
        }
        label?.let {
            assertEquals(it, actualNode?.label)
        }
        value?.let {
            assertEquals(it, actualNode?.value)
        }
        frame?.let {
            assertEquals(it, actualNode?.frame)
        }
        traits?.let {
            assertEquals(it.toSet(), actualNode?.traits?.toSet())
        }
        children?.let {
            assertEquals(it.count(), actualNode?.children?.count())
            it.zip(actualNode?.children ?: emptyList()) { validator, child ->
                validator.validate(child)
            }
        }
    }

    val hasAccessibilityComponents: Boolean = identifier != null ||
        isAccessibilityElement == true ||
        label != null ||
        value != null ||
        traits?.isNotEmpty() == true

    fun printTree(): String {
        val builder = StringBuilder()

        fun print(node: AccessibilityTestNode, level: Int) {
            val indent = "    ".repeat(level)
            builder.append(indent)
            builder.append(node.label ?: node.identifier ?: "other")
            builder.append(" - ${node.frame}")
            node.element?.let {
                builder.append(" - <${it::class}>")
            }
            builder.appendLine()

            val fieldIndent = "$indent |"
            if (node.isAccessibilityElement == true) {
                builder.appendLine("$fieldIndent isAccessibilityElement: true")
            }
            node.identifier?.let {
                builder.appendLine("$fieldIndent accessibilityIdentifier: $it")
            }
            node.label?.let { builder.appendLine("$fieldIndent accessibilityLabel: $it") }
            if (node.traits?.isNotEmpty() == true) {
                builder.appendLine("$fieldIndent accessibilityTraits:")
                node.traits?.forEach {
                    builder.appendLine("$fieldIndent  - ${allAccessibilityTraits.getValue(it)}")
                }
            }
            node.value?.let { builder.appendLine("$fieldIndent accessibilityValue: $it") }
            node.element?.accessibilityCustomActions?.takeIf { it.isNotEmpty() }?.let {
                builder.appendLine("$fieldIndent accessibilityCustomActions: $it")
            }

            node.children?.forEach { print(it, level + 1) }
        }
        print(this, level = 0)

        return builder.toString()
    }
}

/**
 * Normalizes the accessibility nodes tree by analyzing its properties and children.
 * Removes all element that are not accessibility elements or does not work as elements containers.
 */
internal fun AccessibilityTestNode.normalized(): AccessibilityTestNode? {
    val normalizedChildren = children?.flatMap { child ->
        child.normalized()?.let {
            if (it.hasAccessibilityComponents || (it.children?.count() ?: 0) > 1) {
                listOf(it)
            } else {
                it.children
            }
        } ?: emptyList()
    } ?: emptyList()

    return if (hasAccessibilityComponents || normalizedChildren.count() > 1) {
        this.copy(children = normalizedChildren)
    } else if (normalizedChildren.count() == 1) {
        normalizedChildren.single()
    } else {
        null
    }
}

internal fun AccessibilityTestNode.assertVisibleInContainer() {
    var frame = this.frame ?: DpRectZero()
    var iterator = parent
    while (iterator != null) {
        frame = frame.intersect(iterator.frame ?: DpRectZero())
        iterator = iterator.parent
    }

    assertTrue(
        frame.width >= 1.dp && frame.height >= 1.dp,
        "Element with frame ${this.frame} is not visible or has very small size"
    )
}

/**
 * Asserts that the current accessibility tree matches the expected structure defined in the
 * provided lambda. The expected structure is defined by configuring an `AccessibilityTestNode`,
 * which is then validated against the actual normalized accessibility tree. This function waits
 * for the UI to be idle before performing the validation.
 *
 * @param expected A lambda that allows the caller to specify the expected structure and properties
 * of the accessibility tree.
 */
internal fun UIKitInstrumentedTest.assertAccessibilityTree(
    expected: AccessibilityTestNode.() -> Unit
) {
    val validator = AccessibilityTestNode()
    with(validator, expected)
    assertAccessibilityTree(validator)
}

internal fun UIKitInstrumentedTest.findNodeWithTag(tag: String) = findNodeOrNull {
    it.identifier == tag
} ?: fail("Unable to find node with identifier: $tag")

internal fun UIKitInstrumentedTest.findNodeWithLabel(label: String) = findNodeOrNull {
    it.label == label
} ?: fail("Unable to find node with label: $label")

internal fun UIKitInstrumentedTest.firstAccessibleNode() =
    findNodeOrNull { it.isAccessibilityElement == true }
        ?: fail("Unable to find accessibility element")

internal fun UIKitInstrumentedTest.findNodeOrNull(
    isValid: (AccessibilityTestNode) -> Boolean
): AccessibilityTestNode? {
    waitForIdle()
    val actualTreeRoot = getAccessibilityTree()

    fun check(node: AccessibilityTestNode): AccessibilityTestNode? {
        return if (isValid(node)) {
            node
        } else {
            node.children?.firstNotNullOfOrNull(::check)
        }
    }

    return check(node = actualTreeRoot)
}

/**
 * Asserts that the current accessibility tree matches the expected structure defined in the
 * provided lambda. The expected structure is defined by configuring an `AccessibilityTestNode`,
 * which is then validated against the actual normalized accessibility tree. This function waits
 * for the UI to be idle before performing the validation.
 *
 * @param expected The expected accessibility tree structure represented by an instance of
 * `AccessibilityTestNode`.
 */
internal fun UIKitInstrumentedTest.assertAccessibilityTree(expected: AccessibilityTestNode) {
    waitForIdle()

    val actualTreeRoot = getAccessibilityTree()
    val normalizedTree = actualTreeRoot.normalized()

    try {
        expected.validate(normalizedTree)
    } catch (e: Throwable) {
        val message = "Unable to validate accessibility tree. Expected normalized tree:\n\n" +
            "${expected.printTree()}\n" +
            "Normalized tree:\n\n${normalizedTree?.printTree()}\n" +
            "Actual tree:\n\n${actualTreeRoot.printTree()}\n"
        println(message)

        throw e
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun CValue<CGRect>.toDpRect() = useContents {
    DpRect(
        left = origin.x.dp,
        top = origin.y.dp,
        right = origin.x.dp + size.width.dp,
        bottom = origin.y.dp + size.height.dp,
    )
}
