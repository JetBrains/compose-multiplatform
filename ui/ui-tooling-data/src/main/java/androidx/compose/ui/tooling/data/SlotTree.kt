/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.tooling.data

import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A group in the slot table. Represents either a call or an emitted node.
 */
@UiToolingDataApi
sealed class Group(
    /**
     * The key is the key generated for the group
     */
    val key: Any?,

    /**
     * The name of the function called, if provided
     */
    val name: String?,

    /**
     * The source location that produce the group if it can be determined
     */
    val location: SourceLocation?,

    /**
     * An optional value that identifies a Group independently of movement caused by recompositions.
     */
    val identity: Any?,

    /**
     * The bounding layout box for the group.
     */
    val box: IntRect,

    /**
     * Any data that was stored in the slot table for the group
     */
    val data: Collection<Any?>,

    /**
     * The child groups of this group
     */
    val children: Collection<Group>
) {
    /**
     * Modifier information for the Group, or empty list if there isn't any.
     */
    open val modifierInfo: List<ModifierInfo> get() = emptyList()

    /**
     * Parameter information for Groups that represent calls
     */
    open val parameters: List<ParameterInformation> get() = emptyList()
}

@UiToolingDataApi
data class ParameterInformation(
    val name: String,
    val value: Any?,
    val fromDefault: Boolean,
    val static: Boolean,
    val compared: Boolean,
    val inlineClass: String?,
    val stable: Boolean
)

/**
 * Source location of the call that produced the call group.
 */
@UiToolingDataApi
data class SourceLocation(
    /**
     * A 0 offset line number of the source location.
     */
    val lineNumber: Int,

    /**
     * Offset into the file. The offset is calculated as the number of UTF-16 code units from
     * the beginning of the file to the first UTF-16 code unit of the call that produced the group.
     */
    val offset: Int,

    /**
     * The length of the source code. The length is calculated as the number of UTF-16 code units
     * that that make up the call expression.
     */
    val length: Int,

    /**
     * The file name (without path information) of the source file that contains the call that
     * produced the group. A source file names are not guaranteed to be unique, [packageHash] is
     * included to help disambiguate files with duplicate names.
     */
    val sourceFile: String?,

    /**
     * A hash code of the package name of the file. This hash is calculated by,
     *
     *   `packageName.fold(0) { hash, current -> hash * 31 + current.toInt() }?.absoluteValue`
     *
     * where the package name is the dotted name of the package. This can be used to disambiguate
     * which file is referenced by [sourceFile]. This number is -1 if there was no package hash
     * information generated such as when the file does not contain a package declaration.
     */
    val packageHash: Int
)

/**
 * A group that represents the invocation of a component
 */
@UiToolingDataApi
class CallGroup(
    key: Any?,
    name: String?,
    box: IntRect,
    location: SourceLocation?,
    identity: Any?,
    override val parameters: List<ParameterInformation>,
    data: Collection<Any?>,
    children: Collection<Group>
) : Group(key, name, location, identity, box, data, children)

/**
 * A group that represents an emitted node
 */
@UiToolingDataApi
class NodeGroup(
    key: Any?,

    /**
     * An emitted node
     */
    val node: Any,
    box: IntRect,
    data: Collection<Any?>,
    override val modifierInfo: List<ModifierInfo>,
    children: Collection<Group>
) : Group(key, null, null, null, box, data, children)

@UiToolingDataApi
private object EmptyGroup : Group(
    key = null,
    name = null,
    location = null,
    identity = null,
    box = emptyBox,
    data = emptyList(),
    children = emptyList()
)

/**
 * A key that has being joined together to form one key.
 */
@UiToolingDataApi
data class JoinedKey(val left: Any?, val right: Any?)

internal val emptyBox = IntRect(0, 0, 0, 0)

private val tokenizer = Regex("(\\d+)|([,])|([*])|([:])|L|(P\\([^)]*\\))|(C(\\(([^)]*)\\))?)|@")

private fun MatchResult.isNumber() = groups[1] != null
private fun MatchResult.number() = groupValues[1].parseToInt()
private val MatchResult.text get() = groupValues[0]
private fun MatchResult.isChar(c: String) = text == c
private fun MatchResult.isFileName() = groups[4] != null
private fun MatchResult.isParameterInformation() = groups[5] != null
private fun MatchResult.isCallWithName() = groups[6] != null
private fun MatchResult.callName() = groupValues[8]

private class SourceLocationInfo(val lineNumber: Int?, val offset: Int?, val length: Int?)

