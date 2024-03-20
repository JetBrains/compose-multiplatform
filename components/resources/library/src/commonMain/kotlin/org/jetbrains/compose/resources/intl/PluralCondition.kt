/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.intl

import kotlin.math.absoluteValue

internal sealed class PluralCondition {
    abstract fun isFulfilled(n: Int): Boolean

    // add isFulfilled(n: Double) or isFulfilled(n: Decimal) as needed

    class And(
        private val left: PluralCondition,
        private val right: PluralCondition,
    ) : PluralCondition() {
        override fun isFulfilled(n: Int): Boolean = left.isFulfilled(n) and right.isFulfilled(n)
        override fun toString(): String = "$left and $right"
    }

    class Or(
        private val left: PluralCondition,
        private val right: PluralCondition,
    ) : PluralCondition() {
        override fun isFulfilled(n: Int): Boolean = left.isFulfilled(n) or right.isFulfilled(n)
        override fun toString(): String = "$left or $right"
    }

    class Relation(
        private val operand: PluralOperand,
        private val operandDivisor: Int?,
        private val comparisonIsNegated: Boolean,
        private val ranges: Array<IntRange>,
    ) : PluralCondition() {
        override fun isFulfilled(n: Int): Boolean {
            val expressionOperandValue = when (operand) {
                PluralOperand.N, PluralOperand.I -> n.absoluteValue
                else -> 0
            }
            val moduloAppliedValue = if (operandDivisor != null) {
                expressionOperandValue % operandDivisor
            } else {
                expressionOperandValue
            }
            return ranges.any { moduloAppliedValue in it } != comparisonIsNegated
        }

        override fun toString(): String {
            return StringBuilder().run {
                append(operand.name.lowercase())
                if (operandDivisor != null) {
                    append(" % ")
                    append(operand)
                }
                append(' ')
                if (comparisonIsNegated) {
                    append('!')
                }
                append("= ")
                var first = true
                for (range in ranges) {
                    if (!first) {
                        append(',')
                    }
                    first = false
                    append(range.first)
                    if (range.first != range.last) {
                        append("..")
                        append(range.last)
                    }
                }
                toString()
            }
        }

        private class Parser(private val description: String) {
            private var currentIdx = 0

            private fun consumeWhitespaces() {
                while (currentIdx < description.length && description[currentIdx].isWhitespace()) {
                    currentIdx += 1
                }
            }

            private fun peekNextOrNull(): Char? {
                return description.getOrNull(currentIdx)
            }

            private fun peekNext(): Char {
                return peekNextOrNull() ?: throw PluralConditionParseException(description)
            }

            private fun consumeNext(): Char {
                val next = peekNext()
                currentIdx += 1
                return next
            }

            private fun consumeNextInt(): Int {
                if (!peekNext().isDigit()) throw PluralConditionParseException(description)
                var integerValue = 0
                var integerLastIdx = currentIdx
                while (integerLastIdx < description.length && description[integerLastIdx].isDigit()) {
                    integerValue *= 10
                    integerValue += description[integerLastIdx] - '0'
                    integerLastIdx += 1
                }
                currentIdx = integerLastIdx
                return integerValue
            }

            fun nextOperand(): PluralOperand {
                consumeWhitespaces()
                return when (consumeNext()) {
                    'n' -> PluralOperand.N
                    'i' -> PluralOperand.I
                    'f' -> PluralOperand.F
                    't' -> PluralOperand.T
                    'v' -> PluralOperand.V
                    'w' -> PluralOperand.W
                    else -> throw PluralConditionParseException(description)
                }
            }

            fun nextModulusDivisor(): Int? {
                consumeWhitespaces()
                if (peekNext() == '%') {
                    consumeNext()
                    consumeWhitespaces()
                    return consumeNextInt()
                }
                return null
            }

            /**
             * Returns `true` for `!=`, `false` for `=`.
             */
            fun nextComparisonIsNegated(): Boolean {
                consumeWhitespaces()
                when (peekNext()) {
                    '!' -> {
                        consumeNext()
                        if (consumeNext() != '=') throw PluralConditionParseException(description)
                        return true
                    }

                    '=' -> {
                        consumeNext()
                        return false
                    }

                    else -> throw PluralConditionParseException(description)
                }
            }

            /**
             * Returns `number..number` if the range is actually a value.
             */
            fun nextRange(): IntRange {
                consumeWhitespaces()
                val start = consumeNextInt()
                if (peekNextOrNull() != '.') {
                    return start..start
                }
                consumeNext()
                if (peekNext() != '.') throw PluralConditionParseException(description)
                val endInclusive = consumeNextInt()
                return start..endInclusive
            }

            fun nextCommaOrNull(): Char? {
                return when (peekNextOrNull()) {
                    ',' -> ','
                    null -> null
                    else -> throw PluralConditionParseException(description)
                }
            }
        }

        companion object {
            /**
             * Syntax:
             * ```
             * relation     = operand ('%' value)? ('=' | '!=') range_list
             * operand      = 'n' | 'i' | 'f' | 't' | 'v' | 'w'
             * range_list   = (range | value) (',' range_list)*
             * range        = value'..'value
             * value        = digit+
             * digit        = [0-9]
             * ```
             */
            fun parse(description: String): Relation {
                val parser = Parser(description)
                val operand = parser.nextOperand()
                val divisor = parser.nextModulusDivisor()
                val negated = parser.nextComparisonIsNegated()
                val ranges = mutableListOf<IntRange>()
                while (true) {
                    ranges.add(parser.nextRange())
                    if (parser.nextCommaOrNull() == null) break
                }
                // ranges is not empty here
                return Relation(operand, divisor, negated, ranges.toTypedArray())
            }
        }
    }

