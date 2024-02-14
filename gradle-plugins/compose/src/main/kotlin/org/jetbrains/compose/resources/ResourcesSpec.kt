package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.*
import java.nio.file.Path
import java.util.SortedMap
import java.util.TreeMap
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

private fun ResourceType.getClassName(): ClassName = when (this) {
    ResourceType.DRAWABLE -> ClassName("org.jetbrains.compose.resources", "DrawableResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
}

private val resourceItemClass = ClassName("org.jetbrains.compose.resources", "ResourceItem")

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
        if (!resourceItem.path.toString().contains("-$langAndRegion")) {
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
): FileSpec =
    FileSpec.builder(packageName, "Res").apply {
        addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
                .addMember("org.jetbrains.compose.resources.ExperimentalResourceApi::class")
                .build()
        )

        //we need to sort it to generate the same code on different platforms
        val sortedResources = sortResources(resources)

        addType(TypeSpec.objectBuilder("Res").apply {
            addModifiers(KModifier.INTERNAL)
            addAnnotation(
                AnnotationSpec.builder(
                    ClassName("org.jetbrains.compose.resources", "ExperimentalResourceApi")
                ).build()
            )

            //readFileBytes
            val readResourceBytes = MemberName("org.jetbrains.compose.resources", "readResourceBytes")
            addFunction(
                FunSpec.builder("readBytes")
                    .addKdoc(
                        """
                    Reads the content of the resource file at the specified path and returns it as a byte array.
                    
                    Example: `val bytes = Res.readBytes("files/key.bin")`
                    
                    @param path The path of the file to read in the compose resource's directory.
                    @return The content of the file as a byte array.
                """.trimIndent()
                    )
                    .addParameter("path", String::class)
                    .addModifiers(KModifier.SUSPEND)
                    .returns(ByteArray::class)
                    .addStatement("return %M(path)", readResourceBytes) //todo: add module ID here
                    .build()
            )
            val types = sortedResources.map { (type, idToResources) ->
                getResourceTypeObject(type, idToResources)
            }
            addTypes(types)
        }.build())

        sortedResources
            .flatMap { (type, idToResources) ->
                idToResources.map { (name, items) ->
                    getResourceInitializer(name, type, items)
                }
            }
            .forEach { addFunction(it) }
    }.build()

private fun getterName(resourceType: ResourceType, resourceName: String): String =
    "get_${resourceType.typeName}_$resourceName"

private fun getResourceTypeObject(type: ResourceType, nameToResources: Map<String, List<ResourceItem>>) =
    TypeSpec.objectBuilder(type.typeName).apply {
        nameToResources.keys
            .forEach { name ->
                addProperty(
                    PropertySpec
                        .builder(name, type.getClassName())
                        .initializer(getterName(type, name) + "()")
                        .build()
                )
            }
    }.build()

private fun getResourceInitializer(name: String, type: ResourceType, items: List<ResourceItem>): FunSpec {
    val propertyTypeName = type.getClassName()
    val resourceId = "${type}:${name}"
    return FunSpec.builder(getterName(type, name))
        .addModifiers(KModifier.PRIVATE)
        .returns(propertyTypeName)
        .addStatement(
            CodeBlock.builder()
                .add("return %T(\n", propertyTypeName).withIndent {
                    add("\"$resourceId\",")
                    if (type == ResourceType.STRING) add(" \"$name\",")
                    withIndent {
                        add("\nsetOf(\n").withIndent {
                            items.forEach { item ->
                                add("%T(", resourceItemClass)
                                add("setOf(").addQualifiers(item).add("), ")
                                //file separator should be '/' on all platforms
                                add("\"${item.path.invariantSeparatorsPathString}\"") //todo: add module ID here
                                add("),\n")
                            }
                        }
                        add(")\n")
                    }
                }
                .add(")")
                .build().toString()
        )
        .build()
}

private fun sortResources(
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>
): TreeMap<ResourceType, TreeMap<String, List<ResourceItem>>> {
    val result = TreeMap<ResourceType, TreeMap<String, List<ResourceItem>>>()
    resources
        .entries
        .forEach { (type, items) ->
            val typeResult = TreeMap<String, List<ResourceItem>>()
            items
                .entries
                .forEach { (name, resItems) ->
                    typeResult[name] = resItems.sortedBy { it.path }
                }
            result[type] = typeResult
        }
    return result
}