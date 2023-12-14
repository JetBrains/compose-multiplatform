package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.*
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

internal enum class ResourceType(val typeName: String) {
    DRAWABLE("drawable"),
    STRING("string"),
    FONT("font");

    override fun toString(): String = typeName

    companion object {
        fun fromString(str: String): ResourceType =
            ResourceType.values()
                .firstOrNull { it.typeName.equals(str, true) }
                ?: error("Unknown resource type: $str")
    }
}

internal data class ResourceItem(
    val type: ResourceType,
    val qualifiers: List<String>,
    val name: String,
    val path: Path
)

private fun ResourceItem.getClassName(): ClassName = when (type) {
    ResourceType.DRAWABLE -> ClassName("org.jetbrains.compose.resources", "DrawableResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
}

private fun CodeBlock.Builder.addQualifiers(resourceItem: ResourceItem): CodeBlock.Builder {
    val languageQualifier = ClassName("org.jetbrains.compose.resources", "LanguageQualifier")
    val regionQualifier = ClassName("org.jetbrains.compose.resources", "RegionQualifier")
    val themeQualifier = ClassName("org.jetbrains.compose.resources", "ThemeQualifier")
    val densityQualifier = ClassName("org.jetbrains.compose.resources", "DensityQualifier")

    val languageRegex = Regex("[a-z][a-z]")
    val regionRegex = Regex("r[A-Z][A-Z]")

    var alreadyHasLanguage: String? = null
    var alreadyHasRegion: String? = null
    var alreadyHasTheme: String? = null
    var alreadyHasDensity: String? = null

    fun repetitiveQualifiers(first: String, second: String) {
        error("${resourceItem.path} contains repetitive qualifiers: $first and $second")
    }

    resourceItem.qualifiers.sorted().forEach { q ->
        when (q) {
            "light",
            "dark" -> {
                alreadyHasTheme?.let { repetitiveQualifiers(it, q) }
                alreadyHasTheme = q
                add("%T.${q.uppercase()}, ", themeQualifier)
            }

            "mdpi",
            "hdpi",
            "xhdpi",
            "xxhdpi",
            "xxxhdpi",
            "ldpi" -> {
                alreadyHasDensity?.let { repetitiveQualifiers(it, q) }
                alreadyHasDensity = q
                add("%T.${q.uppercase()}, ", densityQualifier)
            }

            else -> when {
                q.matches(languageRegex) -> {
                    alreadyHasLanguage?.let { repetitiveQualifiers(it, q) }
                    alreadyHasLanguage = q
                    add("%T(\"$q\"), ", languageQualifier)
                }

                q.matches(regionRegex) -> {
                    alreadyHasRegion?.let { repetitiveQualifiers(it, q) }
                    alreadyHasRegion = q
                    add("%T(\"${q.takeLast(2)}\"), ", regionQualifier)
                }

                else -> error("${resourceItem.path} contains unknown qualifier: $q")
            }
        }
    }
    return this
}

internal fun getResFileSpec(
    //type -> id -> items
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>,
    packageName: String
): FileSpec = FileSpec.builder(packageName, "Res").apply {
    addType(TypeSpec.objectBuilder("Res").apply {
        addModifiers(KModifier.INTERNAL)
        val types = resources.map { (type, idToResources) ->
            getResourceTypeObject(type, idToResources)
        }.sortedBy { it.name }
        addTypes(types)
    }.build())
}.build()

private fun getResourceTypeObject(type: ResourceType, nameToResources: Map<String, List<ResourceItem>>) =
    TypeSpec.objectBuilder(type.typeName).apply {
        nameToResources.entries
            .sortedBy { it.key }
            .forEach { (name, items) ->
                addResourceProperty(name, items.sortedBy { it.path })
            }
    }.build()

private fun TypeSpec.Builder.addResourceProperty(name: String, items: List<ResourceItem>) {
    val resourceItemClass = ClassName("org.jetbrains.compose.resources", "ResourceItem")

    val first = items.first()
    val propertyClassName = first.getClassName()
    val resourceId = first.let { "${it.type}:${it.name}" }

    val initializer = CodeBlock.builder()
        .add("%T(\n", propertyClassName).withIndent {
            add("\"$resourceId\",\n")
            if (first.type == ResourceType.STRING) {
                add("\"${first.name}\",\n")
            }
            add("setOf(\n").withIndent {
                items.forEach { item ->
                    add("%T(\n", resourceItemClass).withIndent {
                        add("setOf(").addQualifiers(item).add("),\n")
                        //file separator should be '/' on all platforms
                        add("\"${item.path.invariantSeparatorsPathString}\"\n")
                    }
                    add("),\n")
                }
            }
            add(")\n")
        }
        .add(")")
        .build()

    addProperty(
        PropertySpec.builder(name, propertyClassName)
            .initializer(initializer)
            .build()
    )
}