@UiToolingDataApi
private class SourceInformationContext(
    val name: String?,
    val sourceFile: String?,
    val packageHash: Int,
    val locations: List<SourceLocationInfo>,
    val repeatOffset: Int,
    val parameters: List<Parameter>?,
    val isCall: Boolean
) {
    private var nextLocation = 0

    fun nextSourceLocation(): SourceLocation? {
        if (nextLocation >= locations.size && repeatOffset >= 0) {
            nextLocation = repeatOffset
        }
        if (nextLocation < locations.size) {
            val location = locations[nextLocation++]
            return SourceLocation(
                location.lineNumber ?: -1,
                location.offset ?: -1,
                location.length ?: -1,
                sourceFile,
                packageHash
            )
        }
        return null
    }
}

private val parametersInformationTokenizer = Regex("(\\d+)|,|[!P()]|:([^,!)]+)")
private val MatchResult.isANumber get() = groups[1] != null
private val MatchResult.isClassName get() = groups[2] != null

private class ParseError : Exception()

private class Parameter(
    val sortedIndex: Int,
    val inlineClass: String? = null
)

private fun String.parseToInt(): Int =
    try {
        toInt()
    } catch (_: NumberFormatException) {
        throw ParseError()
    }

private fun String.parseToInt(radix: Int): Int =
    try {
        toInt(radix)
    } catch (_: NumberFormatException) {
        throw ParseError()
    }

// The parameter information follows the following grammar:
//
//   parameters: (parameter|run) ("," parameter | run)*
//   parameter: sorted-index [":" inline-class]
//   sorted-index: <number>
//   inline-class: <chars not "," or "!">
//   run: "!" <number>
//
// The full description of this grammar can be found in the ComposableFunctionBodyTransformer of the
// compose compiler plugin.
private fun parseParameters(parameters: String): List<Parameter> {
    var currentResult = parametersInformationTokenizer.find(parameters)
    val expectedSortedIndex = mutableListOf(0, 1, 2, 3)
    var lastAdded = expectedSortedIndex.size - 1
    val result = mutableListOf<Parameter>()
    fun next(): MatchResult? {
        currentResult?.let { currentResult = it.next() }
        return currentResult
    }

    fun expectNumber(): Int {
        val mr = currentResult
        if (mr == null || !mr.isANumber) throw ParseError()
        next()
        return mr.text.parseToInt()
    }

    fun expectClassName(): String {
        val mr = currentResult
        if (mr == null || !mr.isClassName) throw ParseError()
        next()
        return mr.text
            .substring(1)
            .replacePrefix("c#", "androidx.compose.")
    }

    fun expect(value: String) {
        val mr = currentResult
        if (mr == null || mr.text != value) throw ParseError()
        next()
    }

    fun isChar(value: String): Boolean {
        val mr = currentResult
        return mr == null || mr.text == value
    }

    fun isClassName(): Boolean {
        val mr = currentResult
        return mr != null && mr.isClassName
    }

    fun ensureIndexes(index: Int) {
        val missing = index - lastAdded
        if (missing > 0) {
            val minAddAmount = 4
            val amountToAdd = if (missing < minAddAmount) minAddAmount else missing
            repeat(amountToAdd) {
                expectedSortedIndex.add(it + lastAdded + 1)
            }
            lastAdded += amountToAdd
        }
    }

    try {
        expect("P")
        expect("(")
        loop@ while (!isChar(")")) {
            when {
                isChar("!") -> {
                    // run
                    next()
                    val count = expectNumber()
                    ensureIndexes(result.size + count)
                    repeat(count) {
                        result.add(Parameter(expectedSortedIndex.first()))
                        expectedSortedIndex.removeAt(0)
                    }
                }
                isChar(",") -> next()
                else -> {
                    val index = expectNumber()
                    val inlineClass = if (isClassName()) {
                        expectClassName()
                    } else null
                    result.add(Parameter(index, inlineClass))
                    ensureIndexes(index)
                    expectedSortedIndex.remove(index)
                }
            }
        }
        expect(")")

        // Ensure there are at least as many entries as the highest referenced index.
        while (expectedSortedIndex.size > 0) {
            result.add(Parameter(expectedSortedIndex.first()))
            expectedSortedIndex.removeAt(0)
        }
        return result
    } catch (_: ParseError) {
        return emptyList()
    } catch (_: NumberFormatException) {
        return emptyList()
    }
}

