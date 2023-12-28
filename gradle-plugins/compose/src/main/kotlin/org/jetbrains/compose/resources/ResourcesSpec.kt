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
                ?: error("Unknown resource type: '$str'.")
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

    val qualifiersMap = mutableMapOf<ClassName, String>()

    fun saveQualifier(className: ClassName, qualifier: String) {
        qualifiersMap[className]?.let {
            error("${resourceItem.path} contains repetitive qualifiers: '$it' and '$qualifier'.")
        }
        qualifiersMap[className] = qualifier
    }

    resourceItem.qualifiers.forEach { q ->
        when (q) {
            "light",
            "dark" -> {
                saveQualifier(themeQualifier, q)
            }

            "mdpi",
            "hdpi",
            "xhdpi",
            "xxhdpi",
            "xxxhdpi",
            "ldpi" -> {
                saveQualifier(densityQualifier, q)
            }

            else -> when {
                q.matches(languageRegex) -> {
                    saveQualifier(languageQualifier, q)
                }

                q.matches(regionRegex) -> {
                    saveQualifier(regionQualifier, q)
                }

                else -> error("${resourceItem.path} contains unknown qualifier: '$q'.")
            }
        }
    }
    qualifiersMap[themeQualifier]?.let { q -> add("%T.${q.uppercase()}, ", themeQualifier) }
    qualifiersMap[densityQualifier]?.let { q -> add("%T.${q.uppercase()}, ", densityQualifier) }
    qualifiersMap[languageQualifier]?.let { q -> add("%T(\"$q\"), ", languageQualifier) }
    qualifiersMap[regionQualifier]?.let { q ->
        val lang = qualifiersMap[languageQualifier]
        if (lang == null) {
            error("Region qualifier must be used only with language.\nFile: ${resourceItem.path}")
        }
        val langAndRegion = "$lang-$q"
        if(!resourceItem.path.toString().contains("-$langAndRegion")) {
            error("Region qualifier must be declared after language: '$langAndRegion'.\nFile: ${resourceItem.path}")
        }
        add("%T(\"${q.takeLast(2)}\"), ", regionQualifier)
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

        //readFileBytes
        val readResourceBytes = MemberName("org.jetbrains.compose.resources", "readResourceBytes")
        addFunction(
            FunSpec.builder("readFileBytes")
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                        .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
                        .build()
                )
                .addParameter("path", String::class)
                .addModifiers(KModifier.SUSPEND)
                .returns(ByteArray::class)
                .addStatement("return %M(\"$COMPOSE_RESOURCES_DIR/\" + path)", readResourceBytes) //todo: add module ID here
                .build()
        )

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
                        add("\"$COMPOSE_RESOURCES_DIR/${item.path.invariantSeparatorsPathString}\"\n") //todo: add module ID here
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