package org.jetbrains.compose.web.css

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import org.jetbrains.compose.web.ExperimentalComposeWebStyleApi
import org.jetbrains.compose.web.css.selectors.CSSSelector
import org.jetbrains.compose.web.dom.Style
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CSSRulesHolderState : CSSRulesHolder {
    override val cssRules = mutableStateListOf<CSSRuleDeclaration>()


    override fun add(cssRule: CSSRuleDeclaration) {
        cssRules.add(cssRule)
    }
}

/**
 * Represents a collection of the css style rules.
 * StyleSheet needs to be mounted.
 *
 * @param customPrefix Will be used as prefix with current style. Pass `null` to use default value (classname of realization)
 *
 * @see [Style]
 *
 * Example:
 * ```
 * object AppStylesheet : StyleSheet() {
 *     val containerClass by style {
 *         padding(24.px)
 *     }
 * }
 * ```
 *
 * Usage Example:
 * ```
 *    Style(AppStylesheet) // this mounts the stylesheet
 *    Div(classes = listOf(AppStylesheet.containerClass),...)
 * ```
 */
open class StyleSheet(
    customPrefix: String?,
    private val rulesHolder: CSSRulesHolder = CSSRulesHolderState(),
) : StyleSheetBuilder, CSSRulesHolder by rulesHolder {
    private val boundClasses = mutableMapOf<String, CSSRuleDeclarationList>()
    protected val prefix: String = customPrefix ?: "${this::class.simpleName}-"

    val usePrefix: Boolean = customPrefix == null
    constructor(
        rulesHolder: CSSRulesHolder = CSSRulesHolderState(),
        usePrefix: Boolean = true
    ) : this(
        if (usePrefix) null else "",
        rulesHolder
    )

    protected fun style(cssRule: CSSBuilder.() -> Unit) = CSSHolder(prefix, cssRule)

    /**
     * Example:
     * ```
     * object AppStyleSheet : StyleSheet() {
     *  val bounce by keyframes {
     *  from {
     *       property("transform", "translateX(50%)")
     *  }
     *
     *  to {
     *      property("transform", "translateX(-50%)")
     *  }
     * }
     *
     *   val myClass by style {
     *      animation(bounce) {
     *          duration(2.s)
     *          timingFunction(AnimationTimingFunction.EaseIn)
     *          direction(AnimationDirection.Alternate)
     *      }
     *    }
     *  }
     * ```
     */
    protected fun keyframes(cssKeyframes: CSSKeyframesBuilder.() -> Unit) = CSSKeyframesHolder(prefix, cssKeyframes)

    companion object {
        private var counter = 0
    }

    @Suppress("EqualsOrHashCode")
    internal class CSSSelfSelector(var selector: CSSSelector? = null) : CSSSelector() {
        override fun toString(): String =
            throw IllegalStateException("You can't concatenate `String + CSSSelector` which contains `self` or `root`. Use `selector(<your string>)` to convert `String` to `CSSSelector` for proper work. https://github.com/JetBrains/compose-jb/issues/1440")

        override fun asString(): String =
            selector?.asString() ?: throw IllegalStateException("You can't instantiate self")

        override fun equals(other: Any?): Boolean {
            return other is CSSSelfSelector
        }
    }

    protected class CSSHolder(private val prefix: String, private val cssBuilder: CSSBuilder.() -> Unit) {
        operator fun provideDelegate(
            sheet: StyleSheet,
            property: KProperty<*>
        ): ReadOnlyProperty<Any?, String> {
            val className = "$prefix${property.name}"
            val selector = object : CSSSelector() {
                override fun asString() = ".${className}"
            }
            val (properties, rules) = buildCSS(selector, selector, cssBuilder)
            sheet.add(selector, properties)
            rules.forEach { sheet.add(it) }

            return ReadOnlyProperty { _, _ -> className }
        }
    }

    /**
     * See [keyframes]
     */
    protected class CSSKeyframesHolder(
        private val prefix: String,
        private val keyframesBuilder: CSSKeyframesBuilder.() -> Unit
    ) {
        operator fun provideDelegate(
            sheet: StyleSheet,
            property: KProperty<*>
        ): ReadOnlyProperty<Any?, CSSNamedKeyframes> {
            val keyframesName = "$prefix${property.name}"
            val rule = buildKeyframes(keyframesName, keyframesBuilder)
            sheet.add(rule)

            return ReadOnlyProperty { _, _ ->
                rule
            }
        }
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSStyleRuleBuilder>.() -> Unit) =
        StyleSheet().apply(rulesBuild).cssRules
}

internal fun buildCSS(
    thisClass: CSSSelector,
    thisContext: CSSSelector,
    cssRule: CSSBuilder.() -> Unit
): Pair<StyleHolder, CSSRuleDeclarationList> {
    val styleSheet = StyleSheetBuilderImpl()
    // workaround because of problems with plus operator overloading
    val root = (thisClass as? StyleSheet.CSSSelfSelector) ?: StyleSheet.CSSSelfSelector(thisClass)
    // workaround because of problems with plus operator overloading
    val self = (thisContext as? StyleSheet.CSSSelfSelector) ?: StyleSheet.CSSSelfSelector(thisContext)

    val builder = CSSBuilderImpl(root, self, styleSheet)
    builder.cssRule()
    return builder to styleSheet.cssRules
}

@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun Style(
    styleSheet: CSSRulesHolder
) {
    Style(cssRules = styleSheet.cssRules)
}
