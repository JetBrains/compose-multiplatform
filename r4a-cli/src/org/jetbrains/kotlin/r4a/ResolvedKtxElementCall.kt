package org.jetbrains.kotlin.r4a.ast

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.r4a.CHILDREN_KEY
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.hasChildrenAnnotation
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

sealed class ValueNode(
    val name: String,
    val type: KotlinType,
    val descriptor: DeclarationDescriptor
)

class DefaultValueNode(
    name: String,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor)

class ImplicitCtorValueNode(
    name: String,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImplicitCtorValueNode

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class AttributeNode(
    name: String,
    var isStatic: Boolean,
    val expression: KtExpression,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeNode

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

enum class ValidationType {
    CHANGED,
    SET,
    UPDATE
}

sealed class Assignment(
    val assignment: ResolvedCall<*>?,
    val attribute: AttributeNode
)

class ValidatedAssignment(
    val validationType: ValidationType,
    val validationCall: ResolvedCall<*>?,
    assignment: ResolvedCall<*>?,
    attribute: AttributeNode
) : Assignment(assignment, attribute)

class ComposerCallInfo(
    val composerCall: ResolvedCall<*>?,
    val functionDescriptors: List<FunctionDescriptor?>,
    val pivotals: List<AttributeNode>,
    val joinKeyCall: ResolvedCall<*>?,
    val ctorCall: ResolvedCall<*>?,
    val ctorParams: List<ValueNode>,
    val validations: List<ValidatedAssignment>
) {
    fun allAttributes(): List<ValueNode> = ctorParams.filter { it !is ImplicitCtorValueNode } + validations.map { it.attribute }
}

class AttributeMeta(
    val name: String,
    val type: KotlinType,
    val isChildren: Boolean,
    val descriptor: DeclarationDescriptor
)

sealed class EmitOrCallNode {
    abstract fun allAttributes(): List<ValueNode>
    abstract fun print(): String
    fun resolvedCalls(): List<ResolvedCall<*>> {
        return when (this) {
            is ErrorNode -> emptyList()
            is MemoizedCallNode -> listOfNotNull(memoize?.ctorCall) + call.resolvedCalls()
            is NonMemoizedCallNode -> listOf(resolvedCall) + (nextCall?.resolvedCalls() ?: emptyList())
            is EmitCallNode -> listOfNotNull(memoize.ctorCall)
        }
    }

    fun allPossibleAttributes(): Map<String, List<AttributeMeta>> {
        val collector = mutableMapOf<String, MutableList<AttributeMeta>>()
        collectPossibleAttributes(collector)
        return collector
    }

    private fun collectPossibleAttributes(collector: MutableMap<String, MutableList<AttributeMeta>>) {
        when (this) {
            is ErrorNode -> {}
            is MemoizedCallNode -> {
                val callDescriptor = memoize?.ctorCall?.resultingDescriptor as? FunctionDescriptor
                callDescriptor?.let { collectAttributesFromFunction(it, collector) }
                val returnType = callDescriptor?.returnType
                returnType?.let { collectAttributesFromType(it, collector) }

                call.collectPossibleAttributes(collector)
            }
            is NonMemoizedCallNode -> {
                val callDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor
                callDescriptor?.let { collectAttributesFromFunction(it, collector) }
                val returnType = callDescriptor?.returnType
                returnType?.let { collectAttributesFromType(it, collector) }

                nextCall?.collectPossibleAttributes(collector)
            }
            is EmitCallNode -> {
                val callDescriptor = memoize.ctorCall?.resultingDescriptor as? FunctionDescriptor
                callDescriptor?.let { collectAttributesFromFunction(it, collector) }
                val returnType = callDescriptor?.returnType
                returnType?.let { collectAttributesFromType(it, collector) }
            }
        }
    }

    private fun collectAttributesFromFunction(fn: FunctionDescriptor, collector: MutableMap<String, MutableList<AttributeMeta>>) {
        fn.valueParameters.forEach {
            collector.multiPut(
                AttributeMeta(
                    name = it.name.asString(),
                    type = it.type,
                    descriptor = it,
                    isChildren = it.hasChildrenAnnotation()
                )
            )
        }
    }

    private fun collectAttributesFromType(type: KotlinType, collector: MutableMap<String, MutableList<AttributeMeta>>) {
        val cls = type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        cls.unsubstitutedMemberScope.getContributedDescriptors().forEach {
            when (it) {
                is SimpleFunctionDescriptor -> {
                    if (R4aUtils.isSetterMethodName(it.name.asString()) && it.valueParameters.size == 1) {
                        val name = R4aUtils.propertyNameFromSetterMethod(it.name.asString())
                        collector.multiPut(
                            AttributeMeta(
                                name = name,
                                type = it.valueParameters.first().type,
                                descriptor = it,
                                isChildren = it.hasChildrenAnnotation()
                            )
                        )
                    }
                }
                is PropertyDescriptor -> {
                    collector.multiPut(
                        AttributeMeta(
                            name = it.name.asString(),
                            type = it.type,
                            descriptor = it,
                            isChildren = it.hasChildrenAnnotation()
                        )
                    )
                }
            }
        }
    }

    fun errorNode(): ErrorNode? {
        return when (this) {
            is ErrorNode -> this
            is MemoizedCallNode -> call.errorNode()
            is NonMemoizedCallNode -> nextCall?.errorNode()
            is EmitCallNode -> null
        }
    }
}

sealed class CallNode : EmitOrCallNode()

sealed class ErrorNode : EmitOrCallNode() {
    override fun allAttributes(): List<ValueNode> = emptyList()
    override fun print(): String = "<ERROR:${javaClass.simpleName}>"

    class NonEmittableNonCallable(val type: KotlinType) : ErrorNode()
    class RecursionLimitError() : ErrorNode()
    class NonCallableRoot : ErrorNode()
}

class NonMemoizedCallNode(
    val resolvedCall: ResolvedCall<*>,
    val params: List<ValueNode>,
    var nextCall: EmitOrCallNode?
) : CallNode() {
    override fun allAttributes(): List<ValueNode> = params
    override fun print(): String = buildString {
        self("NonMemoizedCallNode")
        attr("resolvedCall", resolvedCall) { it.print() }
        attr("params", params) { it.print() }
        attr("nextCall", nextCall) { it.print() }
    }
}

class MemoizedCallNode(
    val memoize: ComposerCallInfo?,
    val call: EmitOrCallNode
) : CallNode() {
    override fun allAttributes(): List<ValueNode> = call.allAttributes() + (memoize?.allAttributes() ?: emptyList())
    override fun print(): String = buildString {
        self("MemoizedCallNode")
        attr("memoize", memoize) { it.print() }
        attr("call", call) { it.print() }
    }
}

class EmitCallNode(
    val memoize: ComposerCallInfo
) : EmitOrCallNode() {
    override fun allAttributes(): List<ValueNode> = memoize.allAttributes()
    override fun print() = buildString {
        self("EmitCallNode")
        attr("memoize", memoize) { it.print() }
    }
}

class ResolvedKtxElementCall(
    val usedAttributes: List<AttributeNode>,
    val unusedAttributes: List<String>,
    val emitOrCall: EmitOrCallNode,
    val getComposerCall: ResolvedCall<*>
)


fun ComposerCallInfo?.consumedAttributes(): List<AttributeNode> {
    if (this == null) return emptyList()
    return pivotals +
            ctorParams.mapNotNull { it as? AttributeNode } +
            validations.map { it.attribute }
}

fun EmitOrCallNode?.consumedAttributes(): List<AttributeNode> {
    return when (this) {
        is MemoizedCallNode -> memoize.consumedAttributes() + call.consumedAttributes()
        is NonMemoizedCallNode -> params.mapNotNull { it as? AttributeNode } + (nextCall?.consumedAttributes() ?: emptyList())
        is EmitCallNode -> memoize.consumedAttributes()
        is ErrorNode -> emptyList()
        null -> emptyList()
    }
}

fun List<ValueNode>.print(): String {
    return if (isEmpty()) "<empty>"
    else joinToString(", ") { it.print() }
}

fun ValueNode.print(): String = when (this) {
    is AttributeNode -> name
    is ImplicitCtorValueNode -> "(implicit)$name"
    is DefaultValueNode -> "(default)$name"
}

fun ResolvedKtxElementCall.print() = buildString {
    self("ResolvedKtxElementCall")
    attr("emitOrCall", emitOrCall) { it.print() }
    attr("usedAttributes", usedAttributes) { it.print() }
    attr("unusedAttributes", unusedAttributes) {
        it.joinToString(separator = ", ").let { s -> if (s.isBlank()) "<empty>" else s }
    }
}

fun ComposerCallInfo.print() = buildString {
    self("ComposerCallInfo")
    attr("composerCall", composerCall) { it.print() }
//    list("composerCall", functionDescriptors) { if (it != null) DESC_RENDERER.render(it) else "<null>" }
    attr("pivotals", pivotals) { it.print() }
    attr("joinKeyCall", joinKeyCall) { it.print() }
    attr("ctorCall", ctorCall) { it.print() }
    attr("ctorParams", ctorParams) { it.print() }
    list("validations", validations) { it.print() }
}

fun AttributeNode.print() = name
fun ResolvedCall<*>.print() = DESC_RENDERER.render(resultingDescriptor)
fun ValidatedAssignment.print() = buildString {
    self("ValidatedAssignment(${validationType.name})")
    attr("validationCall", validationCall) { it.print() }
    attr("assignment", assignment) { it.print() }
    attr("attribute", attribute) { it.print() }
}

val DESC_RENDERER = DescriptorRenderer.COMPACT_WITHOUT_SUPERTYPES.withOptions {
    parameterNamesInFunctionalTypes = false
    renderConstructorKeyword = false
    classifierNamePolicy = ClassifierNamePolicy.SHORT
    includeAdditionalModifiers = false
    unitReturnType = false
    withoutTypeParameters = true
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    defaultParameterValueRenderer = null
    renderUnabbreviatedType = false
}

fun StringBuilder.self(name: String) {
    append(name)
    appendln(":")
}

fun <T> StringBuilder.attr(name: String, obj: T?, printer: (T) -> String) {
    append("  ")
    append(name)
    append(" = ")
    if (obj == null) {
        appendln("<null>")
    } else {
        appendln(printer(obj).indentExceptFirstLine("  ").trim())
    }
}

fun String.indentExceptFirstLine(indent: String = "    "): String =
    lineSequence()
        .drop(1)
        .map {
            when {
                it.isBlank() -> {
                    when {
                        it.length < indent.length -> indent
                        else -> it
                    }
                }
                else -> indent + it
            }
        }
        .joinToString("\n", prefix = lineSequence().first() + "\n")

fun <T> StringBuilder.list(name: String, obj: Collection<T>, printer: (T) -> String) {
    append("  ")
    append(name)
    if (obj.isEmpty()) {
        appendln(" = <empty>")
    } else {
        appendln(" =")
        obj.forEach {
            append("    - ")
            appendln(printer(it).indentExceptFirstLine("      ").trim())
        }
    }
}

private fun <T, V> MutableMap<T, MutableList<V>>.multiPut(key: T, value: V) {
    val current = get(key)
    if (current != null) {
        current.push(value)
    } else {
        put(key, mutableListOf(value))
    }
}

private fun MutableMap<String, MutableList<AttributeMeta>>.multiPut(value: AttributeMeta) {
    multiPut(value.name, value)
    if (value.isChildren) {
        multiPut(CHILDREN_KEY, value)
    }
}

/*

fun ResolvedCall<*>.printCode(params: List<AttributeNode>): String {
    return ""
}

fun ValidatedAssignment.printCode(): String {
    val name = attribute.name
    var result = "${validationType.name.toLowerCase()}($name)"
    when (validationType) {
        ValidationType.UPDATE -> {
            result += " { $name = it }"
        }
        ValidationType.SET -> {
            result += " { $name = it }"
        }
        ValidationType.CHANGED -> Unit
    }
    return result
}

fun ComposerCallInfo.printCode(methodName: String, itName: String, block: StringBuilder.() -> Unit) = buildString {
    append(methodName)
    append("(")
    val joinKey = (listOf("#k") + pivotals.map { it.name }).joinToString(
        separator = ",",
        prefix = "jk(",
        postfix = ")"
    )
    val ctor = if (ctorCall == null) "null"
    else "{ ${ctorCall.printCode(ctorParams)} }"

    val invalid = validations.joinToString(
        separator = " + ",
        prefix = "{ ",
        postfix = " }",
        transform = ValidatedAssignment::printCode
    )

    appendln("call($joinKey, $ctor, $invalid) { $itName ->")
    val sb = StringBuilder()
    sb.block()
    appendln(sb.toString().prependIndent("  ").trim())
    appendln("}")

}

fun ResolvedKtxElementCall.printCode() = buildString {
    when (emitOrCall) {
        is MemoizedCallNode -> {
            append(emitOrCall.memoize?.printCode("call", "f") {
                append("f()")
            })
        }
        is NonMemoizedCallNode -> {

        }
        is EmitCallNode -> {

        }
    }
}

*/

