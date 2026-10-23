/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

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
    private val interpreter = IrInterpreter(
        IrInterpreterEnvironment(
            context.irBuiltIns,
            IrInterpreterConfiguration(
                platform = context.platform,
                maxStack = MaxInterpreterStackDepth,
                maxCommands = MaxInterpreterCommands,
            )
        )
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
                // Bound concatenation before asking the interpreter to allocate its result.
                if (arguments.sumOf { (it.value?.toString() ?: "null").length.toLong() } > MaxEvaluatedStringLength) {
                    return null
                }
                val copy = IrStringConcatenationImpl(value.startOffset, value.endOffset, value.type)
                copy.arguments.addAll(arguments)
                interpreter.interpret(copy) as? IrConst
            }
            is IrCall -> {
                // OnlyBuiltins also accepts property accessors. Do not evaluate those here.
                if (value.symbol.owner.correspondingPropertySymbol != null ||
                    !EvaluationMode.OnlyBuiltins.canEvaluateFunction(value.symbol.owner)
                ) return null

                val copy = IrCallImpl(
                    value.startOffset, value.endOffset, value.type, value.symbol,
                    typeArgumentsCount = value.typeArguments.size
                )
                for (index in value.typeArguments.indices) copy.typeArguments[index] = value.typeArguments[index]
                for (index in value.arguments.indices) {
                    copy.arguments[index] = foldPortableConstant(value.arguments[index], depth + 1) ?: return null
                }
                interpreter.interpret(copy) as? IrConst
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
}

// Leave strings containing NUL to runtime validation. Raw text uses separate normalization.
internal fun String.withoutNul(): String? = takeUnless { '\u0000' in it }
