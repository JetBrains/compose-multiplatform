/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.plural

import kotlin.math.absoluteValue

internal class PluralRuleParseException(description: String, position: Int) :
    Exception("Invalid syntax at position $position: $description")

internal class PluralRule private constructor(val category: PluralCategory, private val condition: Condition) {

    constructor(category: PluralCategory, condition: String) : this(category, Condition.parse(condition))

    fun appliesTo(n: Int): Boolean {
        return condition.isFulfilled(n)
    }

    private sealed class Condition {
        abstract fun isFulfilled(n: Int): Boolean

        abstract fun simplifyForInteger(): Condition

        abstract fun equivalentForInteger(other: Condition): Boolean

        /**
         * Plural operands defined in the [Unicode Locale Data Markup Language](https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings).
         */
        enum class Operand {
            /**
             * The absolute value of the source number.
             */
            N,

            /**
             * The integer digits of the source number.
             */
            I,

            /**
             * The number of visible fraction digits in the source number, *with* trailing zeros.
             */
            V,

            /**
             * The number of visible fraction digits in the source number, *without* trailing zeros.
             */
            W,

            /**
             * The visible fraction digits in the source number, *with* trailing zeros, expressed as an integer.
             */
            F,

            /**
             * The visible fraction digits in the source number, *without* trailing zeros, expressed as an integer.
             */
            T,

            /**
             * Compact decimal exponent value: exponent of the power of 10 used in compact decimal formatting.
             */
            C,
        }

        class And(
            private val left: Condition,
            private val right: Condition,
        ) : Condition() {
            override fun isFulfilled(n: Int): Boolean = left.isFulfilled(n) && right.isFulfilled(n)

            override fun simplifyForInteger(): Condition {
                val leftSimplified = left.simplifyForInteger()
                if (leftSimplified == False) return False

                val rightSimplified = right.simplifyForInteger()
                when {
                    leftSimplified == True -> return rightSimplified
                    rightSimplified == False -> return False
                    rightSimplified == True -> return leftSimplified
                }

                if (leftSimplified.equivalentForInteger(rightSimplified)) return leftSimplified
                return And(leftSimplified, rightSimplified)
            }

            override fun equivalentForInteger(other: Condition): Boolean {
                if (this === other) return true
                if (other !is And) return false
                return left.equivalentForInteger(other.left) && right.equivalentForInteger(other.right)
            }

            override fun toString(): String = "$left and $right"
        }

        class Or(
            private val left: Condition,
            private val right: Condition,
        ) : Condition() {
            override fun isFulfilled(n: Int): Boolean = left.isFulfilled(n) || right.isFulfilled(n)

            override fun simplifyForInteger(): Condition {
                val leftSimplified = left.simplifyForInteger()
                if (leftSimplified == True) return True

                val rightSimplified = right.simplifyForInteger()
                when {
                    leftSimplified == False -> return rightSimplified
                    rightSimplified == True -> return True
                    rightSimplified == False -> return leftSimplified
                }

                if (leftSimplified.equivalentForInteger(rightSimplified)) return leftSimplified
                return Or(leftSimplified, rightSimplified)
            }

            override fun equivalentForInteger(other: Condition): Boolean {
                if (this === other) return true
                if (other !is Or) return false
                return left.equivalentForInteger(other.left) && right.equivalentForInteger(other.right)
            }

            override fun toString(): String = "$left or $right"
        }

