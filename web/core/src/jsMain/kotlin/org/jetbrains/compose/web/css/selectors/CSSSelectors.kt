@file:Suppress("EqualsOrHashCode", "unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.compose.web.css.selectors

import org.jetbrains.compose.web.css.LanguageCode

sealed class Nth {
    data class Functional(val a: Int? = null, val b: Int? = null) {
        override fun toString(): String = when {
            a != null && b != null -> "${a}n+$b"
            a != null -> "${a}n"
            b != null -> "$b"
            else -> ""
        }
    }
    object Odd : Nth() {
        override fun toString(): String = "odd"
    }
    object Even : Nth() {
        override fun toString(): String = "even"
    }
}

abstract class CSSSelector {
    override fun equals(other: Any?): Boolean {
        return this === other || asString() == (other as? CSSSelector)?.asString()
    }

    internal open fun contains(other: CSSSelector, strict: Boolean = false): Boolean {
        return if (strict) this === other else this == other
    }

    // This method made for workaround because of possible concatenation of `String + CSSSelector`,
    // so `toString` is called for such operator, but we are calling `asString` for instantiation.
    // `toString` is reloaded for CSSSelfSelector
    internal open fun asString(): String = toString()

    data class Raw(val selector: String) : CSSSelector() {
        override fun toString(): String = selector
    }

    object Universal : CSSSelector() {
        override fun toString(): String = "*"
    }

    data class Type(val type: String) : CSSSelector() {
        override fun toString(): String = type
    }

    data class CSSClass(val className: String) : CSSSelector() {
        override fun toString(): String = ".$className"
    }

    data class Id(val id: String) : CSSSelector() {
        override fun toString(): String = "#$id"
    }

    data class Attribute(
        val name: String,
        val value: String? = null,
        val operator: Operator = Operator.Equals,
        val caseSensitive: Boolean = true
    ) : CSSSelector() {
        enum class Operator(val value: String) {
            Equals("="),
            ListContains("~="),
            Hyphened("|="),
            Prefixed("^="),
            Suffixed("$="),
            Contains("*=")
        }
        override fun toString(): String {
            val valueStr = value?.let {
                "${operator.value}$value${if (!caseSensitive) " i" else ""}"
            } ?: ""
            return "[$name$valueStr]"
        }
    }

    data class Combine(val selectors: MutableList<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString("")
        override fun asString(): String = selectors.joinToString("") { it.asString() }
    }

    data class Group(val selectors: List<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString(", ")
        override fun asString(): String = selectors.joinToString(", ") { it.asString() }
    }

    data class Descendant(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent $selected"
        override fun asString(): String = "${parent.asString()} ${selected.asString()}"
    }

    data class Child(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent > $selected"
        override fun asString(): String = "${parent.asString()} > ${selected.asString()}"
    }

    data class Sibling(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev ~ $selected"
        override fun asString(): String = "${prev.asString()} ~ ${selected.asString()}"
    }

    data class Adjacent(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev + $selected"
        override fun asString(): String = "${prev.asString()} + ${selected.asString()}"
    }

    open class PseudoClass(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoClass) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }
        open fun argsStr(): String? = null
        override fun toString(): String = ":$name${argsStr()?.let { "($it)" } ?: ""}"

        companion object {
            // Location pseudo-classes
            val anyLink = PseudoClass("any-link")
            val link = PseudoClass("link")
            val visited = PseudoClass("visited")
            val localLink = PseudoClass("local-link")
            val target = PseudoClass("target")
            val targetWithin = PseudoClass("target-within")
            val scope = PseudoClass("scope")

            // User action pseudo-classes
            val hover = PseudoClass("hover")
            val active = PseudoClass("active")
            val focus = PseudoClass("focus")
            val focusVisible = PseudoClass("focus-visible")

            // Resource state pseudo-classes
            val playing = PseudoClass("playing")
            val paused = PseudoClass("paused")

            // The input pseudo-classes
            val autofill = PseudoClass("autofill")
            val enabled = PseudoClass("enabled")
            val disabled = PseudoClass("disabled")
            val readOnly = PseudoClass("read-only")
            val readWrite = PseudoClass("read-write")
            val placeholderShown = PseudoClass("placeholder-shown")
            val default = PseudoClass("default")
            val checked = PseudoClass("checked")
            val indeterminate = PseudoClass("indeterminate")
            val blank = PseudoClass("blank")
            val valid = PseudoClass("valid")
            val invalid = PseudoClass("invalid")
            val inRange = PseudoClass("in-range")
            val outOfRange = PseudoClass("out-of-range")
            val required = PseudoClass("required")
            val optional = PseudoClass("optional")
            val userInvalid = PseudoClass("user-invalid")

            // Tree-structural pseudo-classes
            val root = PseudoClass("root")
            val empty = PseudoClass("empty")
            val first = PseudoClass("first")
            val firstChild = PseudoClass("first-child")
            val lastChild = PseudoClass("last-child")
            val onlyChild = PseudoClass("only-child")
            val firstOfType = PseudoClass("first-of-type")
            val lastOfType = PseudoClass("last-of-type")
            val onlyOfType = PseudoClass("only-of-type")
            val host = PseudoClass("host")

            // Etc
            val defined = PseudoClass("defined")
            val left = PseudoClass("left")
            val right = PseudoClass("right")
        }

        // Linguistic pseudo-classes
        class Lang(val langCode: LanguageCode) : PseudoClass("lang") {
            override fun argsStr() = langCode
        }

        // Tree-structural pseudo-classes
        class NthChild(val nth: Nth) : PseudoClass("nth-child") {
            override fun argsStr() = "$nth"
        }
        class NthLastChild(val nth: Nth) : PseudoClass("nth-last-child") {
            override fun argsStr() = "$nth"
        }
        class NthOfType(val nth: Nth) : PseudoClass("nth-of-type") {
            override fun argsStr() = "$nth"
        }
        class NthLastOfType(val nth: Nth) : PseudoClass("nth-last-of-type") {
            override fun argsStr() = "$nth"
        }
        class Host(val selector: CSSSelector) : PseudoClass("host") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "${selector.asString()}"
        }

        // Etc
        class Not(val selector: CSSSelector) : PseudoClass("not") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "$selector"
        }
    }

    open class PseudoElement(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoElement) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }
        open fun argsStr(): String? = null
        override fun toString(): String = "::$name${argsStr()?.let { "($it)" } ?: ""}"

        companion object {
            val after = PseudoElement("after")
            val before = PseudoElement("before")
            val cue = PseudoElement("cue")
            val cueRegion = PseudoElement("cue-region")
            val firstLetter = PseudoElement("first-letter")
            val firstLine = PseudoElement("first-line")
            val fileSelectorButton = PseudoElement("file-selector-button")
            val selection = PseudoElement("selection")
        }

        class Slotted(val selector: CSSSelector) : PseudoElement("slotted") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = selector.asString()
        }
    }
}

@Suppress("SuspiciousEqualsCombination")
private fun contains(that: CSSSelector, other: CSSSelector, children: List<CSSSelector>, strict: Boolean): Boolean {
    return that === other || // exactly same selector
            children.any { it.contains(other, strict) } || // contains it in children
            (!strict && that == other) // equals structurally
}
