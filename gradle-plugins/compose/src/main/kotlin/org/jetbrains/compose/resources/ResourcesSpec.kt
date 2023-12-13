package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.withIndent
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.pathString

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
    val qualifiers: Set<String>,
    val name: String,
    val path: Path
)

private fun ResourceItem.getClassName(): ClassName = when (type) {
    ResourceType.DRAWABLE -> ClassName("org.jetbrains.compose.resources", "DrawableResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
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
                    val qualifiers = item.qualifiers.sorted().joinToString { "\"$it\"" }
                    //file separator should be '/' on all platforms
                    add("%T(setOf($qualifiers), \"${item.path.invariantSeparatorsPathString}\"),\n", resourceItemClass)
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