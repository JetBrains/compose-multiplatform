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
    ): CSSSelector = CSSSelector.Attribute(name, value, operator, caseSensitive)

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
        get() = CSSSelector.PseudoClass("any-link")
    val link: CSSSelector
        get() = CSSSelector.PseudoClass("link")
    val visited: CSSSelector
        get() = CSSSelector.PseudoClass("visited")
    val localLink: CSSSelector
        get() = CSSSelector.PseudoClass("local-link")
    val target: CSSSelector
        get() = CSSSelector.PseudoClass("target")
    val targetWithin: CSSSelector
        get() = CSSSelector.PseudoClass("target-within")
    val scope: CSSSelector
        get() = CSSSelector.PseudoClass("scope")

    // User action pseudo-classes
    val hover: CSSSelector
        get() = CSSSelector.PseudoClass("hover")
    val active: CSSSelector
        get() = CSSSelector.PseudoClass("active")
    val focus: CSSSelector
        get() = CSSSelector.PseudoClass("focus")
    val focusVisible: CSSSelector
        get() = CSSSelector.PseudoClass("focus-visible")

    // Resource state pseudo-classes
    val playing: CSSSelector
        get() = CSSSelector.PseudoClass("playing")
    val paused: CSSSelector
        get() = CSSSelector.PseudoClass("paused")

    // The input pseudo-classes
    val autofill: CSSSelector
        get() = CSSSelector.PseudoClass("autofill")
    val enabled: CSSSelector
        get() = CSSSelector.PseudoClass("enabled")
    val disabled: CSSSelector
        get() = CSSSelector.PseudoClass("disabled")
    val readOnly: CSSSelector
        get() = CSSSelector.PseudoClass("read-only")
    val readWrite: CSSSelector
        get() = CSSSelector.PseudoClass("read-write")
    val placeholderShown: CSSSelector
        get() = CSSSelector.PseudoClass("placeholder-shown")
    val default: CSSSelector
        get() = CSSSelector.PseudoClass("default")
    val checked: CSSSelector
        get() = CSSSelector.PseudoClass("checked")
    val indeterminate: CSSSelector
        get() = CSSSelector.PseudoClass("indeterminate")
    val blank: CSSSelector
        get() = CSSSelector.PseudoClass("blank")
    val valid: CSSSelector
        get() = CSSSelector.PseudoClass("valid")
    val invalid: CSSSelector
        get() = CSSSelector.PseudoClass("invalid")
    val inRange: CSSSelector
        get() = CSSSelector.PseudoClass("in-range")
    val outOfRange: CSSSelector
        get() = CSSSelector.PseudoClass("out-of-range")
    val required: CSSSelector
        get() = CSSSelector.PseudoClass("required")
    val optional: CSSSelector
        get() = CSSSelector.PseudoClass("optional")
    val userInvalid: CSSSelector
        get() = CSSSelector.PseudoClass("user-invalid")

    // Tree-structural pseudo-classes
    val root: CSSSelector
        get() = CSSSelector.PseudoClass("root")
    val empty: CSSSelector
        get() = CSSSelector.PseudoClass("empty")
    val first: CSSSelector
        get() = CSSSelector.PseudoClass("first")
    val firstChild: CSSSelector
        get() = CSSSelector.PseudoClass("first-child")
    val lastChild: CSSSelector
        get() = CSSSelector.PseudoClass("last-child")
    val onlyChild: CSSSelector
        get() = CSSSelector.PseudoClass("only-child")
    val firstOfType: CSSSelector
        get() = CSSSelector.PseudoClass("first-of-type")
    val lastOfType: CSSSelector
        get() = CSSSelector.PseudoClass("last-of-type")
    val onlyOfType: CSSSelector
        get() = CSSSelector.PseudoClass("only-of-type")
    val host: CSSSelector
        get() = CSSSelector.PseudoClass("host")

    // Etc
    val defined: CSSSelector
        get() = CSSSelector.PseudoClass("defined")
    val left: CSSSelector
        get() = CSSSelector.PseudoClass("left")
    val right: CSSSelector
        get() = CSSSelector.PseudoClass("right")

    fun lang(langCode: LanguageCode): CSSSelector = CSSSelector.PseudoClass.Lang(langCode)
    fun nthChild(nth: Nth): CSSSelector = CSSSelector.PseudoClass.NthChild(nth)
    fun nthLastChild(nth: Nth): CSSSelector = CSSSelector.PseudoClass.NthLastChild(nth)
    fun nthOfType(nth: Nth): CSSSelector = CSSSelector.PseudoClass.NthOfType(nth)
    fun nthLastOfType(nth: Nth): CSSSelector = CSSSelector.PseudoClass.NthLastOfType(nth)
    fun host(selector: CSSSelector): CSSSelector = CSSSelector.PseudoClass.Host(selector)
    fun not(selector: CSSSelector): CSSSelector = CSSSelector.PseudoClass.Not(selector)

    // Pseudo Element
    val after: CSSSelector
        get() = CSSSelector.PseudoElement("after")
    val before: CSSSelector
        get() = CSSSelector.PseudoElement("before")
    val cue: CSSSelector
        get() = CSSSelector.PseudoElement("cue")
    val cueRegion: CSSSelector
        get() = CSSSelector.PseudoElement("cue-region")
    val firstLetter: CSSSelector
        get() = CSSSelector.PseudoElement("first-letter")
    val firstLine: CSSSelector
        get() = CSSSelector.PseudoElement("first-line")
    val fileSelectorButton: CSSSelector
        get() = CSSSelector.PseudoElement("file-selector-button")
    val selection: CSSSelector
        get() = CSSSelector.PseudoElement("selection")

    fun slotted(selector: CSSSelector): CSSSelector = CSSSelector.PseudoElement.Slotted(selector)
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