@UiToolingDataApi
private fun sourceInformationContextOf(
    information: String,
    parent: SourceInformationContext?
): SourceInformationContext? {
    var currentResult = tokenizer.find(information)

    fun next(): MatchResult? {
        currentResult?.let { currentResult = it.next() }
        return currentResult
    }

    fun parseLocation(): SourceLocationInfo? {
        var lineNumber: Int? = null
        var offset: Int? = null
        var length: Int? = null

        try {
            var mr = currentResult
            if (mr != null && mr.isNumber()) {
                // Offsets are 0 based in the data, we need 1 based.
                lineNumber = mr.number() + 1
                mr = next()
            }
            if (mr != null && mr.isChar("@")) {
                // Offset
                mr = next()
                if (mr == null || !mr.isNumber()) {
                    return null
                }
                offset = mr.number()
                mr = next()
                if (mr != null && mr.isChar("L")) {
                    mr = next()
                    if (mr == null || !mr.isNumber()) {
                        return null
                    }
                    length = mr.number()
                }
            }
            if (lineNumber != null && offset != null && length != null)
                return SourceLocationInfo(lineNumber, offset, length)
        } catch (_: ParseError) {
            return null
        }
        return null
    }
    val sourceLocations = mutableListOf<SourceLocationInfo>()
    var repeatOffset = -1
    var isCall = false
    var name: String? = null
    var parameters: List<Parameter>? = null
    var sourceFile: String? = null
    var packageHash = -1
    loop@ while (currentResult != null) {
        val mr = currentResult!!
        when {
            mr.isNumber() || mr.isChar("@") -> {
                parseLocation()?.let { sourceLocations.add(it) }
            }
            mr.isChar("C") -> {
                isCall = true
                next()
            }
            mr.isCallWithName() -> {
                isCall = true
                name = mr.callName()
                next()
            }
            mr.isParameterInformation() -> {
                parameters = parseParameters(mr.text)
                next()
            }
            mr.isChar("*") -> {
                repeatOffset = sourceLocations.size
                next()
            }
            mr.isChar(",") -> next()
            mr.isFileName() -> {
                sourceFile = information.substring(mr.range.last + 1)
                val hashText = sourceFile.substringAfterLast("#", "")
                if (hashText.isNotEmpty()) {
                    // Remove the hash information
                    sourceFile = sourceFile
                        .substring(0 until sourceFile.length - hashText.length - 1)
                    packageHash = try {
                        hashText.parseToInt(36)
                    } catch (_: NumberFormatException) {
                        -1
                    }
                }
                break@loop
            }
            else -> break@loop
        }
        if (mr == currentResult)
            return null
    }

    return SourceInformationContext(
        name = name,
        sourceFile = sourceFile ?: parent?.sourceFile,
        packageHash = if (sourceFile != null) packageHash else parent?.packageHash ?: packageHash,
        locations = sourceLocations,
        repeatOffset = repeatOffset,
        parameters = parameters,
        isCall = isCall
    )
}

/**
 * Iterate the slot table and extract a group tree that corresponds to the content of the table.
 */
@UiToolingDataApi
private fun CompositionGroup.getGroup(parentContext: SourceInformationContext?): Group {
    val key = key
    val context = sourceInfo?.let { sourceInformationContextOf(it, parentContext) }
    val node = node
    val data = mutableListOf<Any?>()
    val children = mutableListOf<Group>()
    data.addAll(this.data)
    for (child in compositionGroups)
        children.add(child.getGroup(context))

    val modifierInfo = if (node is LayoutInfo) {
        node.getModifierInfo()
    } else {
        emptyList()
    }

    // Calculate bounding box
    val box = when (node) {
        is LayoutInfo -> boundsOfLayoutNode(node)
        else ->
            if (children.isEmpty()) emptyBox else
                children.map { g -> g.box }.reduce { acc, box -> box.union(acc) }
    }
    val location =
        if (context?.isCall == true) { parentContext?.nextSourceLocation() } else { null }
    return if (node != null) NodeGroup(
        key,
        node,
        box,
        data,
        modifierInfo,
        children
    ) else
        CallGroup(
            key,
            context?.name,
            box,
            location,
            identity = if (!context?.name.isNullOrEmpty() &&
                (box.bottom - box.top > 0 || box.right - box.left > 0)
            ) {
                this.identity
            } else {
                null
            },
            extractParameterInfo(data, context),
            data,
            children
        )
}

