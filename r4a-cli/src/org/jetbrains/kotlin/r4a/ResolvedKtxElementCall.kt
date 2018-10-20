package org.jetbrains.kotlin.r4a.ast

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType




class AttributeNode(
    val name: String,
    val isStatic: Boolean,
    val expression: KtExpression,
    val type: KotlinType,
    val descriptor: DeclarationDescriptor
)

enum class ValidationType {
    CHANGED,
    SET,
    UPDATE
}

class ValidationNode(
    val validationType: ValidationType,
    val resolvedCall: ResolvedCall<*>,
    val attribute: AttributeNode,
    val assignment: ResolvedCall<*>
)

class StaticAssignment(
    val resolvedCall: ResolvedCall<*>,
    val attribute: AttributeNode
)

class ComposerCallNode(
    val pivotals: List<AttributeNode>,
    val joinKeyCall: ResolvedCall<*>,
    val ctorCall: ResolvedCall<*>?,
    val ctorParams: List<AttributeNode>,
    val staticAssignments: List<StaticAssignment>,
    val validations: List<ValidationNode>
)

sealed class RecursiveCallNode

class CallNode(
    val resolvedCall: ResolvedCall<*>,
    val params: List<AttributeNode>
): RecursiveCallNode()

class MemoizedCallNode(
    val memoize: ComposerCallNode?,
    val call: RecursiveCallNode
): RecursiveCallNode()

class EmitCallNode(
    val memoize: ComposerCallNode
)

class ResolvedKtxElementCall(
    val attributes: List<AttributeNode>,
    val emit: EmitCallNode?,
    val call: RecursiveCallNode?
)
