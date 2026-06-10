package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.withIndent
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import java.nio.file.Path
import java.util.*
import kotlin.io.path.invariantSeparatorsPathString

internal enum class ResourceType(val typeName: String, val accessorName: String) {
    DRAWABLE("drawable", "drawable"),
    STRING("string", "string"),
    STRING_ARRAY("string-array", "array"),
    PLURAL_STRING("plurals", "plurals"),
    FONT("font", "font");

    override fun toString(): String = typeName

    companion object {
        fun fromString(str: String): ResourceType? =
            ResourceType.values().firstOrNull { it.typeName.equals(str, true) }
    }
}

internal data class ResourceItem(
    val type: ResourceType,
    val qualifiers: List<String>,
    val name: String,
    val path: Path,
    val contentHash: Int,
    val offset: Long = -1,
    val size: Long = -1,
)

private fun ResourceType.getClassName(): ClassName = when (this) {
    ResourceType.DRAWABLE -> ClassName("org.jetbrains.compose.resources", "DrawableResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.STRING_ARRAY -> ClassName("org.jetbrains.compose.resources", "StringArrayResource")
    ResourceType.PLURAL_STRING -> ClassName("org.jetbrains.compose.resources", "PluralStringResource")
}

private fun ResourceType.requiresKeyName() =
    this in setOf(ResourceType.STRING, ResourceType.STRING_ARRAY, ResourceType.PLURAL_STRING)

private val resourceItemClass = ClassName("org.jetbrains.compose.resources", "ResourceItem")
private val internalAnnotationClass = ClassName("org.jetbrains.compose.resources", "InternalResourceApi")
private val internalAnnotation = AnnotationSpec.builder(internalAnnotationClass).build()

private val resourceContentHashAnnotationClass = ClassName("org.jetbrains.compose.resources", "ResourceContentHash")

private val languageRegex = Regex("[a-z]{2,3}") // ISO 639
private val scriptRegex = Regex("[A-Z][a-z]{3}") // ISO 15924
private val regionAlpha2Regex = Regex("[A-Z]{2}") // ISO 3166-1 alpha-2
private val regionNumericRegex = Regex("[0-9]{3}") // UN M.49
private val androidRegionRegex = Regex("r[A-Z]{2}|r[0-9]{3}") // Android `r` prefix

private fun CodeBlock.Builder.addQualifiers(resourceItem: ResourceItem): CodeBlock.Builder {
    val languageQualifier = ClassName("org.jetbrains.compose.resources", "LanguageQualifier")
    val scriptQualifier = ClassName("org.jetbrains.compose.resources", "ScriptQualifier")
    val regionQualifier = ClassName("org.jetbrains.compose.resources", "RegionQualifier")
    val themeQualifier = ClassName("org.jetbrains.compose.resources", "ThemeQualifier")
    val densityQualifier = ClassName("org.jetbrains.compose.resources", "DensityQualifier")

    val qualifiersMap = mutableMapOf<ClassName, String>()

    fun saveQualifier(className: ClassName, qualifier: String) {
        qualifiersMap[className]?.let {
            error("${resourceItem.path} contains repetitive qualifiers: '$it' and '$qualifier'.")
        }
        qualifiersMap[className] = qualifier
    }

    var bcpConsumed = false

    resourceItem.qualifiers.forEachIndexed { index, q ->
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
                q.startsWith("b+") -> {
                    if (index != 0) {
                        error("BCP 47 'b+' segment must be the first qualifier in '${resourceItem.path}'.")
                    }
                    val (language, script, region) = expandBcpQualifier(q, resourceItem.path)
                    language?.let { saveQualifier(languageQualifier, it) }
                    script?.let { saveQualifier(scriptQualifier, it) }
                    region?.let { saveQualifier(regionQualifier, it) }
                    bcpConsumed = true
                }

                q.matches(languageRegex) -> {
                    if (bcpConsumed) {
                        error("Locale qualifier '$q' cannot follow a BCP 47 segment in '${resourceItem.path}'.")
                    }
                    saveQualifier(languageQualifier, q)
                }

                q.matches(androidRegionRegex) -> {
                    if (bcpConsumed) {
                        error("Locale qualifier '$q' cannot follow a BCP 47 segment in '${resourceItem.path}'.")
                    }
                    saveQualifier(regionQualifier, q)
                }

                q.matches(scriptRegex) -> {
                    error("Script qualifier '$q' must be inside a BCP 47 segment in '${resourceItem.path}'.")
                }

                else -> error("${resourceItem.path} contains unknown qualifier: '$q'.")
            }
        }
    }

    qualifiersMap[themeQualifier]?.let { q -> add("%T.${q.uppercase()}, ", themeQualifier) }
    qualifiersMap[densityQualifier]?.let { q -> add("%T.${q.uppercase()}, ", densityQualifier) }
    qualifiersMap[languageQualifier]?.let { q -> add("%T(\"$q\"), ", languageQualifier) }

    val lang = qualifiersMap[languageQualifier]
    val languageIdx = lang?.let { resourceItem.qualifiers.indexOf(it) } ?: -1

    qualifiersMap[scriptQualifier]?.let { q ->
        if (lang == null) {
            error("Script qualifier must be used only with language.\nFile: ${resourceItem.path}")
        }
        val scriptIdx = resourceItem.qualifiers.indexOf(q)
        if (scriptIdx >= 0 && scriptIdx <= languageIdx) {
            error("Script qualifier must be declared after language: '$lang-$q'.\nFile: ${resourceItem.path}")
        }
        add("%T(\"$q\"), ", scriptQualifier)
    }

    qualifiersMap[regionQualifier]?.let { q ->
        if (lang == null) {
            error("Region qualifier must be used only with language.\nFile: ${resourceItem.path}")
        }
        val regionCode = q.removePrefix("r")
        val regionIdx = resourceItem.qualifiers.indexOf(q)
        if (regionIdx >= 0 && regionIdx <= languageIdx) {
            error("Region qualifier must be declared after language: '$lang-$q'.\nFile: ${resourceItem.path}")
        }
        add("%T(\"$regionCode\"), ", regionQualifier)
    }

    return this
}

