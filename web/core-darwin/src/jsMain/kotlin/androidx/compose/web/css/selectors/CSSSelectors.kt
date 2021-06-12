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

open class CSSSelector {
    override fun equals(other: Any?): Boolean {
        return toString() == other.toString()
    }

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
        override fun toString(): String = selectors.joinToString("")
    }

    data class Group(val selectors: List<CSSSelector>) : CSSSelector() {
        override fun toString(): String = selectors.joinToString(", ")
    }

    data class Descendant(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun toString(): String = "$parent $selected"
    }

    data class Child(val parent: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun toString(): String = "$parent > $selected"
    }

    data class Sibling(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun toString(): String = "$prev ~ $selected"
    }

    data class Adjacent(val prev: CSSSelector, val selected: CSSSelector) : CSSSelector() {
        override fun toString(): String = "$prev + $selected"
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
            override fun argsStr() = "$selector"
        }

        // Etc
        class Not(val selector: CSSSelector) : PseudoClass("not") {
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
            override fun argsStr() = selector.toString()
        }
    }
}

fun selector(selector: String) = CSSSelector.Raw(selector)
fun combine(vararg selectors: CSSSelector) = CSSSelector.Combine(selectors.toMutableList())
operator fun CSSSelector.plus(selector: CSSSelector) = combine(this, selector)
operator fun String.plus(selector: CSSSelector) = combine(selector(this), selector)
operator fun CSSSelector.plus(selector: String) = combine(this, selector(selector))
operator fun CSSSelector.Combine.plus(selector: CSSSelector) { this.selectors.add(selector) }
operator fun CSSSelector.Combine.plus(selector: String) { this.selectors.add(selector(selector)) }

fun universal() = CSSSelector.Universal
fun type(type: String) = CSSSelector.Type(type)
fun className(className: String) = CSSSelector.CSSClass(className)
fun id(id: String) = CSSSelector.Id(id)
fun attr(
    name: String,
    value: String? = null,
    operator: CSSSelector.Attribute.Operator = CSSSelector.Attribute.Operator.Equals,
    caseSensitive: Boolean = true
) = CSSSelector.Attribute(name, value, operator, caseSensitive)
fun group(vararg selectors: CSSSelector) = CSSSelector.Group(selectors.toList())
fun descendant(parent: CSSSelector, selected: CSSSelector) =
    CSSSelector.Descendant(parent, selected)
fun child(parent: CSSSelector, selected: CSSSelector) =
    CSSSelector.Child(parent, selected)
fun sibling(sibling: CSSSelector, selected: CSSSelector) = CSSSelector.Descendant(sibling, selected)
fun adjacent(sibling: CSSSelector, selected: CSSSelector) = CSSSelector.Adjacent(sibling, selected)

fun not(selector: CSSSelector) = CSSSelector.PseudoClass.Not(selector)
fun hover() = CSSSelector.PseudoClass.hover
fun hover(selector: CSSSelector) = selector + hover()
