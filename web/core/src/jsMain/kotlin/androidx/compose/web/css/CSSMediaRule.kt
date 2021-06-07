package org.jetbrains.compose.web.css

interface CSSMediaQuery {
    interface Invertible : CSSMediaQuery
    interface Combinable : CSSMediaQuery
    interface Atomic : Invertible, Combinable
    data class Raw(val string: String) : Atomic {
        override fun toString() = string
    }
    data class MediaType(val type: Enum) : Atomic {
        enum class Enum {
            all, print, screen, speech
        }

        override fun toString() = type.name
    }

    data class MediaFeature(
        val name: String,
        val value: StylePropertyValue? = null
    ) : CSSMediaQuery, Atomic {
        override fun equals(other: Any?): Boolean {
            return if (other is MediaFeature) {
                name == other.name && value.toString() == other.value.toString()
            } else false
        }

        override fun toString() = "($name${ value?.let { ": $value)" } ?: "" }"
    }

    // looks like it doesn't work at least in chrome
    data class NotFeature(val query: MediaFeature) : CSSMediaQuery {
        override fun toString() = "(not $query)"
    }

    data class And(val mediaList: MutableList<Atomic>) : CSSMediaQuery, Invertible, Combinable {
        override fun toString() = mediaList.joinToString(" and ")
    }

    data class Not(val query: Invertible) : CSSMediaQuery {
        override fun toString() = "not $query"
    }

    data class Combine(val mediaList: MutableList<CSSMediaQuery>) : CSSMediaQuery {
        override fun toString() = mediaList.joinToString(", ")
    }

    data class Only(val type: MediaType, val query: Combinable) : CSSMediaQuery, Invertible {
        override fun toString() = "only $type and $query"
    }

    // looks like it doesn't work at least in chrome, maybe need fallback to Combine
    data class Or(val mediaList: List<CSSMediaQuery>) {
        override fun toString() = mediaList.joinToString(" or ")
    }
}

class CSSMediaRuleDeclaration(
    val query: CSSMediaQuery,
    rules: CSSRuleDeclarationList
) : CSSGroupingRuleDeclaration(rules) {
    override val header: String
        get() = "@media $query"

    override fun equals(other: Any?): Boolean {
        return if (other is CSSMediaRuleDeclaration) {
            rules == other.rules && query == other.query
        } else false
    }
}

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.media(
    query: CSSMediaQuery,
    rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
) {
    val rules = buildRules(rulesBuild)
    add(CSSMediaRuleDeclaration(query, rules))
}

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.media(
    query: String,
    rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
) {
    media(CSSMediaQuery.Raw(query), rulesBuild)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.media(
    name: String,
    value: StylePropertyValue? = null,
    noinline rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
) {
    media(feature(name, value), rulesBuild)
}

fun feature(
    name: String,
    value: StylePropertyValue? = null
) = CSSMediaQuery.MediaFeature(name, value)

@Suppress("NOTHING_TO_INLINE")
inline fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.media(
    vararg mediaList: CSSMediaQuery,
    noinline rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
) {
    media(combine(*mediaList), rulesBuild)
}

fun combine(
    vararg mediaList: CSSMediaQuery
) = CSSMediaQuery.Combine(mediaList.toMutableList())

infix fun CSSMediaQuery.Atomic.and(
    query: CSSMediaQuery.Atomic
) = CSSMediaQuery.And(mutableListOf(this, query))

infix fun CSSMediaQuery.And.and(
    query: CSSMediaQuery.Atomic
) = this.apply {
    this.mediaList.add(query)
}

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.not(
    query: CSSMediaQuery.Invertible
) = CSSMediaQuery.Not(query)

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.minWidth(value: CSSSizeValue) =
    CSSMediaQuery.MediaFeature("min-width", value.asStylePropertyValue())

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.maxWidth(value: CSSSizeValue) =
    CSSMediaQuery.MediaFeature("max-width", value.asStylePropertyValue())

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.minHeight(value: CSSSizeValue) =
    CSSMediaQuery.MediaFeature("min-height", value.asStylePropertyValue())

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.maxHeight(value: CSSSizeValue) =
    CSSMediaQuery.MediaFeature("max-height", value.asStylePropertyValue())
