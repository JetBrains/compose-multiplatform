package com.google.r4a.examples.explorerapp.calculator


import org.mariuszgromada.math.mxparser.Expression
import java.lang.Double.isNaN

import java.text.DecimalFormat
import java.util.regex.Pattern

/**
 * A wrapper around a calculator expression (like "2×5+3"), provides accessor function for building,
 * parsing, and evaluating math strings.
 */

class CalculatorFormula {

    /** Returns the formula to be displayed on the calculator's formula box  */
    var formulaString = ""
        internal set // A calculator string like "3+2*5"

    /** Returns a preview of the result, if one exists  */
    // For trivially simple formulas, don't show the preview
    // Clean up the formula for evaluation (remove/ignore a training operator)
    // We use fancy characters because they look prettier, swap them out when performing math
    // Format the result to look pretty
    val previewString: String
        get() {
            if (!formulaString.contains(MULTIPLY) && !formulaString.contains(DIVIDE) && !formulaString.contains(SUBTRACT) && !formulaString.contains(ADD)) return ""
            var formula = this.formulaString
            if (formula.endsWith(MULTIPLY) || formula.endsWith(DIVIDE) || formula.endsWith(SUBTRACT) || formula.endsWith(ADD)) {
                formula = formula.substring(0, formula.length - 1)
            }
            formula = formula.replace(Pattern.quote(MULTIPLY).toRegex(), "*")
            formula = formula.replace(Pattern.quote(DIVIDE).toRegex(), "/")
            formula = formula.replace(Pattern.quote(SUBTRACT).toRegex(), "-")
            formula = formula.replace(Pattern.quote(ADD).toRegex(), "+")
            return DecimalFormat("#.#######").format(Expression(formula).calculate())
        }

    /** Append an operator to the end of the formula  */
    fun appendOperator(operator: String) {
        // Replace any trailing operators, if one exists
        if (formulaString.length > 0 && isOperator(formulaString[formulaString.length - 1]))
            formulaString = formulaString.substring(0, formulaString.length - 1)

        formulaString = formulaString + operator
    }

    /** Remove the last digit from the end of the formula  */
    fun backspace() {
        if (formulaString.length == 0) return
        formulaString = formulaString.substring(0, formulaString.length - 1)
    }

    fun append(value: String) {
        when {
            "." == value -> appendDecimalPoint()
            "=" == value -> appendEquals()
            DELETE == value -> backspace()
            isOperator(value) -> appendOperator(value)
            else -> appendDigit(value)
        }
    }

    /** Append a digit to the end of the formula  */
    fun appendDigit(digit: String) {
        formulaString = formulaString + digit
    }

    fun appendDecimalPoint() {
        var lastTermContainsDecimal = false
        var hasDigits = false
//        for (index in formulaString.length - 1 downTo 0) {
//            val c = formulaString[index]
//            if (isOperator(c)) break
//            if (c >= '0' && c <= '9') hasDigits = true
//            if (c == '.') lastTermContainsDecimal = true
//        }

        if (!lastTermContainsDecimal) {
            if (!hasDigits) formulaString = formulaString + "0"
            formulaString = "$formulaString."
        }
    }

    /** User should invoke this method to request that the result be placed in the formula box  */
    fun appendEquals() {
        try {
            if (!isNaN(java.lang.Double.parseDouble(previewString))) formulaString = previewString
        } catch (e: NumberFormatException) {
//            // Do nothing if there is an error
        }

    }

    companion object {

        const val ADD = "+"
        const val SUBTRACT = "−"
        const val MULTIPLY = "×"
        const val DIVIDE = "÷"
        const val DELETE = "⌫"

        private fun isOperator(c: Char): Boolean {
            return isOperator(Character.toString(c))
        }

        fun isOperator(c: String): Boolean {
            return MULTIPLY == c || DIVIDE == c || ADD == c || SUBTRACT == c
        }
    }
}