private fun boundsOfLayoutNode(node: LayoutInfo): IntRect {
    if (!node.isAttached) {
        return IntRect(
            left = 0,
            top = 0,
            right = node.width,
            bottom = node.height
        )
    }
    val position = node.coordinates.positionInWindow()
    val size = node.coordinates.size
    val left = position.x.roundToInt()
    val top = position.y.roundToInt()
    val right = left + size.width
    val bottom = top + size.height
    return IntRect(left = left, top = top, right = right, bottom = bottom)
}

/**
 * Return a group tree for for the slot table that represents the entire content of the slot
 * table.
 */
@UiToolingDataApi
fun CompositionData.asTree(): Group = compositionGroups.firstOrNull()?.getGroup(null)
    ?: EmptyGroup

internal fun IntRect.union(other: IntRect): IntRect {
    if (this == emptyBox) return other else if (other == emptyBox) return this

    return IntRect(
        left = min(left, other.left),
        top = min(top, other.top),
        bottom = max(bottom, other.bottom),
        right = max(right, other.right)
    )
}

@UiToolingDataApi
private fun keyPosition(key: Any?): String? = when (key) {
    is String -> key
    is JoinedKey ->
        keyPosition(key.left)
            ?: keyPosition(key.right)
    else -> null
}

private const val parameterPrefix = "${'$'}"
private const val internalFieldPrefix = parameterPrefix + parameterPrefix
private const val defaultFieldName = "${internalFieldPrefix}default"
private const val changedFieldName = "${internalFieldPrefix}changed"
private const val jacocoDataField = "${parameterPrefix}jacoco"
private const val recomposeScopeNameSuffix = ".RecomposeScopeImpl"

@UiToolingDataApi
private fun extractParameterInfo(
    data: List<Any?>,
    context: SourceInformationContext?
): List<ParameterInformation> {
    if (data.isNotEmpty()) {
        val recomposeScope = data.firstOrNull {
            it != null && it.javaClass.name.endsWith(recomposeScopeNameSuffix)
        }
        if (recomposeScope != null) {
            try {
                val blockField = recomposeScope.javaClass.accessibleField("block")
                if (blockField != null) {
                    val block = blockField.get(recomposeScope)
                    if (block != null) {
                        val blockClass = block.javaClass
                        val defaultsField = blockClass.accessibleField(defaultFieldName)
                        val changedField = blockClass.accessibleField(changedFieldName)
                        val default =
                            if (defaultsField != null) defaultsField.get(block) as Int else 0
                        val changed =
                            if (changedField != null) changedField.get(block) as Int else 0
                        val fields = blockClass.declaredFields
                            .filter {
                                it.name.startsWith(parameterPrefix) &&
                                    !it.name.startsWith(internalFieldPrefix) &&
                                    !it.name.startsWith(jacocoDataField)
                            }.sortedBy { it.name }
                        val parameters = mutableListOf<ParameterInformation>()
                        val parametersMetadata = context?.parameters ?: emptyList()
                        repeat(fields.size) { index ->
                            val metadata = if (index < parametersMetadata.size)
                                parametersMetadata[index] else Parameter(index)
                            if (metadata.sortedIndex >= fields.size) return@repeat
                            val field = fields[metadata.sortedIndex]
                            field.isAccessible = true
                            val value = field.get(block)
                            val fromDefault = (1 shl index) and default != 0
                            val changedOffset = index * BITS_PER_SLOT + 1
                            val parameterChanged = (
                                (SLOT_MASK shl changedOffset) and changed
                                ) shr changedOffset
                            val static = parameterChanged and STATIC_BITS == STATIC_BITS
                            val compared = parameterChanged and STATIC_BITS == 0
                            val stable = parameterChanged and STABLE_BITS == 0
                            parameters.add(
                                ParameterInformation(
                                    name = field.name.substring(1),
                                    value = value,
                                    fromDefault = fromDefault,
                                    static = static,
                                    compared = compared && !fromDefault,
                                    inlineClass = metadata.inlineClass,
                                    stable = stable
                                )
                            )
                        }
                        return parameters
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }
    return emptyList()
}

private const val BITS_PER_SLOT = 3
private const val SLOT_MASK = 0b111
private const val STATIC_BITS = 0b011
private const val STABLE_BITS = 0b100

/**
 * The source position of the group extracted from the key, if one exists for the group.
 */
@UiToolingDataApi
val Group.position: String?
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @UiToolingDataApi
    get() = keyPosition(key)

private fun Class<*>.accessibleField(name: String): Field? = declaredFields.firstOrNull {
    it.name == name
}?.apply { isAccessible = true }

private fun String.replacePrefix(prefix: String, replacement: String) =
    if (startsWith(prefix)) replacement + substring(prefix.length) else this
