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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import com.google.common.base.Objects
import org.junit.Assert

internal fun chainTester() = NodeChainTester()

class DiffLog() {
    val oplog = mutableListOf<DiffOp>()
    fun op(op: DiffOp) = oplog.add(op)
    fun clear() = oplog.clear()

    fun assertElementDiff(expected: String) {
        Assert.assertEquals(
            expected,
            oplog.reversed().joinToString("\n") {
                it.elementDiffString()
            }
        )
    }

    fun debug(): String = buildString {
        for (op in oplog) {
            appendLine(op.debug())
        }
    }
}

internal class NodeChainTester : NodeChain.Logger {
    val layoutNode = LayoutNode()
    val chain = layoutNode.nodes.also { it.useLogger(this) }
    val log = DiffLog()

    val tail get() = chain.tail
    val head get() = chain.head
    val nodes: List<Modifier.Node>
        get() {
            val result = mutableListOf<Modifier.Node>()
            chain.headToTailExclusive {
                result.add(it)
            }
            return result
        }

    val aggregateChildMasks: List<Int> get() = nodes.map { it.aggregateChildKindSet }

    fun clearLog(): NodeChainTester {
        log.clear()
        return this
    }

    fun debug(): NodeChainTester {
        if (true) error(log.debug())
        return this
    }

    fun withModifiers(vararg modifiers: Modifier): NodeChainTester {
        chain.updateFrom(modifierOf(*modifiers))
        return this
    }

    fun assertStringEquals(expected: String): NodeChainTester {
        Assert.assertEquals(expected, chain.toString())
        return this
    }

    fun assertElementDiff(expected: String): NodeChainTester {
        log.assertElementDiff(expected)
        return this
    }

    override fun linearDiffAborted(
        index: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node
    ) {
        // TODO
    }

    override fun nodeUpdated(
        oldIndex: Int,
        newIndex: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        before: Modifier.Node,
        after: Modifier.Node
    ) {
        log.op(DiffOp.Same(oldIndex, newIndex, prev, next, before, after, true))
    }

    override fun nodeReused(
        oldIndex: Int,
        newIndex: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node
    ) {
        log.op(DiffOp.Same(oldIndex, newIndex, prev, next, node, node, false))
    }

    override fun nodeInserted(
        atIndex: Int,
        newIndex: Int,
        element: Modifier.Element,
        child: Modifier.Node,
        inserted: Modifier.Node
    ) {
        log.op(DiffOp.Insert(atIndex, newIndex, element, child, inserted))
    }

    override fun nodeRemoved(oldIndex: Int, element: Modifier.Element, node: Modifier.Node) {
        log.op(DiffOp.Remove(oldIndex, element, node))
    }
}

sealed class DiffOp(
    val element: Modifier.Element,
    val opChar: String,
    val opString: String,
) {
    fun elementDiffString(): String {
        return "$opChar$element"
    }

    abstract fun debug(): String
    class Same(
        val oldIndex: Int,
        val newIndex: Int,
        val beforeEl: Modifier.Element,
        val afterEl: Modifier.Element,
        val beforeEntity: Modifier.Node,
        val afterEntity: Modifier.Node,
        val updated: Boolean,
    ) : DiffOp(beforeEl, if (updated) "*" else " ", "Same") {
        override fun debug() = """
            <$opString>
                $beforeEl @ $oldIndex = $afterEl @ $newIndex
                before = $beforeEntity
                after = $afterEntity
                updated? = $updated
            </$opString>
        """.trimIndent()
    }

    class Insert(
        val oldIndex: Int,
        val newIndex: Int,
        val afterEl: Modifier.Element,
        val child: Modifier.Node,
        val inserted: Modifier.Node,
    ) : DiffOp(afterEl, "+", "Insert") {
        override fun debug() = """
            <$opString>
                $afterEl @ $newIndex (inserted at $oldIndex)
                child = $child
                inserted = $inserted
            </$opString>
        """.trimIndent()
    }

    class Remove(
        val oldIndex: Int,
        val beforeEl: Modifier.Element,
        val beforeEntity: Modifier.Node,
    ) : DiffOp(beforeEl, "-", "Remove") {
        override fun debug() = """
            <$opString>
                $beforeEl @ $oldIndex
                beforeEntity = $beforeEntity
            </$opString>
        """.trimIndent()
    }
}

fun modifierOf(vararg modifiers: Modifier): Modifier {
    var result: Modifier = Modifier
    for (m in modifiers) {
        result = result.then(m)
    }
    return result
}

fun reusableModifier(name: String): Modifier.Element = object : Modifier.Element {
    override fun toString(): String = name
}

fun reusableModifiers(vararg names: String): List<Modifier.Element> {
    return names.map { reusableModifier(it) }
}

class A : Modifier.Node() {
    override fun toString(): String = "a"
}

fun modifierA(params: Any? = null): Modifier.Element {
    return object : TestModifierElement<A>("a", params, A()) {}
}

class B : Modifier.Node() {
    override fun toString(): String = "b"
}

fun modifierB(params: Any? = null): Modifier.Element {
    return object : TestModifierElement<B>("b", params, B()) {}
}

class C : Modifier.Node() {
    override fun toString(): String = "c"
}

fun modifierC(params: Any? = null): Modifier.Element {
    return object : TestModifierElement<C>("c", params, C()) {}
}

fun modifierD(params: Any? = null): Modifier.Element {
    return object : TestModifierElement<Modifier.Node>("d", params,
        object : Modifier.Node() {}
    ) {}
}

fun managedModifier(
    name: String,
    params: Any? = null
): ModifierNodeElement<*> = object : TestModifierElement<Modifier.Node>(name, params,
    object : Modifier.Node() {}
) {}

private abstract class TestModifierElement<T : Modifier.Node>(
    val modifierName: String,
    val param: Any? = null,
    val node: T
) : ModifierNodeElement<T>() {
    override fun create(): T = node
    override fun update(node: T): T = this.node
    override fun InspectorInfo.inspectableProperties() {
        name = modifierName
    }

    override fun hashCode() = Objects.hashCode(modifierName, param)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is TestModifierElement<*> &&
            javaClass == other.javaClass &&
            modifierName == other.modifierName &&
            param == other.param
    }

    override fun toString() = modifierName
}

fun entityModifiers(vararg names: String): List<ModifierNodeElement<*>> {
    return names.map { managedModifier(it, null) }
}

fun assertReused(before: Modifier.Element, after: Modifier.Element) {
    with(chainTester()) {
        withModifiers(before)
        val beforeEntity = chain.tail
        withModifiers(after)
        val afterEntity = chain.tail
        assert(beforeEntity === afterEntity)
    }
}