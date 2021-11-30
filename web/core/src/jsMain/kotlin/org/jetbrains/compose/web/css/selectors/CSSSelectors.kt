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

    internal data class Raw internal constructor(val selector: String) : CSSSelector() {
        override fun toString(): String = selector
    }

    internal object Universal : CSSSelector() {
        override fun toString(): String = "*"
    }

    internal data class Type internal constructor(val type: String) : CSSSelector() {
        override fun toString(): String = type
    }

    internal data class CSSClass internal constructor(val className: String) : CSSSelector() {
        override fun toString(): String = ".$className"
    }

    internal data class Id internal constructor(val id: String) : CSSSelector() {
        override fun toString(): String = "#$id"
    }

    object Attribute {
        enum class Operator(val value: String) {
            Equals("="),
            ListContains("~="),
            Hyphened("|="),
            Prefixed("^="),
            Suffixed("$="),
            Contains("*=")
        }
    }

    internal data class AttributeInternal internal constructor(
        val name: String,
        val value: String? = null,
        val operator: Attribute.Operator = Attribute.Operator.Equals,
        val caseSensitive: Boolean = true
    ) : CSSSelector() {

        override fun toString(): String {
            val valueStr = value?.let {
                "${operator.value}$value${if (!caseSensitive) " i" else ""}"
            } ?: ""
            return "[$name$valueStr]"
        }
    }

    internal data class Combine internal constructor(val selectors: MutableList<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString("")
        override fun asString(): String = selectors.joinToString("") { it.asString() }
    }

    internal data class Group internal constructor(val selectors: List<CSSSelector>) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, selectors, strict)

        override fun toString(): String = selectors.joinToString(", ")
        override fun asString(): String = selectors.joinToString(", ") { it.asString() }
    }

    internal data class Descendant internal constructor(val parent: CSSSelector, val selected: CSSSelector) :
        CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent $selected"
        override fun asString(): String = "${parent.asString()} ${selected.asString()}"
    }

    internal data class Child internal constructor(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(parent, selected), strict)

        override fun toString(): String = "$parent > $selected"
        override fun asString(): String = "${parent.asString()} > ${selected.asString()}"
    }

    internal data class Sibling internal constructor(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev ~ $selected"
        override fun asString(): String = "${prev.asString()} ~ ${selected.asString()}"
    }

    internal data class Adjacent internal constructor(val prev: CSSSelector, val selected: CSSSelector) :
        CSSSelector() {
        override fun contains(other: CSSSelector, strict: Boolean): Boolean =
            contains(this, other, listOf(prev, selected), strict)

        override fun toString(): String = "$prev + $selected"
        override fun asString(): String = "${prev.asString()} + ${selected.asString()}"
    }

    object PseudoClass {
        // Location pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val anyLink : CSSSelector = PseudoClassInternal("any-link")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val link : CSSSelector = PseudoClassInternal("link")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val visited : CSSSelector = PseudoClassInternal("visited")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val localLink : CSSSelector = PseudoClassInternal("local-link")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val target : CSSSelector = PseudoClassInternal("target")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val targetWithin : CSSSelector = PseudoClassInternal("target-within")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val scope : CSSSelector = PseudoClassInternal("scope")

        // User action pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val hover : CSSSelector = PseudoClassInternal("hover")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val active : CSSSelector = PseudoClassInternal("active")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val focus : CSSSelector = PseudoClassInternal("focus")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val focusVisible : CSSSelector = PseudoClassInternal("focus-visible")

        // Resource state pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val playing : CSSSelector = PseudoClassInternal("playing")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val paused : CSSSelector = PseudoClassInternal("paused")

        // The input pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val autofill : CSSSelector = PseudoClassInternal("autofill")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val enabled : CSSSelector = PseudoClassInternal("enabled")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val disabled : CSSSelector = PseudoClassInternal("disabled")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val readOnly : CSSSelector = PseudoClassInternal("read-only")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val readWrite : CSSSelector = PseudoClassInternal("read-write")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val placeholderShown : CSSSelector = PseudoClassInternal("placeholder-shown")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val default : CSSSelector = PseudoClassInternal("default")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val checked : CSSSelector = PseudoClassInternal("checked")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val indeterminate : CSSSelector = PseudoClassInternal("indeterminate")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val blank : CSSSelector = PseudoClassInternal("blank")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val valid : CSSSelector = PseudoClassInternal("valid")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val invalid : CSSSelector = PseudoClassInternal("invalid")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val inRange : CSSSelector = PseudoClassInternal("in-range")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val outOfRange : CSSSelector = PseudoClassInternal("out-of-range")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val required : CSSSelector = PseudoClassInternal("required")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val optional : CSSSelector = PseudoClassInternal("optional")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val userInvalid : CSSSelector = PseudoClassInternal("user-invalid")

        // Tree-structural pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val root : CSSSelector = PseudoClassInternal("root")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val empty : CSSSelector = PseudoClassInternal("empty")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val first : CSSSelector = PseudoClassInternal("first")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstChild : CSSSelector = PseudoClassInternal("first-child")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val lastChild : CSSSelector = PseudoClassInternal("last-child")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val onlyChild : CSSSelector = PseudoClassInternal("only-child")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstOfType : CSSSelector = PseudoClassInternal("first-of-type")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val lastOfType : CSSSelector = PseudoClassInternal("last-of-type")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val onlyOfType : CSSSelector = PseudoClassInternal("only-of-type")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val host : CSSSelector = PseudoClassInternal("host")

        // Etc
        @Deprecated(webCssSelectorsDeprecationMessage)
        val defined : CSSSelector = PseudoClassInternal("defined")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val left : CSSSelector = PseudoClassInternal("left")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val right : CSSSelector = PseudoClassInternal("right")
    }

    internal open class PseudoClassInternal internal constructor(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoClassInternal) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }
        open fun argsStr(): String? = null
        override fun toString(): String = ":$name${argsStr()?.let { "($it)" } ?: ""}"

        // Linguistic pseudo-classes
        internal class Lang internal constructor(val langCode: LanguageCode) : PseudoClassInternal("lang") {
            override fun argsStr() = langCode
        }

        // Tree-structural pseudo-classes
        internal class NthChild internal constructor(val nth: Nth) : PseudoClassInternal("nth-child") {
            override fun argsStr() = "$nth"
        }

        internal class NthLastChild internal constructor(val nth: Nth) : PseudoClassInternal("nth-last-child") {
            override fun argsStr() = "$nth"
        }

        internal class NthOfType internal constructor(val nth: Nth) : PseudoClassInternal("nth-of-type") {
            override fun argsStr() = "$nth"
        }

        internal class NthLastOfType internal constructor(val nth: Nth) : PseudoClassInternal("nth-last-of-type") {
            override fun argsStr() = "$nth"
        }

        internal class Host internal constructor(val selector: CSSSelector) : PseudoClassInternal("host") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "${selector.asString()}"
        }

        // Etc
        internal class Not internal constructor(val selector: CSSSelector) : PseudoClassInternal("not") {
            override fun contains(other: CSSSelector, strict: Boolean): Boolean =
                contains(this, other, listOf(selector), strict)

            override fun argsStr() = "$selector"
        }
    }

    object PseudoElement {
        @Deprecated(webCssSelectorsDeprecationMessage)
        val after : CSSSelector = PseudoElementInternal("after")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val before : CSSSelector = PseudoElementInternal("before")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val cue : CSSSelector = PseudoElementInternal("cue")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val cueRegion : CSSSelector = PseudoElementInternal("cue-region")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstLetter : CSSSelector = PseudoElementInternal("first-letter")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstLine : CSSSelector = PseudoElementInternal("first-line")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val fileSelectorButton : CSSSelector = PseudoElementInternal("file-selector-button")
        @Deprecated(webCssSelectorsDeprecationMessage)
        val selection : CSSSelector = PseudoElementInternal("selection")
    }

    internal open class PseudoElementInternal internal constructor(val name: String) : CSSSelector() {
        override fun equals(other: Any?): Boolean {
            return if (other is PseudoElementInternal) {
                name == other.name && argsStr() == other.argsStr()
            } else false
        }

        open fun argsStr(): String? = null
        override fun toString(): String = "::$name${argsStr()?.let { "($it)" } ?: ""}"

        internal class Slotted internal constructor(val selector: CSSSelector) : PseudoElementInternal("slotted") {
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
