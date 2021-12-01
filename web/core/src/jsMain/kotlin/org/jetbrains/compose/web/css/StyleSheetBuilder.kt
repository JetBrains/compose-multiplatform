package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.*

interface CSSRulesHolder {
    val cssRules: CSSRuleDeclarationList
    fun add(cssRule: CSSRuleDeclaration)
    fun add(selector: CSSSelector, style: StyleHolder) {
        add(CSSStyleRuleDeclaration(selector, style))
    }
}

interface GenericStyleSheetBuilder<TBuilder> : CSSRulesHolder, SelectorsScope {
    fun buildRules(
        rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
    ): CSSRuleDeclarationList

    fun style(selector: CSSSelector, cssRule: TBuilder.() -> Unit)

    operator fun CSSSelector.invoke(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    infix fun CSSSelector.style(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    operator fun String.invoke(cssRule: TBuilder.() -> Unit) {
        style(Raw(this), cssRule)
    }

    infix fun String.style(cssRule: TBuilder.() -> Unit) {
        style(Raw(this), cssRule)
    }
}

interface SelectorsScope {
    fun selector(selector: String): CSSSelector = Raw(selector)
    fun combine(vararg selectors: CSSSelector): CSSSelector = Combine(selectors.toMutableList())

    operator fun CSSSelector.plus(selector: CSSSelector): CSSSelector {
        if (this is Combine) {
            this.selectors.add(selector)
            return this
        }
        return if (selector is Combine) {
            selector.selectors.add(0, this)
            selector
        } else {
            combine(this, selector)
        }
    }

    operator fun CSSSelector.plus(selector: String): CSSSelector {
        if (this is Combine) {
            this.selectors.add(selector(selector))
            return this
        }

        return combine(this, selector(selector))
    }

    @JsName("returnUniversalSelector")
    @Deprecated("Use universal property", replaceWith = ReplaceWith("universal"))
    fun universal(): CSSSelector = Universal

    val universal: CSSSelector
        get() = Universal

    fun type(type: String): CSSSelector = Type(type)
    fun className(className: String): CSSSelector = CSSSelector.CSSClass(className)
    fun id(id: String): CSSSelector = Id(id)

    fun attr(
        name: String,
        value: String? = null,
        operator: CSSSelector.Attribute.Operator = CSSSelector.Attribute.Operator.Equals,
        caseSensitive: Boolean = true
    ): CSSSelector = AttributeInternal(name, value, operator, caseSensitive)

    fun attrEquals(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.Equals, caseSensitive)

    fun attrListContains(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.ListContains, caseSensitive)

    fun attrHyphened(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.Hyphened, caseSensitive)

    fun attrPrefixed(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.Prefixed, caseSensitive)

    fun attrSuffixed(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.Suffixed, caseSensitive)

    fun attrContains(name: String, value: String? = null, caseSensitive: Boolean = true) =
        attr(name, value, CSSSelector.Attribute.Operator.Contains, caseSensitive)

    fun group(vararg selectors: CSSSelector): CSSSelector = Group(selectors.toList())

    @Deprecated("Replaced with `desc`", ReplaceWith("desc(parent, selected)"))
    fun descendant(parent: CSSSelector, selected: CSSSelector): CSSSelector = desc(parent, selected)
    fun desc(parent: CSSSelector, selected: CSSSelector): CSSSelector = Descendant(parent, selected)
    fun desc(parent: CSSSelector, selected: String): CSSSelector = desc(parent, selector(selected))
    fun desc(parent: String, selected: CSSSelector): CSSSelector = desc(selector(parent), selected)
    fun desc(parent: String, selected: String): CSSSelector = desc(selector(parent), selector(selected))

    fun child(parent: CSSSelector, selected: CSSSelector): CSSSelector = Child(parent, selected)
    fun sibling(sibling: CSSSelector, selected: CSSSelector): CSSSelector = Sibling(sibling, selected)
    fun adjacent(sibling: CSSSelector, selected: CSSSelector): CSSSelector = Adjacent(sibling, selected)

    @JsName("returnHoverSelector")
    @Deprecated("Use hover property", replaceWith = ReplaceWith("hover"))
    fun hover(): CSSSelector = hover
    fun hover(selector: CSSSelector): CSSSelector = selector + hover

    // Location pseudo-classes
    val anyLink: CSSSelector
        get() = PseudoClassInternal("any-link")
    val link: CSSSelector
        get() = PseudoClassInternal("link")
    val visited: CSSSelector
        get() = PseudoClassInternal("visited")
    val localLink: CSSSelector
        get() = PseudoClassInternal("local-link")
    val target: CSSSelector
        get() = PseudoClassInternal("target")
    val targetWithin: CSSSelector
        get() = PseudoClassInternal("target-within")
    val scope: CSSSelector
        get() = PseudoClassInternal("scope")

    // User action pseudo-classes
    val hover: CSSSelector
        get() = PseudoClassInternal("hover")
    val active: CSSSelector
        get() = PseudoClassInternal("active")
    val focus: CSSSelector
        get() = PseudoClassInternal("focus")
    val focusVisible: CSSSelector
        get() = PseudoClassInternal("focus-visible")

    // Resource state pseudo-classes
    val playing: CSSSelector
        get() = PseudoClassInternal("playing")
    val paused: CSSSelector
        get() = PseudoClassInternal("paused")

    // The input pseudo-classes
    val autofill: CSSSelector
        get() = PseudoClassInternal("autofill")
    val enabled: CSSSelector
        get() = PseudoClassInternal("enabled")
    val disabled: CSSSelector
        get() = PseudoClassInternal("disabled")
    val readOnly: CSSSelector
        get() = PseudoClassInternal("read-only")
    val readWrite: CSSSelector
        get() = PseudoClassInternal("read-write")
    val placeholderShown: CSSSelector
        get() = PseudoClassInternal("placeholder-shown")
    val default: CSSSelector
        get() = PseudoClassInternal("default")
    val checked: CSSSelector
        get() = PseudoClassInternal("checked")
    val indeterminate: CSSSelector
        get() = PseudoClassInternal("indeterminate")
    val blank: CSSSelector
        get() = PseudoClassInternal("blank")
    val valid: CSSSelector
        get() = PseudoClassInternal("valid")
    val invalid: CSSSelector
        get() = PseudoClassInternal("invalid")
    val inRange: CSSSelector
        get() = PseudoClassInternal("in-range")
    val outOfRange: CSSSelector
        get() = PseudoClassInternal("out-of-range")
    val required: CSSSelector
        get() = PseudoClassInternal("required")
    val optional: CSSSelector
        get() = PseudoClassInternal("optional")
    val userInvalid: CSSSelector
        get() = PseudoClassInternal("user-invalid")

    // Tree-structural pseudo-classes
    val root: CSSSelector
        get() = PseudoClassInternal("root")
    val empty: CSSSelector
        get() = PseudoClassInternal("empty")
    val first: CSSSelector
        get() = PseudoClassInternal("first")
    val firstChild: CSSSelector
        get() = PseudoClassInternal("first-child")
    val lastChild: CSSSelector
        get() = PseudoClassInternal("last-child")
    val onlyChild: CSSSelector
        get() = PseudoClassInternal("only-child")
    val firstOfType: CSSSelector
        get() = PseudoClassInternal("first-of-type")
    val lastOfType: CSSSelector
        get() = PseudoClassInternal("last-of-type")
    val onlyOfType: CSSSelector
        get() = PseudoClassInternal("only-of-type")
    val host: CSSSelector
        get() = PseudoClassInternal("host")

    // Etc
    val defined: CSSSelector
        get() = PseudoClassInternal("defined")
    val left: CSSSelector
        get() = PseudoClassInternal("left")
    val right: CSSSelector
        get() = PseudoClassInternal("right")

    fun lang(langCode: LanguageCode): CSSSelector = PseudoClassInternal.Lang(langCode)
    fun nthChild(nth: Nth): CSSSelector = PseudoClassInternal.NthChild(nth)
    fun nthLastChild(nth: Nth): CSSSelector = PseudoClassInternal.NthLastChild(nth)
    fun nthOfType(nth: Nth): CSSSelector = PseudoClassInternal.NthOfType(nth)
    fun nthLastOfType(nth: Nth): CSSSelector = PseudoClassInternal.NthLastOfType(nth)
    fun host(selector: CSSSelector): CSSSelector = PseudoClassInternal.Host(selector)
    fun not(selector: CSSSelector): CSSSelector = PseudoClassInternal.Not(selector)

    // Pseudo Element
    val after: CSSSelector
        get() = PseudoElementInternal("after")
    val before: CSSSelector
        get() = PseudoElementInternal("before")
    val cue: CSSSelector
        get() = PseudoElementInternal("cue")
    val cueRegion: CSSSelector
        get() = PseudoElementInternal("cue-region")
    val firstLetter: CSSSelector
        get() = PseudoElementInternal("first-letter")
    val firstLine: CSSSelector
        get() = PseudoElementInternal("first-line")
    val fileSelectorButton: CSSSelector
        get() = PseudoElementInternal("file-selector-button")
    val selection: CSSSelector
        get() = PseudoElementInternal("selection")

    fun slotted(selector: CSSSelector): CSSSelector = PseudoElementInternal.Slotted(selector)
}

private data class Id(val id: String) : CSSSelector() {
    override fun toString(): String = "#$id"
}

private data class Type(val type: String) : CSSSelector() {
    override fun toString(): String = type
}

private object Universal : CSSSelector() {
    override fun toString(): String = "*"
}

private data class Raw(val selector: String) : CSSSelector() {
    override fun toString(): String = selector
}

private data class Combine(val selectors: MutableList<CSSSelector>) : CSSSelector() {
    override fun contains(other: CSSSelector, strict: Boolean): Boolean =
        contains(this, other, selectors, strict)

    override fun toString(): String = selectors.joinToString("")
    override fun asString(): String = selectors.joinToString("") { it.asString() }
}

private data class Group(val selectors: List<CSSSelector>) : CSSSelector() {
    override fun contains(other: CSSSelector, strict: Boolean): Boolean =
        contains(this, other, selectors, strict)

    override fun toString(): String = selectors.joinToString(", ")
    override fun asString(): String = selectors.joinToString(", ") { it.asString() }
}

interface StyleSheetBuilder : CSSRulesHolder, GenericStyleSheetBuilder<CSSStyleRuleBuilder> {
    override fun style(selector: CSSSelector, cssRule: CSSStyleRuleBuilder.() -> Unit) {
        add(selector, buildCSSStyleRule(cssRule))
    }
}

open class StyleSheetBuilderImpl : StyleSheetBuilder {
    override val cssRules: MutableCSSRuleDeclarationList = mutableListOf()

    override fun add(cssRule: CSSRuleDeclaration) {
        cssRules.add(cssRule)
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSStyleRuleBuilder>.() -> Unit) =
        StyleSheetBuilderImpl().apply(rulesBuild).cssRules
}