    companion object {
        /**
         * Parses [description] as defined in the [Unicode Plural rules syntax](https://unicode.org/reports/tr35/tr35-numbers.html#Plural_rules_syntax).
         * For compact implementation, samples and keywords for backward compatibility are also not handled. You can
         * find such keywords in the [Relations Examples](https://unicode.org/reports/tr35/tr35-numbers.html#Relations_Examples) section.
         * ```
         * condition       = and_condition ('or' and_condition)*
         * and_condition   = relation ('and' relation)*
         * relation        = operand ('%' value)? ('=' | '!=') range_list
         * operand         = 'n' | 'i' | 'f' | 't' | 'v' | 'w'
         * range_list      = (range | value) (',' range_list)*
         * range           = value'..'value
         * value           = digit+
         * digit           = [0-9]
         * ```
         * See [Relation.parse] for the remaining part of the syntax.
         */
        fun parse(description: String): PluralCondition {
            var condition: PluralCondition? = null
            val orConditionDescriptions = OR_PATTERN.split(description.trim())
            if (orConditionDescriptions.isNotEmpty() && orConditionDescriptions[0].isNotEmpty()) {
                for (orConditionDescription in orConditionDescriptions) {
                    if (orConditionDescription.isEmpty()) throw PluralConditionParseException(description)
                    var andCondition: PluralCondition? = null
                    val andConditionDescriptions = AND_PATTERN.split(orConditionDescription.trim())
                    for (relationDescription in andConditionDescriptions) {
                        val relation = Relation.parse(relationDescription)
                        andCondition = if (andCondition == null)
                            relation
                        else
                            And(andCondition, relation)
                    }
                    if (andCondition == null) throw PluralConditionParseException(description)
                    condition = if (condition == null) andCondition else Or(condition, andCondition)
                }
            }
            return condition ?: NoCondition
        }

        private val AND_PATTERN = Regex("""\s*and\s*""")
        private val OR_PATTERN = Regex("""\s*or\s*""")
    }
}

internal object NoCondition : PluralCondition() {
    override fun isFulfilled(n: Int): Boolean = true
    override fun toString(): String = ""
}