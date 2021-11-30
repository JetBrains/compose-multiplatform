package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector
import org.jetbrains.compose.web.css.selectors.Nth

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
        style(CSSSelector.Raw(this), cssRule)
    }

    infix fun String.style(cssRule: TBuilder.() -> Unit) {
        style(CSSSelector.Raw(this), cssRule)
    }
}

interface SelectorsScope {
    fun selector(selector: String): CSSSelector = CSSSelector.Raw(selector)
    fun combine(vararg selectors: CSSSelector): CSSSelector = CSSSelector.Combine(selectors.toMutableList())

    operator fun CSSSelector.plus(selector: CSSSelector): CSSSelector {
        if (this is CSSSelector.Combine) {
            this.selectors.add(selector)
            return this
        }
        return if (selector is CSSSelector.Combine) {
            selector.selectors.add(0, this)
            selector
        } else {
            combine(this, selector)
        }
    }

    operator fun CSSSelector.plus(selector: String): CSSSelector {
        if (this is CSSSelector.Combine) {
            this.selectors.add(selector(selector))
            return this
        }

        return combine(this, selector(selector))
    }

    @JsName("returnUniversalSelector")
    @Deprecated("Use universal property", replaceWith = ReplaceWith("universal"))
    fun universal(): CSSSelector = CSSSelector.Universal

    val universal: CSSSelector
        get() = CSSSelector.Universal

    fun type(type: String): CSSSelector = CSSSelector.Type(type)
    fun className(className: String): CSSSelector = CSSSelector.CSSClass(className)
    fun id(id: String): CSSSelector = CSSSelector.Id(id)

    fun attr(
        name: String,
        value: String? = null,
        operator: CSSSelector.Attribute.Operator = CSSSelector.Attribute.Operator.Equals,
        caseSensitive: Boolean = true
    ): CSSSelector = CSSSelector.AttributeInternal(name, value, operator, caseSensitive)

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

    fun group(vararg selectors: CSSSelector): CSSSelector = CSSSelector.Group(selectors.toList())