private fun expandBcpQualifier(segment: String, path: Path): Triple<String?, String?, String?> {
    val subtags = segment.removePrefix("b+").split("+")
    var language: String? = null
    var script: String? = null
    var region: String? = null
    var lastKind = -1
    for ((index, subtag) in subtags.withIndex()) {
        val kind = when {
            subtag.matches(languageRegex)      -> 0
            subtag.matches(scriptRegex)        -> 1
            subtag.matches(regionAlpha2Regex)  -> 2
            subtag.matches(regionNumericRegex) -> 2
            else -> error("Malformed BCP 47 subtag '$subtag' in '$path'.")
        }
        if (index == 0 && kind != 0) {
            error("BCP 47 segment must start with a language subtag in '$path'.")
        }
        if (kind <= lastKind) {
            error("BCP 47 subtags must follow language -> script -> region order in '$path'.")
        }
        when (kind) {
            0 -> language = subtag
            1 -> script = subtag
            2 -> region = "r$subtag"
        }
        lastKind = kind
    }
    return Triple(language, script, region)
}

internal fun getResFileSpec(
    packageName: String,
    className: String,
    moduleDir: String,
    isPublic: Boolean
): FileSpec {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    return FileSpec.builder(packageName, className).also { file ->
        file.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", internalAnnotationClass)
                .build()
        )
        file.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                .addMember("%S", "RedundantVisibilityModifier")
                .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                .build()
        )
        file.addType(TypeSpec.objectBuilder(className).also { resObject ->
            resObject.addModifiers(resModifier)

            //readFileBytes
            val readResourceBytes = MemberName("org.jetbrains.compose.resources", "readResourceBytes")
            resObject.addFunction(
                FunSpec.builder("readBytes")
                    .addKdoc(
                        """
                    Reads the content of the resource file at the specified path and returns it as a byte array.
                    
                    Example: `val bytes = ${className}.readBytes("files/key.bin")`
                    
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

            //getUri
            val getResourceUri = MemberName("org.jetbrains.compose.resources", "getResourceUri")
            resObject.addFunction(
                FunSpec.builder("getUri")
                    .addKdoc(
                        """
                    Returns the URI string of the resource file at the specified path.
                    
                    Example: `val uri = ${className}.getUri("files/key.bin")`
                    
                    @param path The path of the file in the compose resource's directory.
                    @return The URI string of the file.
                """.trimIndent()
                    )
                    .addParameter("path", String::class)
                    .returns(String::class)
                    .addStatement("""return %M("$moduleDir" + path)""", getResourceUri)
                    .build()
            )

            ResourceType.values().forEach { type ->
                resObject.addType(TypeSpec.objectBuilder(type.accessorName).build())
            }
        }.build())
    }.build()
}

// We need to divide accessors by different files because
//
// if all accessors are generated in a single object
// then a build may fail with: org.jetbrains.org.objectweb.asm.MethodTooLargeException: Method too large: Res$drawable.<clinit> ()V
// e.g. https://github.com/JetBrains/compose-multiplatform/issues/4285
//
// if accessor initializers are extracted from the single object but located in the same file
// then a build may fail with: org.jetbrains.org.objectweb.asm.ClassTooLargeException: Class too large: Res$drawable
private const val ITEMS_PER_FILE_LIMIT = 100
internal fun getAccessorsSpecs(
    //type -> id -> items
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>,
    packageName: String,
    sourceSetName: String,
    moduleDir: String,
    resClassName: String,
    isPublic: Boolean,
    generateResourceContentHashAnnotation: Boolean
): List<FileSpec> {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    val files = mutableListOf<FileSpec>()

    //we need to sort it to generate the same code on different platforms
    sortResources(resources).forEach { (type, idToResources) ->
        val chunks = idToResources.keys.chunked(ITEMS_PER_FILE_LIMIT)

        chunks.forEachIndexed { index, ids ->
            files.add(
                getChunkFileSpec(
                    type,
                    "${type.accessorName.uppercaseFirstChar()}$index.$sourceSetName",
                    sourceSetName.uppercaseFirstChar() + type.accessorName.uppercaseFirstChar() + index,
                    packageName,
                    moduleDir,
                    resClassName,
                    resModifier,
                    idToResources.subMap(ids.first(), true, ids.last(), true),
                    generateResourceContentHashAnnotation
                )
            )
        }
    }

    return files
}

private fun getChunkFileSpec(
    type: ResourceType,
    fileName: String,
    chunkClassName: String,
    packageName: String,
    moduleDir: String,
    resClassName: String,
    resModifier: KModifier,
    idToResources: Map<String, List<ResourceItem>>,
    generateResourceContentHashAnnotation: Boolean
): FileSpec {
    return FileSpec.builder(packageName, fileName).also { chunkFile ->
        chunkFile.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", internalAnnotationClass)
                .build()
        )

        chunkFile.addProperty(
            PropertySpec.builder("MD", String::class)
                .addModifiers(KModifier.PRIVATE, KModifier.CONST)
                .initializer("%S", moduleDir)
                .build()
        )

        idToResources.forEach { (resName, items) ->
            val initializer = CodeBlock.builder()
                .beginControlFlow("lazy {")
                .apply {
                    if (type.requiresKeyName()) {
                        add("%T(%S, %S, setOf(\n", type.getClassName(), "$type:$resName", resName)
                    } else {
                        add("%T(%S, setOf(\n", type.getClassName(), "$type:$resName")
                    }
                    items.forEach { item ->
                        add("  %T(setOf(", resourceItemClass)
                        addQualifiers(item)
                        add("), ")
                        //file separator should be '/' on all platforms
                        add("\"${'$'}{MD}${item.path.invariantSeparatorsPathString}\", ${item.offset}, ${item.size}")
                        add("),\n")
                    }
                    add("))\n")
                }
                .endControlFlow()
                .build()

            val accessorBuilder = PropertySpec.builder(resName, type.getClassName(), resModifier)
                .receiver(ClassName(packageName, resClassName, type.accessorName))
                .delegate(initializer)
            if (generateResourceContentHashAnnotation) {
                accessorBuilder.addAnnotation(
                    AnnotationSpec.builder(resourceContentHashAnnotationClass)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE)
                        .addMember("%L", items.fold(0) { acc, item -> ((acc * 31) + item.contentHash) })
                        .build()
                )
            }
            chunkFile.addProperty(accessorBuilder.build())
        }

        //__collect${chunkClassName}Resources function
        chunkFile.addFunction(
            FunSpec.builder("_collect${chunkClassName}Resources")
                .addAnnotation(internalAnnotation)
                .addModifiers(KModifier.INTERNAL)
                .addParameter(
                    "map",
                    MUTABLE_MAP.parameterizedBy(String::class.asClassName(), type.getClassName())
                )
                .also { collectFun ->
                    idToResources.keys.forEach { resName ->
                        collectFun.addStatement("map.put(%S, %N.%N.%N)", resName, resClassName, type.accessorName, resName)
                    }
                }
                .build()
        )
    }.build()
}

internal fun getExpectResourceCollectorsFileSpec(
    packageName: String,
    fileName: String,
    resClassName: String,
    isPublic: Boolean
): FileSpec {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    return FileSpec.builder(packageName, fileName).also { file ->
        ResourceType.values().forEach { type ->
            val typeClassName = type.getClassName()
            file.addProperty(
                PropertySpec
                    .builder(
                        "all${typeClassName.simpleName}s",
                        MAP.parameterizedBy(String::class.asClassName(), typeClassName),
                        KModifier.EXPECT,
                        resModifier
                    )
                    .receiver(ClassName(packageName, resClassName))
                    .build()
            )
        }
    }.build()
}

internal fun getActualResourceCollectorsFileSpec(
    packageName: String,
    fileName: String,
    resClassName: String,
    isPublic: Boolean,
    useActualModifier: Boolean, //e.g. java only project doesn't need actual modifiers
    typeToCollectorFunctions: Map<ResourceType, List<String>>
): FileSpec = FileSpec.builder(packageName, fileName).also { file ->
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL

    file.addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
            .build()
    )

    ResourceType.values().forEach { type ->
        val typeClassName = type.getClassName()
        val initBlock = CodeBlock.builder()
            .addStatement("lazy {").withIndent {
                addStatement("val map = mutableMapOf<String, %T>()", typeClassName)
                typeToCollectorFunctions.get(type).orEmpty().forEach { item ->
                    addStatement("%N(map)", item)
                }
                addStatement("return@lazy map")
            }
            .addStatement("}")
            .build()

        val mods = if (useActualModifier) {
            listOf(KModifier.ACTUAL, resModifier)
        } else {
            listOf(resModifier)
        }

        val property = PropertySpec
            .builder(
                "all${typeClassName.simpleName}s",
                MAP.parameterizedBy(String::class.asClassName(), typeClassName),
                mods
            )
            .receiver(ClassName(packageName, resClassName))
            .delegate(initBlock)
            .build()
        file.addProperty(property)
    }
}.build()

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
