/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType
import org.jetbrains.kotlin.resolve.constants.evaluate.canEvalOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalBinaryOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalUnaryOp

/**
 * Computes constant values without running application code.
 * Follows local `val` references and supports built-in operations and constant branches.
 *
 * ```kotlin
 * const val APP_NAME = "Compose"
 *
 * @Composable
 * fun Page() {
 *     val label = "Hello, " + APP_NAME
 *     Div { Text(label) } // The text can be computed at compile time.
 * }
 * ```
 *
 * Kotlin resolves `const val` references before this pass.
 * Unsupported expressions and values that exceed the limits stay at runtime.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ConstantEvaluator(context: IrPluginContext) {
    private val builtIns = context.irBuiltIns
    private val compileTimeTypes = mapOf(
        builtIns.booleanClass to CompileTimeType.BOOLEAN,
        builtIns.byteClass to CompileTimeType.BYTE,
        builtIns.shortClass to CompileTimeType.SHORT,
        builtIns.intClass to CompileTimeType.INT,
        builtIns.longClass to CompileTimeType.LONG,
        builtIns.charClass to CompileTimeType.CHAR,
        builtIns.stringClass to CompileTimeType.STRING,
        builtIns.anyClass to CompileTimeType.ANY,
    )
    private val aliases = mutableMapOf<IrVariable, IrConst?>()
    private var work = MaxFoldSteps
    private var limited = false

    // Return null when the value cannot be safely computed at compile time.
    fun string(expression: IrExpression?): String? = evaluate(expression)?.value as? String

    fun stringWithoutNul(expression: IrExpression?): String? = string(expression)?.withoutNul()

    fun integer(expression: IrExpression?): Int? = evaluate(expression)?.value as? Int

    fun isConstantAlias(statement: IrStatement): Boolean =
        statement is IrVariable && !statement.isVar && evaluate(statement.initializer) != null

    private fun evaluate(expression: IrExpression?): IrConst? {
        work = MaxFoldSteps
        limited = false
        return foldPortableConstant(expression, 0)
    }

    private fun foldPortableConstant(value: IrExpression?, depth: Int): IrConst? =
        foldExpression(value, depth)?.takeIf(::isPortable)

    private fun isPortable(constant: IrConst): Boolean =
        // Compile-time floating-point formatting can differ from the runtime's formatNumber.
        constant.kind != IrConstKind.Float && constant.kind != IrConstKind.Double &&
                ((constant.value as? String)?.length ?: 0) <= MaxEvaluatedStringLength

    private fun foldExpression(value: IrExpression?, depth: Int): IrConst? {
        if (value == null) return null
        if (depth > MaxFoldDepth || --work < 0) {
            limited = true
            return null
        }
        return when (value) {
            is IrConst -> value
            is IrGetValue -> {
                val variable = value.symbol.owner as? IrVariable ?: return null
                if (variable.isVar) return null

                if (variable in aliases) return aliases[variable]
                val constant = foldPortableConstant(variable.initializer, depth + 1)
                if (!limited) aliases[variable] = constant
                constant
            }

            is IrStringConcatenation -> {
                val arguments = value.arguments.map { foldPortableConstant(it, depth + 1) ?: return null }
                // Bound concatenation before allocating its result.
                if (arguments.sumOf { (it.value?.toString() ?: "null").length.toLong() } > MaxEvaluatedStringLength) {
                    return null
                }
                constant(value, arguments.joinToString("") { it.value?.toString() ?: "null" })
            }

            is IrCall -> {
                val arguments = value.arguments.map { foldPortableConstant(it, depth + 1) ?: return null }
                try {
                    constant(value, evaluateCall(value, arguments.map { it.value }))
                } catch (_: RuntimeException) {
                    // Leave failures such as division by zero to runtime.
                    null
                }
            }

            is IrWhen -> {
                // Evaluate only the selected branch, preserving short-circuit behavior.
                for (branch in value.branches) {
                    val condition = foldPortableConstant(branch.condition, depth + 1)?.value as? Boolean ?: return null
                    if (condition) return foldPortableConstant(branch.result, depth + 1)
                }
                null
            }

            else -> null
        }
    }

    private fun evaluateCall(call: IrCall, arguments: List<Any?>): Any? = when (call.symbol) {
        builtIns.eqeqSymbol -> arguments[0] == arguments[1]
        in builtIns.lessFunByOperandType.values -> compare(arguments) < 0
        in builtIns.lessOrEqualFunByOperandType.values -> compare(arguments) <= 0
        in builtIns.greaterFunByOperandType.values -> compare(arguments) > 0
        in builtIns.greaterOrEqualFunByOperandType.values -> compare(arguments) >= 0
        else -> evaluateMember(call.symbol.owner, arguments)
    }

    private fun compare(arguments: List<Any?>): Int =
        compareValues(arguments[0] as Comparable<*>?, arguments[1] as Comparable<*>?)

    // Evaluate primitive/String members and floorDiv/mod. Property accessors stay at runtime.
    private fun evaluateMember(function: IrSimpleFunction, arguments: List<Any?>): Any? {
        if (function.correspondingPropertySymbol != null) return null
        val name = function.name.asString()
        if (function.dispatchReceiverParameter == null &&
            (function.parent !is IrPackageFragment || name !in setOf("floorDiv", "mod"))
        ) return null
        val types = function.parameters.map { compileTimeTypes[it.type.classOrNull] ?: return null }
        if (!canEvalOp(function.callableId, types[0], types.getOrNull(1))) return null
        val receiver = arguments[0] ?: return null
        return when (types.size) {
            1 -> evalUnaryOp(name, types[0], receiver)
            2 -> when {
                name == "equals" -> receiver == arguments[1]
                types[0] == CompileTimeType.STRING && name == "plus" -> (receiver as String) + arguments[1]
                else -> evalBinaryOp(name, types[0], receiver, types[1], arguments[1] ?: return null)
            }

            else -> null
        }
    }

    private fun constant(expression: IrExpression, value: Any?): IrConst? = with(expression) {
        when (value) {
            is Boolean -> IrConstImpl.boolean(startOffset, endOffset, type, value)
            is Byte -> IrConstImpl.byte(startOffset, endOffset, type, value)
            is Short -> IrConstImpl.short(startOffset, endOffset, type, value)
            is Int -> IrConstImpl.int(startOffset, endOffset, type, value)
            is Long -> IrConstImpl.long(startOffset, endOffset, type, value)
            is Char -> IrConstImpl.char(startOffset, endOffset, type, value)
            is String -> IrConstImpl.string(startOffset, endOffset, type, value)
            else -> null
        }
    }
}

// Leave strings containing NUL to runtime validation. Raw text uses separate normalization.
internal fun String.withoutNul(): String? = takeUnless { '\u0000' in it }