        class Relation(
            private val operand: Operand,
            private val operandDivisor: Int?,
            private val comparisonIsNegated: Boolean,
            private val ranges: Array<IntRange>,
        ) : Condition() {
            override fun isFulfilled(n: Int): Boolean {
                val expressionOperandValue = when (operand) {
                    Operand.N, Operand.I -> n.absoluteValue
                    else -> 0
                }
                val moduloAppliedValue = if (operandDivisor != null) {
                    expressionOperandValue % operandDivisor
                } else {
                    expressionOperandValue
                }
                return ranges.any { moduloAppliedValue in it } != comparisonIsNegated
            }

            override fun simplifyForInteger(): Condition {
                return when (operand) {
                    Operand.N, Operand.I -> Relation(
                        Operand.N,
                        operandDivisor,
                        comparisonIsNegated,
                        ranges,
                    )

                    else -> if (ranges.any { 0 in it } != comparisonIsNegated) True else False
                }
            }

            override fun equivalentForInteger(other: Condition): Boolean {
                if (this === other) return true
                if (other !is Relation) return false
                if ((operand == Operand.N || operand == Operand.I) != (other.operand == Operand.N || other.operand == Operand.I)) return false
                if (operandDivisor != other.operandDivisor) return false
                if (comparisonIsNegated != other.comparisonIsNegated) return false
                if (!ranges.contentEquals(other.ranges)) return false
                return true
            }

            override fun toString(): String {
                return StringBuilder().run {
                    append(operand.name.lowercase())
                    if (operandDivisor != null) {
                        append(" % ")
                        append(operandDivisor)
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
        }

        private object True : Condition() {
            override fun isFulfilled(n: Int) = true
            override fun simplifyForInteger() = this
            override fun equivalentForInteger(other: Condition) = this == other
            override fun toString(): String = ""
        }

        private object False : Condition() {
            override fun isFulfilled(n: Int) = false
            override fun simplifyForInteger() = this
            override fun equivalentForInteger(other: Condition) = this == other
            override fun toString(): String = "(false)"
        }

        private class Parser(private val description: String) {
            private var currentIdx = 0

            private fun eof() = currentIdx >= description.length

            private fun nextUnchecked() = description[currentIdx]

            private fun consumeWhitespaces() {
                while (!eof() && nextUnchecked().isWhitespace()) {
                    currentIdx += 1
                }
            }

            private fun raise(): Nothing = throw PluralRuleParseException(description, currentIdx + 1)

            private fun assert(condition: Boolean) {
                if (!condition) raise()
            }

            private fun peekNextOrNull() = description.getOrNull(currentIdx)

            private fun peekNext() = peekNextOrNull() ?: raise()

            private fun consumeNext(): Char {
                val next = peekNext()
                currentIdx += 1
                return next
            }

            private fun consumeNextInt(): Int {
                assert(peekNext().isDigit())
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

            fun parse(): Condition {
                consumeWhitespaces()
                if (eof()) return True
                val condition = nextCondition()
                consumeWhitespaces()
                assert(eof())
                return condition
            }

            /**
             * Syntax:
             * ```
             * condition = and_condition ('or' and_condition)*
             * ```
             */
            private fun nextCondition(): Condition {
                var condition: Condition = nextAndCondition()
                while (true) {
                    consumeWhitespaces()
                    if (peekNextOrNull() != 'o') break
                    consumeNext()
                    assert(consumeNext() == 'r')
                    condition = Or(condition, nextAndCondition())
                }
                return condition
            }

            /**
             * Syntax:
             * ```
             * and_condition = relation ('and' relation)*
             * ```
             */
            private fun nextAndCondition(): Condition {
                var condition: Condition = nextRelation()
                while (true) {
                    consumeWhitespaces()
                    if (peekNextOrNull() != 'a') break
                    consumeNext()
                    assert(consumeNext() == 'n')
                    assert(consumeNext() == 'd')
                    condition = And(condition, nextRelation())
                }
                return condition
            }

            /**
             * Syntax:
             * ```
             * relation = operand ('%' value)? ('=' | '!=') range_list
             * ```
             */
            fun nextRelation(): Relation {
                val operand = nextOperand()
                val divisor = nextModulusDivisor()
                val negated = nextComparisonIsNegated()
                val ranges = mutableListOf(nextRange())
                while (peekNextOrNull() == ',') {
                    consumeNext()
                    ranges.add(nextRange())
                }
                // ranges is not empty here
                return Relation(operand, divisor, negated, ranges.toTypedArray())
            }

            /**
             * Syntax:
             * ```
             * operand = 'n' | 'i' | 'f' | 't' | 'v' | 'w'
             * ```
             */
            fun nextOperand(): Operand {
                consumeWhitespaces()
                return when (consumeNext()) {
                    'n' -> Operand.N
                    'i' -> Operand.I
                    'f' -> Operand.F
                    't' -> Operand.T
                    'v' -> Operand.V
                    'w' -> Operand.W
                    'c', 'e' -> Operand.C
                    else -> raise()
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
                        assert(consumeNext() == '=')
                        return true
                    }

                    '=' -> {
                        consumeNext()
                        return false
                    }

                    else -> raise()
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
                assert(consumeNext() == '.')
                val endInclusive = consumeNextInt()
                return start..endInclusive
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
             */
            fun parse(description: String): Condition = Parser(description).parse().simplifyForInteger()
        }
    }
}