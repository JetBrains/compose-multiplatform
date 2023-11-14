package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.nio.file.Path

internal data class ResourceItem(
    val type: String,
    val qualifiers: Set<String>,
    val id: String,
    val path: Path
)

internal fun getResFileSpec(
    //type -> id -> items
    resources: Map<String, Map<String, List<ResourceItem>>>,
    packageName: String,
    fileName: String
): FileSpec = FileSpec.builder(packageName, fileName).apply {
    addType(TypeSpec.objectBuilder("Res").apply {
        addModifiers(KModifier.INTERNAL)
        val types = resources.map { (typeName, idToResources) ->
            getResourceObject(typeName, idToResources)
        }
        addTypes(types)
    }.build())
}.build()

private fun getResourceObject(typeName: String, nameToResources: Map<String, List<ResourceItem>>) =
    TypeSpec.objectBuilder(typeName).apply {
        addProperties(typeName, nameToResources.keys.toList())
    }.build()

private fun TypeSpec.Builder.addProperties(type: String, names: List<String>) {
    val props = names.map { name ->
        PropertySpec.builder(name, String::class, KModifier.CONST)
            .initializer("\"${getResourceId(type, name)}\"")
            .build()
    }
    addProperties(props)
}

internal fun generateResourceIndex(
    //type -> id -> items
    resources: Map<String, Map<String, List<ResourceItem>>>
) = buildString {
    resources.entries.sortedBy { it.key }.forEach { (typeName, nameToResources) ->
        nameToResources.entries.sortedBy { it.key }.forEach { (name, resources) ->
            //line format
            //type:name = q1:q2:filePath; filePath
            val id = getResourceId(typeName, name)
            val resStr = resources.joinToString("; ") {
                val qStr = it.qualifiers.joinToString(":")
                "$qStr${if (qStr.isBlank()) "" else ":"}${it.path}"
            }
            appendLine("$id = $resStr")
        }
    }
}

private fun getResourceId(type: String, name: String) = "$type:$name"