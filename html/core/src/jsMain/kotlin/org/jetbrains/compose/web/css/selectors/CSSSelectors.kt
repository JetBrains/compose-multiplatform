@file:Suppress("EqualsOrHashCode", "unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.compose.web.css.selectors

import org.jetbrains.compose.web.css.SelectorsScope

internal const val webCssSelectorsDeprecationMessage = "Consider using a property from SelectorsScope"
private val selectorScope = object : SelectorsScope {}

sealed interface Nth {
    private data class FunctionalImpl(val a: Int? = null, val b: Int? = null) : Nth {
        override fun toString(): String = when {
            a != null && b != null -> "${a}n+$b"
            a != null -> "${a}n"
            b != null -> "$b"
            else -> ""
        }
    }
    private object OddImpl : Nth {
        override fun toString(): String = "odd"
    }
    private object EvenImpl : Nth {
        override fun toString(): String = "even"
    }

    companion object {
        val Odd: Nth = OddImpl
        val Even: Nth = EvenImpl

        @Suppress("FunctionName") // we want it to look like old Functional class constructor
        fun Functional(a: Int? = null, b: Int? = null): Nth {
            return FunctionalImpl(a = a, b = b)
        }
    }
}

abstract class CSSSelector internal constructor() {

    internal open fun contains(other: CSSSelector): Boolean {
        return this === other
    }

    @Suppress("SuspiciousEqualsCombination")
    protected fun contains(that: CSSSelector, other: CSSSelector, children: List<CSSSelector>): Boolean {
        return (that === other) || children.any { it.contains(other) }
    }

    // This method made for workaround because of possible concatenation of `String + CSSSelector`,
    // so `toString` is called for such operator, but we are calling `asString` for instantiation.
    // `toString` is reloaded for CSSSelfSelector
    internal open fun asString(): String = toString()

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

    object PseudoClass {
        // Location pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val anyLink : CSSSelector = selectorScope.anyLink
        @Deprecated(webCssSelectorsDeprecationMessage)
        val link : CSSSelector = selectorScope.link
        @Deprecated(webCssSelectorsDeprecationMessage)
        val visited : CSSSelector = selectorScope.visited
        @Deprecated(webCssSelectorsDeprecationMessage)
        val localLink : CSSSelector = selectorScope.localLink
        @Deprecated(webCssSelectorsDeprecationMessage)
        val target : CSSSelector = selectorScope.target
        @Deprecated(webCssSelectorsDeprecationMessage)
        val targetWithin : CSSSelector = selectorScope.targetWithin
        @Deprecated(webCssSelectorsDeprecationMessage)
        val scope : CSSSelector = selectorScope.scope

        // User action pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val hover : CSSSelector = selectorScope.hover
        @Deprecated(webCssSelectorsDeprecationMessage)
        val active : CSSSelector = selectorScope.active
        @Deprecated(webCssSelectorsDeprecationMessage)
        val focus : CSSSelector = selectorScope.focus
        @Deprecated(webCssSelectorsDeprecationMessage)
        val focusVisible : CSSSelector = selectorScope.focusVisible

        // Resource state pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val playing : CSSSelector = selectorScope.playing
        @Deprecated(webCssSelectorsDeprecationMessage)
        val paused : CSSSelector = selectorScope.paused

        // The input pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val autofill : CSSSelector = selectorScope.autofill
        @Deprecated(webCssSelectorsDeprecationMessage)
        val enabled : CSSSelector = selectorScope.enabled
        @Deprecated(webCssSelectorsDeprecationMessage)
        val disabled : CSSSelector = selectorScope.disabled
        @Deprecated(webCssSelectorsDeprecationMessage)
        val readOnly : CSSSelector = selectorScope.readOnly
        @Deprecated(webCssSelectorsDeprecationMessage)
        val readWrite : CSSSelector = selectorScope.readWrite
        @Deprecated(webCssSelectorsDeprecationMessage)
        val placeholderShown : CSSSelector = selectorScope.placeholderShown
        @Deprecated(webCssSelectorsDeprecationMessage)
        val default : CSSSelector = selectorScope.default
        @Deprecated(webCssSelectorsDeprecationMessage)
        val checked : CSSSelector = selectorScope.checked
        @Deprecated(webCssSelectorsDeprecationMessage)
        val indeterminate : CSSSelector = selectorScope.indeterminate
        @Deprecated(webCssSelectorsDeprecationMessage)
        val blank : CSSSelector = selectorScope.blank
        @Deprecated(webCssSelectorsDeprecationMessage)
        val valid : CSSSelector = selectorScope.valid
        @Deprecated(webCssSelectorsDeprecationMessage)
        val invalid : CSSSelector = selectorScope.invalid
        @Deprecated(webCssSelectorsDeprecationMessage)
        val inRange : CSSSelector = selectorScope.invalid
        @Deprecated(webCssSelectorsDeprecationMessage)
        val outOfRange : CSSSelector = selectorScope.outOfRange
        @Deprecated(webCssSelectorsDeprecationMessage)
        val required : CSSSelector = selectorScope.required
        @Deprecated(webCssSelectorsDeprecationMessage)
        val optional : CSSSelector = selectorScope.optional
        @Deprecated(webCssSelectorsDeprecationMessage)
        val userInvalid : CSSSelector = selectorScope.userInvalid

        // Tree-structural pseudo-classes
        @Deprecated(webCssSelectorsDeprecationMessage)
        val root : CSSSelector = selectorScope.root
        @Deprecated(webCssSelectorsDeprecationMessage)
        val empty : CSSSelector = selectorScope.empty
        @Deprecated(webCssSelectorsDeprecationMessage)
        val first : CSSSelector = selectorScope.first
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstChild : CSSSelector = selectorScope.firstChild
        @Deprecated(webCssSelectorsDeprecationMessage)
        val lastChild : CSSSelector = selectorScope.lastChild
        @Deprecated(webCssSelectorsDeprecationMessage)
        val onlyChild : CSSSelector = selectorScope.onlyChild
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstOfType : CSSSelector = selectorScope.firstOfType
        @Deprecated(webCssSelectorsDeprecationMessage)
        val lastOfType : CSSSelector = selectorScope.lastOfType
        @Deprecated(webCssSelectorsDeprecationMessage)
        val onlyOfType : CSSSelector = selectorScope.onlyOfType
        @Deprecated(webCssSelectorsDeprecationMessage)
        val host : CSSSelector = selectorScope.host

        // Etc
        @Deprecated(webCssSelectorsDeprecationMessage)
        val defined : CSSSelector = selectorScope.defined
        @Deprecated(webCssSelectorsDeprecationMessage)
        val left : CSSSelector = selectorScope.left
        @Deprecated(webCssSelectorsDeprecationMessage)
        val right : CSSSelector = selectorScope.right
    }

    object PseudoElement {
        @Deprecated(webCssSelectorsDeprecationMessage)
        val after : CSSSelector = selectorScope.after
        @Deprecated(webCssSelectorsDeprecationMessage)
        val before : CSSSelector = selectorScope.before
        @Deprecated(webCssSelectorsDeprecationMessage)
        val cue : CSSSelector = selectorScope.cue
        @Deprecated(webCssSelectorsDeprecationMessage)
        val cueRegion : CSSSelector = selectorScope.cueRegion
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstLetter : CSSSelector = selectorScope.firstLetter
        @Deprecated(webCssSelectorsDeprecationMessage)
        val firstLine : CSSSelector = selectorScope.firstLine
        @Deprecated(webCssSelectorsDeprecationMessage)
        val fileSelectorButton : CSSSelector = selectorScope.fileSelectorButton
        @Deprecated(webCssSelectorsDeprecationMessage)
        val selection : CSSSelector = selectorScope.selection
    }
}
