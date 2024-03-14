package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.*
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import java.nio.file.Path
import java.util.*
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
private val experimentalAnnotation = AnnotationSpec.builder(
    ClassName("org.jetbrains.compose.resources", "ExperimentalResourceApi")
).build()

private fun CodeBlock.Builder.addQualifiers(resourceItem: ResourceItem): CodeBlock.Builder {
    val languageQualifier = ClassName("org.jetbrains.compose.resources", "LanguageQualifier")
    val regionQualifier = ClassName("org.jetbrains.compose.resources", "RegionQualifier")
    val themeQualifier = ClassName("org.jetbrains.compose.resources", "ThemeQualifier")
    val densityQualifier = ClassName("org.jetbrains.compose.resources", "DensityQualifier")

    val languageRegex = Regex("[a-z]{2,3}")
    val regionRegex = Regex("r[A-Z]{2}")

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

// We need to divide accessors by different files because
//
// if all accessors are generated in a single object
// then a build may fail with: org.jetbrains.org.objectweb.asm.MethodTooLargeException: Method too large: Res$drawable.<clinit> ()V
// e.g. https://github.com/JetBrains/compose-multiplatform/issues/4285
//
// if accessor initializers are extracted from the single object but located in the same file
// then a build may fail with: org.jetbrains.org.objectweb.asm.ClassTooLargeException: Class too large: Res$drawable
private const val ITEMS_PER_FILE_LIMIT = 500
internal fun getResFileSpecs(
    //type -> id -> items
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>,
    packageName: String,
    moduleDir: String,
    isPublic: Boolean
): List<FileSpec> {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    val files = mutableListOf<FileSpec>()
    val resClass = FileSpec.builder(packageName, "Res").also { file ->
        file.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
                .addMember("org.jetbrains.compose.resources.ExperimentalResourceApi::class")
                .build()
        )
        file.addType(TypeSpec.objectBuilder("Res").also { resObject ->
            resObject.addModifiers(resModifier)
            resObject.addAnnotation(experimentalAnnotation)

            //readFileBytes
            val readResourceBytes = MemberName("org.jetbrains.compose.resources", "readResourceBytes")
            resObject.addFunction(
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
                    .addStatement("""return %M("$moduleDir" + path)""", readResourceBytes)
                    .build()
            )
            ResourceType.values().forEach { type ->
                resObject.addType(TypeSpec.objectBuilder(type.typeName).build())
            }
        }.build())
    }.build()
    files.add(resClass)

    //we need to sort it to generate the same code on different platforms
    sortResources(resources).forEach { (type, idToResources) ->
        val chunks = idToResources.keys.chunked(ITEMS_PER_FILE_LIMIT)

        chunks.forEachIndexed { index, ids ->
            files.add(
                getChunkFileSpec(
                    type,
                    index,
                    packageName,
                    moduleDir,
                    resModifier,
                    idToResources.subMap(ids.first(), true, ids.last(), true)
                )
            )
        }
    }

    return files
}

private fun getChunkFileSpec(
    type: ResourceType,
    index: Int,
    packageName: String,
    moduleDir: String,
    resModifier: KModifier,
    idToResources: Map<String, List<ResourceItem>>
): FileSpec {
    val chunkClassName = type.typeName.uppercaseFirstChar() + index
    return FileSpec.builder(packageName, chunkClassName).also { chunkFile ->
        chunkFile.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
                .build()
        )

        val objectSpec = TypeSpec.objectBuilder(chunkClassName).also { typeObject ->
            typeObject.addModifiers(KModifier.PRIVATE)
            typeObject.addAnnotation(experimentalAnnotation)
            val properties = idToResources.keys.map { resName ->
                PropertySpec.builder(resName, type.getClassName())
                    .delegate("\nlazy·{ init_$resName() }")
                    .build()
            }
            typeObject.addProperties(properties)
        }.build()
        chunkFile.addType(objectSpec)

        idToResources.forEach { (resName, items) ->
            val accessor = PropertySpec.builder(resName, type.getClassName(), resModifier)
                .receiver(ClassName(packageName, "Res", type.typeName))
                .addAnnotation(experimentalAnnotation)
                .getter(FunSpec.getterBuilder().addStatement("return $chunkClassName.$resName").build())
                .build()
            chunkFile.addProperty(accessor)

            val initializer = FunSpec.builder("init_$resName")
                .addModifiers(KModifier.PRIVATE)
                .addAnnotation(experimentalAnnotation)
                .returns(type.getClassName())
                .addStatement(
                    CodeBlock.builder()
                        .add("return %T(\n", type.getClassName()).withIndent {
                            add("\"${type}:${resName}\",")
                            if (type == ResourceType.STRING) add(" \"$resName\",")
                            withIndent {
                                add("\nsetOf(\n").withIndent {
                                    items.forEach { item ->
                                        add("%T(", resourceItemClass)
                                        add("setOf(").addQualifiers(item).add("), ")
                                        //file separator should be '/' on all platforms
                                        add("\"$moduleDir${item.path.invariantSeparatorsPathString}\"")
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
            chunkFile.addFunction(initializer)
        }
    }.build()
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