    @Deprecated("Replaced with `desc`", ReplaceWith("desc(parent, selected)"))
    fun descendant(parent: CSSSelector, selected: CSSSelector): CSSSelector = desc(parent, selected)
    fun desc(parent: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Descendant(parent, selected)
    fun desc(parent: CSSSelector, selected: String): CSSSelector = desc(parent, selector(selected))
    fun desc(parent: String, selected: CSSSelector): CSSSelector = desc(selector(parent), selected)
    fun desc(parent: String, selected: String): CSSSelector = desc(selector(parent), selector(selected))

    fun child(parent: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Child(parent, selected)
    fun sibling(sibling: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Sibling(sibling, selected)
    fun adjacent(sibling: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Adjacent(sibling, selected)

    @JsName("returnHoverSelector")
    @Deprecated("Use hover property", replaceWith = ReplaceWith("hover"))
    fun hover(): CSSSelector = hover
    fun hover(selector: CSSSelector): CSSSelector = selector + hover

    // Location pseudo-classes
    val anyLink: CSSSelector
        get() = CSSSelector.PseudoClassInternal("any-link")
    val link: CSSSelector
        get() = CSSSelector.PseudoClassInternal("link")
    val visited: CSSSelector
        get() = CSSSelector.PseudoClassInternal("visited")
    val localLink: CSSSelector
        get() = CSSSelector.PseudoClassInternal("local-link")
    val target: CSSSelector
        get() = CSSSelector.PseudoClassInternal("target")
    val targetWithin: CSSSelector
        get() = CSSSelector.PseudoClassInternal("target-within")
    val scope: CSSSelector
        get() = CSSSelector.PseudoClassInternal("scope")

    // User action pseudo-classes
    val hover: CSSSelector
        get() = CSSSelector.PseudoClassInternal("hover")
    val active: CSSSelector
        get() = CSSSelector.PseudoClassInternal("active")
    val focus: CSSSelector
        get() = CSSSelector.PseudoClassInternal("focus")
    val focusVisible: CSSSelector
        get() = CSSSelector.PseudoClassInternal("focus-visible")

    // Resource state pseudo-classes
    val playing: CSSSelector
        get() = CSSSelector.PseudoClassInternal("playing")
    val paused: CSSSelector
        get() = CSSSelector.PseudoClassInternal("paused")

    // The input pseudo-classes
    val autofill: CSSSelector
        get() = CSSSelector.PseudoClassInternal("autofill")
    val enabled: CSSSelector
        get() = CSSSelector.PseudoClassInternal("enabled")
    val disabled: CSSSelector
        get() = CSSSelector.PseudoClassInternal("disabled")
    val readOnly: CSSSelector
        get() = CSSSelector.PseudoClassInternal("read-only")
    val readWrite: CSSSelector
        get() = CSSSelector.PseudoClassInternal("read-write")
    val placeholderShown: CSSSelector
        get() = CSSSelector.PseudoClassInternal("placeholder-shown")
    val default: CSSSelector
        get() = CSSSelector.PseudoClassInternal("default")
    val checked: CSSSelector
        get() = CSSSelector.PseudoClassInternal("checked")
    val indeterminate: CSSSelector
        get() = CSSSelector.PseudoClassInternal("indeterminate")
    val blank: CSSSelector
        get() = CSSSelector.PseudoClassInternal("blank")
    val valid: CSSSelector
        get() = CSSSelector.PseudoClassInternal("valid")
    val invalid: CSSSelector
        get() = CSSSelector.PseudoClassInternal("invalid")
    val inRange: CSSSelector
        get() = CSSSelector.PseudoClassInternal("in-range")
    val outOfRange: CSSSelector
        get() = CSSSelector.PseudoClassInternal("out-of-range")
    val required: CSSSelector
        get() = CSSSelector.PseudoClassInternal("required")
    val optional: CSSSelector
        get() = CSSSelector.PseudoClassInternal("optional")
    val userInvalid: CSSSelector
        get() = CSSSelector.PseudoClassInternal("user-invalid")

    // Tree-structural pseudo-classes
    val root: CSSSelector
        get() = CSSSelector.PseudoClassInternal("root")
    val empty: CSSSelector
        get() = CSSSelector.PseudoClassInternal("empty")
    val first: CSSSelector
        get() = CSSSelector.PseudoClassInternal("first")
    val firstChild: CSSSelector
        get() = CSSSelector.PseudoClassInternal("first-child")
    val lastChild: CSSSelector
        get() = CSSSelector.PseudoClassInternal("last-child")
    val onlyChild: CSSSelector
        get() = CSSSelector.PseudoClassInternal("only-child")
    val firstOfType: CSSSelector
        get() = CSSSelector.PseudoClassInternal("first-of-type")
    val lastOfType: CSSSelector
        get() = CSSSelector.PseudoClassInternal("last-of-type")
    val onlyOfType: CSSSelector
        get() = CSSSelector.PseudoClassInternal("only-of-type")
    val host: CSSSelector
        get() = CSSSelector.PseudoClassInternal("host")

    // Etc
    val defined: CSSSelector
        get() = CSSSelector.PseudoClassInternal("defined")
    val left: CSSSelector
        get() = CSSSelector.PseudoClassInternal("left")
    val right: CSSSelector
        get() = CSSSelector.PseudoClassInternal("right")

    fun lang(langCode: LanguageCode): CSSSelector = CSSSelector.PseudoClassInternal.Lang(langCode)
    fun nthChild(nth: Nth): CSSSelector = CSSSelector.PseudoClassInternal.NthChild(nth)
    fun nthLastChild(nth: Nth): CSSSelector = CSSSelector.PseudoClassInternal.NthLastChild(nth)
    fun nthOfType(nth: Nth): CSSSelector = CSSSelector.PseudoClassInternal.NthOfType(nth)
    fun nthLastOfType(nth: Nth): CSSSelector = CSSSelector.PseudoClassInternal.NthLastOfType(nth)
    fun host(selector: CSSSelector): CSSSelector = CSSSelector.PseudoClassInternal.Host(selector)
    fun not(selector: CSSSelector): CSSSelector = CSSSelector.PseudoClassInternal.Not(selector)

    // Pseudo Element
    val after: CSSSelector
        get() = CSSSelector.PseudoElementInternal("after")
    val before: CSSSelector
        get() = CSSSelector.PseudoElementInternal("before")
    val cue: CSSSelector
        get() = CSSSelector.PseudoElementInternal("cue")
    val cueRegion: CSSSelector
        get() = CSSSelector.PseudoElementInternal("cue-region")
    val firstLetter: CSSSelector
        get() = CSSSelector.PseudoElementInternal("first-letter")
    val firstLine: CSSSelector
        get() = CSSSelector.PseudoElementInternal("first-line")
    val fileSelectorButton: CSSSelector
        get() = CSSSelector.PseudoElementInternal("file-selector-button")
    val selection: CSSSelector
        get() = CSSSelector.PseudoElementInternal("selection")

    fun slotted(selector: CSSSelector): CSSSelector = CSSSelector.PseudoElementInternal.Slotted(selector)
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
