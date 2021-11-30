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

abstract class CSSSelector internal constructor() {
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

    data class Raw internal constructor(val selector: String) : CSSSelector() {
        override fun toString(): String = selector
    }

    internal object Universal : CSSSelector() {
        override fun toString(): String = "*"
    }

    data class Type internal constructor(val type: String) : CSSSelector() {
        override fun toString(): String = type
    }

    data class CSSClass internal constructor(val className: String) : CSSSelector() {
        override fun toString(): String = ".$className"
    }

    data class Id internal constructor(val id: String) : CSSSelector() {
        override fun toString(): String = "#$id"
    }

    data class Attribute internal constructor(
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

    data class Combine internal constructor(val selectors: MutableList<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString("")
        override fun asString(): String = selectors.joinToString("") { it.asString() }
    }

    data class Group internal constructor(val selectors: List<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString(", ")
        override fun asString(): String = selectors.joinToString(", ") { it.asString() }
    }

    data class Descendant internal constructor(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent $selected"
        override fun asString(): String = "${parent.asString()} ${selected.asString()}"
    }

    data class Child internal constructor(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent > $selected"
        override fun asString(): String = "${parent.asString()} > ${selected.asString()}"
    }

    data class Sibling internal constructor(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev ~ $selected"
        override fun asString(): String = "${prev.asString()} ~ ${selected.asString()}"
    }

    data class Adjacent internal constructor(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev + $selected"
        override fun asString(): String = "${prev.asString()} + ${selected.asString()}"
    }

    open class PseudoClass internal constructor(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoClass) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }
        open fun argsStr(): String? = null
        override fun toString(): String = ":$name${argsStr()?.let { "($it)" } ?: ""}"

        companion object {
            // Location pseudo-classes
            @Deprecated(webCssSelectorsDeprecationMessage)
            val anyLink = PseudoClass("any-link")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val link = PseudoClass("link")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val visited = PseudoClass("visited")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val localLink = PseudoClass("local-link")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val target = PseudoClass("target")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val targetWithin = PseudoClass("target-within")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val scope = PseudoClass("scope")

            // User action pseudo-classes
            @Deprecated(webCssSelectorsDeprecationMessage)
            val hover = PseudoClass("hover")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val active = PseudoClass("active")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val focus = PseudoClass("focus")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val focusVisible = PseudoClass("focus-visible")

            // Resource state pseudo-classes
            @Deprecated(webCssSelectorsDeprecationMessage)
            val playing = PseudoClass("playing")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val paused = PseudoClass("paused")

            // The input pseudo-classes
            @Deprecated(webCssSelectorsDeprecationMessage)
            val autofill = PseudoClass("autofill")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val enabled = PseudoClass("enabled")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val disabled = PseudoClass("disabled")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val readOnly = PseudoClass("read-only")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val readWrite = PseudoClass("read-write")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val placeholderShown = PseudoClass("placeholder-shown")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val default = PseudoClass("default")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val checked = PseudoClass("checked")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val indeterminate = PseudoClass("indeterminate")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val blank = PseudoClass("blank")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val valid = PseudoClass("valid")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val invalid = PseudoClass("invalid")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val inRange = PseudoClass("in-range")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val outOfRange = PseudoClass("out-of-range")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val required = PseudoClass("required")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val optional = PseudoClass("optional")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val userInvalid = PseudoClass("user-invalid")

            // Tree-structural pseudo-classes
            @Deprecated(webCssSelectorsDeprecationMessage)
            val root = PseudoClass("root")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val empty = PseudoClass("empty")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val first = PseudoClass("first")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val firstChild = PseudoClass("first-child")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val lastChild = PseudoClass("last-child")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val onlyChild = PseudoClass("only-child")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val firstOfType = PseudoClass("first-of-type")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val lastOfType = PseudoClass("last-of-type")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val onlyOfType = PseudoClass("only-of-type")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val host = PseudoClass("host")

            // Etc
            @Deprecated(webCssSelectorsDeprecationMessage)
            val defined = PseudoClass("defined")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val left = PseudoClass("left")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val right = PseudoClass("right")
        }

        // Linguistic pseudo-classes
        class Lang internal constructor(val langCode: LanguageCode) : PseudoClass("lang") {
            override fun argsStr() = langCode
        }

        // Tree-structural pseudo-classes
        class NthChild internal constructor(val nth: Nth) : PseudoClass("nth-child") {
            override fun argsStr() = "$nth"
        }
        class NthLastChild internal constructor(val nth: Nth) : PseudoClass("nth-last-child") {
            override fun argsStr() = "$nth"
        }
        class NthOfType internal constructor(val nth: Nth) : PseudoClass("nth-of-type") {
            override fun argsStr() = "$nth"
        }
        class NthLastOfType internal constructor(val nth: Nth) : PseudoClass("nth-last-of-type") {
            override fun argsStr() = "$nth"
        }
        class Host internal constructor(val selector: CSSSelector) : PseudoClass("host") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "${selector.asString()}"
        }

        // Etc
        class Not internal constructor(val selector: CSSSelector) : PseudoClass("not") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "$selector"
        }
    }

    open class PseudoElement internal constructor(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoElement) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }
        open fun argsStr(): String? = null
        override fun toString(): String = "::$name${argsStr()?.let { "($it)" } ?: ""}"

        companion object {
            @Deprecated(webCssSelectorsDeprecationMessage)
            val after = PseudoElement("after")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val before = PseudoElement("before")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val cue = PseudoElement("cue")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val cueRegion = PseudoElement("cue-region")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val firstLetter = PseudoElement("first-letter")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val firstLine = PseudoElement("first-line")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val fileSelectorButton = PseudoElement("file-selector-button")
            @Deprecated(webCssSelectorsDeprecationMessage)
            val selection = PseudoElement("selection")
        }

        class Slotted internal constructor(val selector: CSSSelector) : PseudoElement("slotted") {
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

internal const val webCssSelectorsDeprecationMessage = "Consider using a property from SelectorsScope"
