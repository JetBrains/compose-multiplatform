@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.compose.web.css

interface CSSNamedKeyframes {
    val name: String
}

data class CSSKeyframesRuleDeclaration(
    override val name: String,
    val keys: CSSKeyframeRuleDeclarationList
) : CSSRuleDeclaration, CSSNamedKeyframes {
    override val header: String
        get() = "@keyframes $name"
}

typealias CSSKeyframeRuleDeclarationList = List<CSSKeyframeRuleDeclaration>

abstract class CSSKeyframe {
    abstract override fun toString(): String

    object From: CSSKeyframe() {
        override fun toString(): String = "from"
    }

    object To: CSSKeyframe() {
        override fun toString(): String = "to"
    }

    data class Percentage(val value: CSSSizeValue<CSSUnit.percent>): CSSKeyframe() {
        override fun toString(): String = value.toString()
    }

    data class Combine(val values: List<CSSSizeValue<CSSUnit.percent>>): CSSKeyframe() {
        override fun toString(): String = values.joinToString(", ")
    }
}

data class CSSKeyframeRuleDeclaration(
    val keyframe: CSSKeyframe,
    override val style: StyleHolder
) : CSSRuleDeclaration, CSSStyledRuleDeclaration {
    override val header: String
        get() = keyframe.toString()
}

class CSSKeyframesBuilder() {
    constructor(init: CSSKeyframesBuilder.() -> Unit) : this() {
        init()
    }
    val frames: MutableList<CSSKeyframeRuleDeclaration> = mutableListOf()

    fun from(style: CSSStyleRuleBuilder.() -> Unit) {
        frames += CSSKeyframeRuleDeclaration(CSSKeyframe.From, buildCSSStyleRule(style))
    }

    fun to(style: CSSStyleRuleBuilder.() -> Unit) {
        frames += CSSKeyframeRuleDeclaration(CSSKeyframe.To, buildCSSStyleRule(style))
    }

    fun each(vararg keys: CSSSizeValue<CSSUnit.percent>, style: CSSStyleRuleBuilder.() -> Unit) {
        frames += CSSKeyframeRuleDeclaration(CSSKeyframe.Combine(keys.toList()), buildCSSStyleRule(style))
    }

    operator fun CSSSizeValue<CSSUnit.percent>.invoke(style: CSSStyleRuleBuilder.() -> Unit) {
        frames += CSSKeyframeRuleDeclaration(CSSKeyframe.Percentage(this), buildCSSStyleRule(style))
    }
}

internal fun buildKeyframes(name: String, builder: CSSKeyframesBuilder.() -> Unit): CSSKeyframesRuleDeclaration {
    val frames = CSSKeyframesBuilder(builder).frames
    return CSSKeyframesRuleDeclaration(name, frames)
}
