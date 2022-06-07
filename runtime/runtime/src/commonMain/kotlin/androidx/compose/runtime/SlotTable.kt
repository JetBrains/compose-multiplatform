/*
 * Copyright 2019 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.fastFilterIndexed
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastMap
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import kotlin.math.max
import kotlin.math.min

// Nomenclature -
// Address      - an absolute offset into the array ignoring its gap. See Index below.
// Anchor       - an encoding of Index that allows it to not need to be updated when groups or slots
//                are inserted or deleted. An anchor is positive if the Index it is tracking is
//                before the gap and negative if it is after the gap. If the Anchor is negative, it
//                records its distance from the end of the array. If slots or groups are inserted or
//                deleted this distance doesn't change but the index it represents automatically
//                reflects the deleted or inserted groups or slots. Anchors only need to be updated
//                if they track indexes whose group or slot Address moves when the gap moves.
//                The term anchor is used not only for the Anchor class but for the parent anchor
//                and data anchors in the group fields which are also anchors but not Anchor
//                instances.
// Aux          - auxiliary data that can be associated with a node and set independent of groups
//                slots. This is used, for example, by the composer to record CompositionLocal
//                maps as the map is not known at the when the group starts, only when the map is
//                calculated after using an arbitrary number of slots.
// Data         - the portion of the slot array associated with the group that contains the slots as
//                well as the ObjectKey, Node, and Aux if present. The slots for a group are after
//                the optional fixed group data.
// Group fields - a set of 5 contiguous integer elements in the groups array aligned at 5
//                containing the key, node count, group size parent anchor and an anchor to the
//                group data and flags to indicate if the group is a node, has Aux or has an
//                ObjectKey. There are a set of extension methods used to access the group fields.
// Group        - a contiguous range in the groups array. The groups is an inlined array of group
//                fields. The group fields for a group and all of its children's fields comprise
//                a group. A groups describes how to interpret the slot array. Groups have an
//                integer key, and optional object key, node and aux and a 0 or more slots.
//                Groups form a tree where the child groups are immediately after the group
//                fields for the group. This data structure favors a linear scan of the children.
//                Random access is expensive unless it is through a group anchor.
// Index        - the logical index of a group or slot in its array. The index doesn't change when
//                the gap moves. See Address and Anchor above. The index and address of a group
//                or slot are identical if the gap is at the end of the buffer. This is taken
//                advantage of in the SlotReader.
// Key          - an Int value used as a key by the composer.
// Node         - a value of a node group that can be set independently of the slots of the group.
//                This is, for example, where the LayoutNode is stored by the slot table when
//                emitting using the UIEmitter.
// ObjectKey    - an object that can be used by the composer as part of a groups key. The key()
//                composable, for example, produces an ObjectKey. Using the key composable
//                function, for example, produces an ObjectKey value.
// Slot         - and element in the slot array. Slots are managed by and addressed through a group.
//                Slots are allocated in the slots array and are stored in order of their respective
//                groups.

// All fields and variables referring to an array index are assumed to be Index values, not Address
// values, unless explicitly ending in Address.

// For simplicity and efficiency of the reader, the gaps are always moved to the end resulting in
// Index and Address being identical in a reader.

// The public API refers only to Index values. Address values are internal.

internal class SlotTable : CompositionData, Iterable<CompositionGroup> {
    /**
     * An array to store group information that is stored as groups of [Group_Fields_Size]
     * elements of the array. The [groups] array can be thought of as an array of an inline
     * struct.
     */
    var groups = IntArray(0)
        private set

    /**
     * The number of groups contained in [groups].
     */
    var groupsSize = 0
        private set

    /**
     * An array that stores the slots for a group. The slot elements for a group start at the
     * offset returned by [dataAnchor] of [groups] and continue to the next group's slots or to
     * [slotsSize] for the last group. When in a writer the [dataAnchor] is an anchor instead of
     * an index as [slots] might contain a gap.
     */
    var slots = Array<Any?>(0) { null }
        private set

    /**
     * The number of slots used in [slots].
     */
    var slotsSize = 0
        private set

    /**
     * Tracks the number of active readers. A SlotTable can have multiple readers but only one
     * writer.
     */
    private var readers = 0

    /**
     * Tracks whether there is an active writer.
     */
    internal var writer = false
        private set

    /**
     * An internal version that is incremented whenever a writer is created. This is used to
     * detect when an iterator created by [CompositionData] is invalid.
     */
    internal var version = 0

    /**
     * A list of currently active anchors.
     */
    internal var anchors: ArrayList<Anchor> = arrayListOf()

    /**
     * Returns true if the slot table is empty
     */
    override val isEmpty get() = groupsSize == 0

    /**
     * Read the slot table in [block]. Any number of readers can be created but a slot table cannot
     * be read while it is being written to.
     *
     * @see SlotReader
     */
    inline fun <T> read(block: (reader: SlotReader) -> T): T = openReader()
        .let { reader ->
            try {
                block(reader)
            } finally {
                reader.close()
            }
        }

    /**
     * Write to the slot table in [block]. Only one writer can be created for a slot table at a
     * time and all readers must be closed an do readers can be created while the slot table is
     * being written to.
     *
     * @see SlotWriter
     */
    inline fun <T> write(block: (writer: SlotWriter) -> T): T = openWriter()
        .let { writer ->
            try {
                block(writer)
            } finally {
                writer.close()
            }
        }

    /**
     * Open a reader. Any number of readers can be created but a slot table cannot be read while
     * it is being written to.
     *
     * @see SlotReader
     */
    fun openReader(): SlotReader {
        if (writer) error("Cannot read while a writer is pending")
        readers++
        return SlotReader(table = this)
    }

    /**
     * Open a writer. Only one writer can be created for a slot table at a time and all readers
     * must be closed an do readers can be created while the slot table is being written to.
     *
     * @see SlotWriter
     */
    fun openWriter(): SlotWriter {
        runtimeCheck(!writer) { "Cannot start a writer when another writer is pending" }
        runtimeCheck(readers <= 0) { "Cannot start a writer when a reader is pending" }
        writer = true
        version++
        return SlotWriter(this)
    }

    /**
     * Return an anchor to the given index. [anchorIndex] can be used to determine the current index
     * of the group currently at [index] after a [SlotWriter] as inserted or removed groups or the
     * group itself was moved. [Anchor.valid] will be `false` if the group at [index] was removed.
     *
     * If an anchor is moved using [SlotWriter.moveFrom] or [SlotWriter.moveTo] the anchor will move
     * to be owned by the receiving table. [ownsAnchor] can be used to determine if the group
     * at [index] is still in this table.
     */
    fun anchor(index: Int): Anchor {
        runtimeCheck(!writer) { "use active SlotWriter to create an anchor location instead " }
        require(index in 0 until groupsSize) { "Parameter index is out of range" }
        return anchors.getOrAdd(index, groupsSize) {
            Anchor(index)
        }
    }

    /**
     * Return the group index for [anchor]. This [SlotTable] is assumed to own [anchor] but that
     * is not validated. If [anchor] is not owned by this [SlotTable] the result is undefined.
     * If a [SlotWriter] is open the [SlotWriter.anchorIndex] must be called instead as [anchor]
     * might be affected by the modifications being performed by the [SlotWriter].
     */
    fun anchorIndex(anchor: Anchor): Int {
        runtimeCheck(!writer) { "Use active SlotWriter to determine anchor location instead" }
        require(anchor.valid) { "Anchor refers to a group that was removed" }
        return anchor.location
    }

    /**
     * Returns true if [anchor] is owned by this [SlotTable] or false if it is owned by a
     * different [SlotTable] or no longer part of this table (e.g. it was moved or the group it
     * was an anchor for was removed).
     */
    fun ownsAnchor(anchor: Anchor): Boolean {
        return anchor.valid && anchors.search(anchor.location, groupsSize).let {
            it >= 0 && anchors[it] == anchor
        }
    }

    /**
     * Returns true if the [anchor] is for the group at [groupIndex] or one of it child groups.
     */
    fun groupContainsAnchor(groupIndex: Int, anchor: Anchor): Boolean {
        runtimeCheck(!writer) { "Writer is active" }
        runtimeCheck(groupIndex in 0 until groupsSize) { "Invalid group index" }
        return ownsAnchor(anchor) &&
            anchor.location in groupIndex until (groupIndex + groups.groupSize(groupIndex))
    }

    /**
     * Close [reader].
     */
    internal fun close(reader: SlotReader) {
        require(reader.table === this && readers > 0) { "Unexpected reader close()" }
        readers--
    }

    /**
     * Close [writer] and adopt the slot arrays returned. The [SlotTable] is invalid until
     * [SlotWriter.close] is called as the [SlotWriter] is modifying [groups] and [slots]
     * directly and will only make copies of the arrays if the slot table grows.
     */
    internal fun close(
        writer: SlotWriter,
        groups: IntArray,
        groupsSize: Int,
        slots: Array<Any?>,
        slotsSize: Int,
        anchors: ArrayList<Anchor>
    ) {
        require(writer.table === this && this.writer) { "Unexpected writer close()" }
        this.writer = false
        setTo(groups, groupsSize, slots, slotsSize, anchors)
    }

    /**
     * Used internally by [SlotWriter.moveFrom] to swap arrays with a slot table target
     * [SlotTable] is empty.
     */
    internal fun setTo(
        groups: IntArray,
        groupsSize: Int,
        slots: Array<Any?>,
        slotsSize: Int,
        anchors: ArrayList<Anchor>
    ) {
        // Adopt the slots from the writer
        this.groups = groups
        this.groupsSize = groupsSize
        this.slots = slots
        this.slotsSize = slotsSize
        this.anchors = anchors
    }

    /**
     * Modifies the current slot table such that every group with the target key will be
     * invalidated, and when recomposed, the content of those groups will be disposed and
     * re-inserted.
     *
     * This is currently only used for developer tooling such as Live Edit to invalidate groups
     * which we know will no longer have the same structure so we want to remove them before
     * recomposing.
     *
     * Returns true if all the groups were successfully invalidated. If this returns fals then
     * the a full composition must be foreced.
     */
    internal fun invalidateGroupsWithKey(target: Int): List<RecomposeScopeImpl>? {
        val anchors = mutableListOf<Anchor>()
        val scopes = mutableListOf<RecomposeScopeImpl>()
        var allScopesFound = true

        // Invalidate groups with the target key
        read { reader ->
            fun scanGroup() {
                val key = reader.groupKey
                if (key == target) {
                    anchors.add(reader.anchor())
                    if (allScopesFound) {
                        val nearestScope = findEffectiveRecomposeScope(reader.currentGroup)
                        if (nearestScope != null) {
                            scopes.add(nearestScope)
                        } else {
                            allScopesFound = false
                            scopes.clear()
                        }
                    }
                    reader.skipGroup()
                    return
                }
                reader.startGroup()
                while (!reader.isGroupEnd) {
                    scanGroup()
                }
                reader.endGroup()
            }
            scanGroup()
        }

        // Bash groups even if we could not invalidate it. The call is responsible for ensuring
        // the group is recomposed when this happens.
        write { writer ->
            writer.startGroup()
            anchors.fastForEach { anchor ->
                if (anchor.toIndexFor(writer) >= writer.currentGroup) {
                    writer.seek(anchor)
                    writer.bashGroup()
                }
            }
            writer.skipToGroupEnd()
            writer.endGroup()
        }

        return if (allScopesFound) scopes else null
    }

    /**
     * Find the nearest recompose scope for [group] that, when invalidated, will cause [group]
     * group to be recomposed.
     */
    private fun findEffectiveRecomposeScope(group: Int): RecomposeScopeImpl? {
        var current = group
        while (current > 0) {
            for (data in DataIterator(this, current)) {
                if (data is RecomposeScopeImpl) {
                    return data
                }
            }
            current = groups.parentAnchor(current)
        }
        return null
    }

    /**
     * Finds the nearest recompose scope to the provided group and invalidates it. Return
     * true if the invalidation will cause the scope to reccompose, otherwise false which will
     * require forcing recomposition some other way.
     */
    private fun invalidateGroup(group: Int): Boolean {
        var current = group
        // for each parent up the spine
        while (current >= 0) {
            for (data in DataIterator(this, current)) {
                if (data is RecomposeScopeImpl) {
                    data.requiresRecompose = true
                    return data.invalidateForResult(null) != InvalidationResult.IGNORED
                }
            }
            current = groups.parentAnchor(current)
        }
        return false
    }

    /**
     * A debugging aid to validate the internal structure of the slot table. Throws an exception
     * if the slot table is not in the expected shape.
     */
    fun verifyWellFormed() {
        // If the check passes Address and Index are identical so there is no need for
        // indexToAddress conversions.
        var current = 0
        fun validateGroup(parent: Int, parentEnd: Int): Int {
            val group = current++
            val parentIndex = groups.parentAnchor(group)
            check(parentIndex == parent) {
                "Invalid parent index detected at $group, expected parent index to be $parent " +
                    "found $parentIndex"
            }
            val end = group + groups.groupSize(group)
            check(end <= groupsSize) {
                "A group extends past the end of the table at $group"
            }
            check(end <= parentEnd) {
                "A group extends past its parent group at $group"
            }

            val dataStart = groups.dataAnchor(group)
            val dataEnd = if (group >= groupsSize - 1) slotsSize else groups.dataAnchor(group + 1)
            check(dataEnd <= slots.size) {
                "Slots for $group extend past the end of the slot table"
            }
            check(dataStart <= dataEnd) {
                "Invalid data anchor at $group"
            }
            val slotStart = groups.slotAnchor(group)
            check(slotStart <= dataEnd) {
                "Slots start out of range at $group"
            }
            val minSlotsNeeded = (if (groups.isNode(group)) 1 else 0) +
                (if (groups.hasObjectKey(group)) 1 else 0) +
                (if (groups.hasAux(group)) 1 else 0)
            check(dataEnd - dataStart >= minSlotsNeeded) {
                "Not enough slots added for group $group"
            }
            val isNode = groups.isNode(group)
            check(!isNode || slots[groups.nodeIndex(group)] != null) {
                "No node recorded for a node group at $group"
            }
            var nodeCount = 0
            while (current < end) {
                nodeCount += validateGroup(group, end)
            }
            val expectedNodeCount = groups.nodeCount(group)
            val expectedSlotCount = groups.groupSize(group)
            check(expectedNodeCount == nodeCount) {
                "Incorrect node count detected at $group, " +
                    "expected $expectedNodeCount, received $nodeCount"
            }
            val actualSlotCount = current - group
            check(expectedSlotCount == actualSlotCount) {
                "Incorrect slot count detected at $group, expected $expectedSlotCount, received " +
                    "$actualSlotCount"
            }
            if (groups.containsAnyMark(group)) {
                check(group <= 0 || groups.containsMark(parent)) {
                    "Expected group $parent to record it contains a mark because $group does"
                }
            }

            return if (isNode) 1 else nodeCount
        }

        if (groupsSize > 0) {
            while (current < groupsSize) {
                validateGroup(-1, current + groups.groupSize(current))
            }
            check(current == groupsSize) {
                "Incomplete group at root $current expected to be $groupsSize"
            }
        }

        // Verify anchors are well-formed
        var lastLocation = -1
        anchors.fastForEach { anchor ->
            val location = anchor.toIndexFor(this)
            require(location in 0..groupsSize) { "Location out of bound" }
            require(lastLocation < location) { "Anchor is out of order" }
            lastLocation = location
        }
    }

    /**
     * A debugging aid that renders the slot table as a string. [toString] is avoided as this is
     * potentially a very expensive operation for large slot tables and calling it in the
     * debugger should never be implicit.
     */
    @Suppress("unused")
    fun asString(): String {
        return if (writer) super.toString() else
            buildString {
                append(super.toString())
                append('\n')
                val groupsSize = groupsSize
                if (groupsSize > 0) {
                    var current = 0
                    while (current < groupsSize) {
                        current += emitGroup(current, 0)
                    }
                } else
                    append("<EMPTY>")
            }
    }

    /**
     * A helper function used by [asString] to render a particular group.
     */
    private fun StringBuilder.emitGroup(index: Int, level: Int): Int {
        repeat(level) { append(' ') }
        append("Group(")
        append(index)
        append(") key=")
        append(groups.key(index))
        fun dataIndex(index: Int) =
            if (index >= groupsSize) slotsSize else groups.dataAnchor(index)

        val groupSize = groups.groupSize(index)
        append(", nodes=")
        append(groups.nodeCount(index))
        append(", size=")
        append(groupSize)
        if (groups.hasMark(index)) {
            append(", mark")
        }
        if (groups.containsMark(index)) {
            append(", contains mark")
        }
        val dataStart = dataIndex(index)
        val dataEnd = dataIndex(index + 1)
        if (dataStart in 0..dataEnd && dataEnd <= slotsSize) {
            if (groups.hasObjectKey(index)) {
                append(" objectKey=${slots[groups.objectKeyIndex(index)]}")
            }
            if (groups.isNode(index)) {
                append(" node=${slots[groups.nodeIndex(index)]}")
            }
            if (groups.hasAux(index)) {
                append(" aux=${slots[groups.auxIndex(index)]}")
            }
            val slotStart = groups.slotAnchor(index)
            if (slotStart < dataEnd) {
                append(", slots=[")
                append(slotStart)
                append(": ")
                for (dataIndex in slotStart until dataEnd) {
                    if (dataIndex != slotStart) append(", ")
                    append(slots[dataIndex].toString())
                }
                append("]")
            }
        } else {
            append(", *invalid data offsets $dataStart-$dataEnd*")
        }
        append('\n')
        var current = index + 1
        val end = index + groupSize
        while (current < end) {
            current += emitGroup(current, level + 1)
        }
        return groupSize
    }

    /**
     * A debugging aid to list all the keys [key] values in the [groups] array.
     */
    @Suppress("unused")
    private fun keys() = groups.keys(groupsSize * Group_Fields_Size)

    /**
     * A debugging aid to list all the [nodeCount] values in the [groups] array.
     */
    @Suppress("unused")
    private fun nodes() = groups.nodeCounts(groupsSize * Group_Fields_Size)

    /**
     * A debugging aid to list all the [parentAnchor] values in the [groups] array.
     */
    @Suppress("unused")
    private fun parentIndexes() = groups.parentAnchors(groupsSize * Group_Fields_Size)

    /**
     * A debugging aid to list all the indexes into the [slots] array from the [groups] array.
     */
    @Suppress("unused")
    private fun dataIndexes() = groups.dataAnchors(groupsSize * Group_Fields_Size)

    /**
     * A debugging aid to list the [groupsSize] of all the groups in [groups].
     */
    @Suppress("unused")
    private fun groupSizes() = groups.groupSizes(groupsSize * Group_Fields_Size)

    @Suppress("unused")
    internal fun slotsOf(group: Int): List<Any?> {
        val start = groups.dataAnchor(group)
        val end = if (group + 1 < groupsSize) groups.dataAnchor(group + 1) else slots.size
        return slots.toList().subList(start, end)
    }

    override val compositionGroups: Iterable<CompositionGroup> get() = this

    override fun iterator(): Iterator<CompositionGroup> =
        GroupIterator(this, 0, groupsSize)
}

/**
 * An [Anchor] tracks a groups as its index changes due to other groups being inserted and
 * removed before it. If the group the [Anchor] is tracking is removed, directly or indirectly,
 * [valid] will return false. The current index of the group can be determined by passing either
 * the [SlotTable] or [SlotWriter] to [toIndexFor]. If a [SlotWriter] is active, it must be used
 * instead of the [SlotTable] as the anchor index could have shifted due to operations performed
 * on the writer.
 */
internal class Anchor(loc: Int) {
    internal var location: Int = loc
    val valid get() = location != Int.MIN_VALUE
    fun toIndexFor(slots: SlotTable) = slots.anchorIndex(this)
    fun toIndexFor(writer: SlotWriter) = writer.anchorIndex(this)
}

/**
 * A reader of a slot table. See [SlotTable]
 */
internal class SlotReader(
    /**
     * The table for whom this is a reader.
     */
    internal val table: SlotTable
) {

    /**
     * A copy of the [SlotTable.groups] array to avoid having indirect through [table].
     */
    private val groups: IntArray = table.groups

    /**
     * A copy of [SlotTable.groupsSize] to avoid having to indirect through [table].
     */
    private val groupsSize: Int = table.groupsSize

    /**
     * A copy of [SlotTable.slots] to avoid having to indirect through [table].
     */
    private val slots: Array<Any?> = table.slots

    /**
     * A Copy of [SlotTable.slotsSize] to avoid having to indirect through [table].
     */
    private val slotsSize: Int = table.slotsSize

    /**
     * The current group that will be started with [startGroup] or skipped with [skipGroup].
     */
    var currentGroup = 0
        private set

    /**
     * The end of the [parent] group.
     */
    var currentEnd = groupsSize
        private set

    /**
     * The parent of the [currentGroup] group which is the last group started with [startGroup].
     */
    var parent = -1
        private set

    /**
     * The number of times [beginEmpty] has been called.
     */
    private var emptyCount = 0

    /**
     * The current slot of [parent]. This slot will be the next slot returned by [next] unless it
     * is equal ot [currentSlotEnd].
     */
    private var currentSlot = 0

    /**
     * The current end slot of [parent].
     */
    private var currentSlotEnd = 0

    /**
     * Return the total number of groups in the slot table.
     */
    val size: Int get() = groupsSize

    /**
     * Return the current slot of the group whose value will be returned by calling [next].
     */
    val slot: Int get() = currentSlot - groups.slotAnchor(parent)

    /**
     * Return the parent index of [index].
     */
    fun parent(index: Int) = groups.parentAnchor(index)

    /**
     * Determine if the slot is start of a node.
     */
    val isNode: Boolean get() = groups.isNode(currentGroup)

    /**
     * Determine if the group at [index] is a node.
     */
    fun isNode(index: Int) = groups.isNode(index)

    /**
     * The number of nodes managed by the current group. For node groups, this is the list of the
     * children nodes.
     */
    val nodeCount: Int get() = groups.nodeCount(currentGroup)

    /**
     * Return the number of nodes for the group at [index].
     */
    fun nodeCount(index: Int) = groups.nodeCount(index)

    /**
     * Return the node at [index] if [index] is a node group else null.
     */
    fun node(index: Int): Any? = if (groups.isNode(index)) groups.node(index) else null

    /**
     * Determine if the reader is at the end of a group and an [endGroup] is expected.
     */
    val isGroupEnd get() = inEmpty || currentGroup == currentEnd

    /**
     * Determine if a [beginEmpty] has been called.
     */
    val inEmpty get() = emptyCount > 0

    /**
     * Get the size of the group at [currentGroup].
     */
    val groupSize get() = groups.groupSize(currentGroup)

    /**
     * Get the size of the group at [index]. Will throw an exception if [index] is not a group
     * start.
     */
    fun groupSize(index: Int) = groups.groupSize(index)

    /**
     * Get location the end of the currently started group.
     */
    val groupEnd get() = currentEnd

    /**
     * Get location of the end of the group at [index].
     */
    fun groupEnd(index: Int) = index + groups.groupSize(index)

    /**
     * Get the key of the current group. Returns 0 if the [currentGroup] is not a group.
     */
    val groupKey
        get() = if (currentGroup < currentEnd) {
            groups.key(currentGroup)
        } else 0

    /**
     * Get the key of the group at [index].
     */
    fun groupKey(index: Int) = groups.key(index)

    /**
     * The group slot index is the index into the current groups slots that will be updated by
     * read by [next].
     */
    val groupSlotIndex get() = currentSlot - groups.slotAnchor(parent)

    /**
     * Determine if the group at [index] has an object key
     */
    fun hasObjectKey(index: Int) = groups.hasObjectKey(index)

    /**
     * Get the object key for the current group or null if no key was provide
     */
    val groupObjectKey
        get() =
            if (currentGroup < currentEnd) groups.objectKey(currentGroup) else null

    /**
     * Get the object key at [index].
     */
    fun groupObjectKey(index: Int) = groups.objectKey(index)

    /**
     * Get the current group aux data.
     */
    val groupAux get() = if (currentGroup < currentEnd) groups.aux(currentGroup) else 0

    /**
     * Get the aux data for the group at [index]
     */
    fun groupAux(index: Int) = groups.aux(index)

    /**
     * Get the node associated with the group if there is one.
     */
    val groupNode get() = if (currentGroup < currentEnd) groups.node(currentGroup) else null

    /**
     * Get the group key at [anchor]. This return 0 if the anchor is not valid.
     */
    fun groupKey(anchor: Anchor) = if (anchor.valid) groups.key(table.anchorIndex(anchor)) else 0

    /**
     * Returns true when the group at [index] was marked with [SlotWriter.markGroup].
     */
    fun hasMark(index: Int) = groups.hasMark(index)

    /**
     * Returns true if the group contains a group, directly or indirectly, that has been marked by
     * a call to [SlotWriter.markGroup].
     */
    fun containsMark(index: Int) = groups.containsMark(index)

    /**
     * Return the number of nodes where emitted into the current group.
     */
    val parentNodes: Int get() = if (parent >= 0) groups.nodeCount(parent) else 0

    /**
     * Return the index of the parent group of the given group
     */
    fun parentOf(index: Int): Int {
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        require(index >= 0 && index < groupsSize) { "Invalid group index $index" }
        return groups.parentAnchor(index)
    }

    /**
     * Return the number of slots allocated to the [currentGroup] group.
     */
    val groupSlotCount: Int
        get() {
            val current = currentGroup
            val start = groups.slotAnchor(current)
            val next = current + 1
            val end = if (next < groupsSize) groups.dataAnchor(next) else slotsSize
            return end - start
        }

    /**
     * Get the value stored at [index] in the parent group's slot.
     */
    fun get(index: Int) = (currentSlot + index).let { slotIndex ->
        if (slotIndex < currentSlotEnd) slots[slotIndex] else Composer.Empty
    }

    /**
     * Get the value of the group's slot at [index] for the [currentGroup] group.
     */
    fun groupGet(index: Int): Any? = groupGet(currentGroup, index)

    /**
     * Get the slot value of the [group] at [index]
     */
    fun groupGet(group: Int, index: Int): Any? {
        val start = groups.slotAnchor(group)
        val next = group + 1
        val end = if (next < groupsSize) groups.dataAnchor(next) else slotsSize
        val address = start + index
        return if (address < end) slots[address] else Composer.Empty
    }

    /**
     * Get the value of the slot at [currentGroup] or [Composer.Empty] if at then end of a group.
     * During empty mode this value is always [Composer.Empty] which is the value a newly inserted
     * slot.
     */
    fun next(): Any? {
        if (emptyCount > 0 || currentSlot >= currentSlotEnd) return Composer.Empty
        return slots[currentSlot++]
    }

    /**
     * Begin reporting empty for all calls to next() or get(). beginEmpty() can be nested and must
     * be called with a balanced number of endEmpty()
     */
    fun beginEmpty() {
        emptyCount++
    }

    /**
     * End reporting [Composer.Empty] for calls to [next] and [get],
     */
    fun endEmpty() {
        require(emptyCount > 0) { "Unbalanced begin/end empty" }
        emptyCount--
    }

    /**
     * Close the slot reader. After all [SlotReader]s have been closed the [SlotTable] a
     * [SlotWriter] can be created.
     */
    fun close() = table.close(this)

    /**
     * Start a group.
     */
    fun startGroup() {
        if (emptyCount <= 0) {
            require(groups.parentAnchor(currentGroup) == parent) { "Invalid slot table detected" }
            parent = currentGroup
            currentEnd = currentGroup + groups.groupSize(currentGroup)
            val current = currentGroup++
            currentSlot = groups.slotAnchor(current)
            currentSlotEnd = if (current >= groupsSize - 1)
                slotsSize else
                groups.dataAnchor(current + 1)
        }
    }

    /**
     * Start a group and validate it is a node group
     */
    fun startNode() {
        if (emptyCount <= 0) {
            require(groups.isNode(currentGroup)) { "Expected a node group" }
            startGroup()
        }
    }

    /**
     *  Skip a group. Must be called at the start of a group.
     */
    fun skipGroup(): Int {
        require(emptyCount == 0) { "Cannot skip while in an empty region" }
        val count = if (groups.isNode(currentGroup)) 1 else groups.nodeCount(currentGroup)
        currentGroup += groups.groupSize(currentGroup)
        return count
    }

    /**
     * Skip to the end of the current group.
     */
    fun skipToGroupEnd() {
        require(emptyCount == 0) { "Cannot skip the enclosing group while in an empty region" }
        currentGroup = currentEnd
    }

    /**
     * Reposition the read to the group at [index].
     */
    fun reposition(index: Int) {
        require(emptyCount == 0) { "Cannot reposition while in an empty region" }
        currentGroup = index
        val parent = if (index < groupsSize) groups.parentAnchor(index) else -1
        this.parent = parent
        if (parent < 0)
            this.currentEnd = groupsSize
        else
            this.currentEnd = parent + groups.groupSize(parent)
        this.currentSlot = 0
        this.currentSlotEnd = 0
    }

    /**
     * Restore the parent to a parent of the current group.
     */
    fun restoreParent(index: Int) {
        val newCurrentEnd = index + groups.groupSize(index)
        val current = currentGroup
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        require(current >= index && current <= newCurrentEnd) {
            "Index $index is not a parent of $current"
        }
        this.parent = index
        this.currentEnd = newCurrentEnd
        this.currentSlot = 0
        this.currentSlotEnd = 0
    }

    /**
     * End the current group. Must be called after the corresponding [startGroup].
     */
    fun endGroup() {
        if (emptyCount == 0) {
            require(currentGroup == currentEnd) { "endGroup() not called at the end of a group" }
            val parent = groups.parentAnchor(parent)
            this.parent = parent
            currentEnd = if (parent < 0)
                groupsSize
            else
                parent + groups.groupSize(parent)
        }
    }

    /**
     * Extract the keys from this point to the end of the group. The current is left unaffected.
     * Must be called inside a group.
     */
    fun extractKeys(): MutableList<KeyInfo> {
        val result = mutableListOf<KeyInfo>()
        if (emptyCount > 0) return result
        var index = 0
        var childIndex = currentGroup
        while (childIndex < currentEnd) {
            result.add(
                KeyInfo(
                    groups.key(childIndex),
                    groups.objectKey(childIndex),
                    childIndex,
                    if (groups.isNode(childIndex)) 1 else groups.nodeCount(childIndex),
                    index++
                )
            )
            childIndex += groups.groupSize(childIndex)
        }
        return result
    }

    internal fun forEachData(group: Int, block: (index: Int, data: Any?) -> Unit) {
        val start = groups.slotAnchor(group)
        val end = if (group + 1 < table.groupsSize)
            table.groups.dataAnchor(group + 1) else table.slotsSize
        for (index in start until end) {
            block(index - start, slots[index])
        }
    }

    override fun toString(): String = "SlotReader(current=$currentGroup, key=$groupKey, " +
        "parent=$parent, end=$currentEnd)"

    /**
     * Create an anchor to the current reader location or [index].
     */
    fun anchor(index: Int = currentGroup) = table.anchors.getOrAdd(index, groupsSize) {
        Anchor(index)
    }

    private fun IntArray.node(index: Int) = if (isNode(index)) {
        slots[nodeIndex(index)]
    } else Composer.Empty

    private fun IntArray.aux(index: Int) = if (hasAux(index)) {
        slots[auxIndex(index)]
    } else Composer.Empty

    private fun IntArray.objectKey(index: Int) = if (hasObjectKey(index)) {
        slots[objectKeyIndex(index)]
    } else null
}

/**
 * Information about groups and their keys.
 */
internal class KeyInfo internal constructor(
    /**
     * The group key.
     */
    val key: Int,

    /**
     * The object key for the group
     */
    val objectKey: Any?,

    /**
     * The location of the group.
     */
    val location: Int,

    /**
     * The number of nodes in the group. If the group is a node this is always 1.
     */
    val nodes: Int,

    /**
     * The index of the key info in the list returned by extractKeys
     */
    val index: Int
)

/**
 * The writer for a slot table. See [SlotTable] for details.
 */
internal class SlotWriter(
    /**
     * The [SlotTable] for whom this is writer.
     */
    internal val table: SlotTable
) {
    /**
     * The gap buffer for groups. This might change as groups are inserted and the array needs to
     * be expanded to account groups. The current valid groups occupy 0 until [groupGapStart]
     * followed [groupGapStart] + [groupGapLen] until groups.size where [groupGapStart]
     * until [groupGapStart] + [groupGapLen] is the gap.
     */
    private var groups: IntArray = table.groups

    /**
     * The gap buffer for the slots. This might change as slots are inserted an and the array
     * needs to be expanded to account for the new slots. The current valid slots occupy 0 until
     * [slotsGapStart] and [slotsGapStart] + [slotsGapLen] until slots.size where [slotsGapStart]
     * until [slotsGapStart] + [slotsGapLen] is the gap.
     */
    private var slots: Array<Any?> = table.slots

    /**
     * A copy of the [SlotWriter.anchors] to avoid having to index through [table].
     */
    private var anchors: ArrayList<Anchor> = table.anchors

    /**
     * Group index of the start of the gap in the groups array.
     */
    private var groupGapStart: Int = table.groupsSize

    /**
     * The number of groups in the gap in the groups array.
     */
    private var groupGapLen: Int = groups.size / Group_Fields_Size - table.groupsSize

    /**
     * The index end of the current group.
     */
    private var currentGroupEnd = table.groupsSize

    /**
     * The location of the [slots] array that contains the data for the [parent] group.
     */
    private var currentSlot = 0

    /**
     * The location of the index in [slots] after the slots for the [parent] group.
     */
    private var currentSlotEnd = 0

    /**
     * The is the index of gap in the [slots] array.
     */
    private var slotsGapStart: Int = table.slotsSize

    /**
     * The number of slots in the gop in the [slots] array.
     */
    private var slotsGapLen: Int = slots.size - table.slotsSize

    /**
     * The owner of the gap is the first group that has a end relative index.
     */
    private var slotsGapOwner = table.groupsSize

    /**
     * The number of times [beginInsert] has been called.
     */
    private var insertCount = 0

    /**
     * The number of nodes in the current group. Used to track when nodes are being added and
     * removed in the [parent] group. Once [endGroup] is called, if the nodes count has changed,
     * the containing groups are updated until a node group is encountered.
     */
    private var nodeCount = 0

    /**
     * A stack of the groups that have been explicitly started. A group can be implicitly started
     * by using [seek] to seek to indirect child and calling [startGroup] on that child. The
     * groups implicitly started groups will be updated when the [endGroup] is called for the
     * indirect child group.
     */
    private val startStack = IntStack()

    /**
     * A stack of the [currentGroupEnd] corresponding with the group is [startStack]. As groups
     * are ended by calling [endGroup], the size of the group might have changed. This stack is a
     * stack of enc group anchors where will reflect the group size change when it is restored by
     * calling [restoreCurrentGroupEnd].
     */
    private val endStack = IntStack()

    /**
     * This a count of the [nodeCount] of the explicitly started groups.
     */
    private val nodeCountStack = IntStack()

    /**
     * The current group that will be started by [startGroup] or skipped by [skipGroup]
     */
    var currentGroup = 0
        private set

    /**
     * True if at the end of a group and an [endGroup] is expected.
     */
    val isGroupEnd get() = currentGroup == currentGroupEnd

    /**
     * Return true if the current slot starts a node. A node is a kind of group so this will
     * return true for isGroup as well.
     */
    val isNode
        get() =
            currentGroup < currentGroupEnd && groups.isNode(groupIndexToAddress(currentGroup))

    /**
     * Return true if the group at [index] is a node.
     */
    fun isNode(index: Int) = groups.isNode(groupIndexToAddress(index))

    /**
     * return the number of nodes contained in the group at [index]
     */
    fun nodeCount(index: Int) = groups.nodeCount(groupIndexToAddress(index))

    /**
     * Return the key for the group at [index].
     */
    fun groupKey(index: Int): Int = groups.key(groupIndexToAddress(index))

    /**
     * Return the object key for the group at [index], if it has one, or null if not.
     */
    fun groupObjectKey(index: Int): Any? {
        val address = groupIndexToAddress(index)
        return if (groups.hasObjectKey(address)) slots[groups.objectKeyIndex(address)] else null
    }

    /**
     * Return the size of the group at [index].
     */
    fun groupSize(index: Int): Int = groups.groupSize(groupIndexToAddress(index))

    /**
     * Return the aux of the group at [index].
     */
    fun groupAux(index: Int): Any? {
        val address = groupIndexToAddress(index)
        return if (groups.hasAux(address)) slots[groups.auxIndex(address)] else Composer.Empty
    }

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    fun indexInParent(index: Int): Boolean = index > parent && index < currentGroupEnd ||
        (parent == 0 && index == 0)

    fun indexInCurrentGroup(index: Int): Boolean = indexInGroup(index, currentGroup)

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    fun indexInGroup(index: Int, group: Int): Boolean {
        // If the group is open then the group size in the groups array has not been updated yet
        // so calculate the end from the stored anchor value in the end stack.
        val end = when {
            group == parent -> currentGroupEnd
            group > startStack.peekOr(0) -> group + groupSize(group)
            else -> {
                val openIndex = startStack.indexOf(group)
                when {
                    openIndex < 0 -> group + groupSize(group)
                    else -> (capacity - groupGapLen) - endStack.peek(openIndex)
                }
            }
        }
        return index > group && index < end
    }

    /**
     * Return the node at [index] if [index] is a node group or null.
     */
    fun node(index: Int): Any? {
        val address = groupIndexToAddress(index)
        return if (groups.isNode(address))
            slots[dataIndexToDataAddress(groups.nodeIndex(address))]
        else null
    }

    /**
     * Return the node at [anchor] if it is a node group or null.
     */
    fun node(anchor: Anchor) = node(anchor.toIndexFor(this))

    /**
     * Return the index of the nearest group that contains [currentGroup].
     */
    var parent: Int = -1
        private set

    /**
     * Return the index of the parent for the group at [index].
     */
    fun parent(index: Int) = groups.parent(index)

    /**
     * Return the index of the parent for the group referenced by [anchor]. If the anchor is not
     * valid it returns -1.
     */
    fun parent(anchor: Anchor) = if (anchor.valid) groups.parent(anchorIndex(anchor)) else -1

    /**
     * True if the writer has been closed
     */
    var closed = false
        private set

    /**
     * Close the writer
     */
    fun close() {
        closed = true
        // Ensure, for readers, there is no gap
        if (startStack.isEmpty()) {
            // Only reset the writer if it closes normally.
            moveGroupGapTo(size)
            moveSlotGapTo(slots.size - slotsGapLen, groupGapStart)
            recalculateMarks()
        }
        table.close(
            writer = this,
            groups = groups,
            groupsSize = groupGapStart,
            slots = slots,
            slotsSize = slotsGapStart,
            anchors = anchors
        )
    }

    /**
     * Reset the writer to the beginning of the slot table and in the state as if it had just been
     * opened. This differs form closing a writer and opening a new one in that the instance
     * doesn't change and the gap in the slots are not reset to the end of the buffer.
     */
    fun reset() {
        runtimeCheck(insertCount == 0) { "Cannot reset when inserting" }
        recalculateMarks()
        currentGroup = 0
        currentGroupEnd = capacity - groupGapLen
        currentSlot = 0
        currentSlotEnd = 0
        nodeCount = 0
    }

    /**
     * Set the value of the next slot. Returns the previous value of the slot or [Composer.Empty]
     * is being inserted.
     */
    fun update(value: Any?): Any? {
        val result = skip()
        set(value)
        return result
    }

    /**
     * Updates the data for the current data group.
     */
    fun updateAux(value: Any?) {
        val address = groupIndexToAddress(currentGroup)
        runtimeCheck(groups.hasAux(address)) {
            "Updating the data of a group that was not created with a data slot"
        }
        slots[dataIndexToDataAddress(groups.auxIndex(address))] = value
    }

    /**
     * Insert aux data into the parent group.
     *
     * This must be done only after at most one value has been inserted into the slot table for
     * the group.
     */
    fun insertAux(value: Any?) {
        runtimeCheck(insertCount >= 0) { "Cannot insert auxiliary data when not inserting" }
        val parent = parent
        val parentGroupAddress = groupIndexToAddress(parent)
        runtimeCheck(!groups.hasAux(parentGroupAddress)) { "Group already has auxiliary data" }
        insertSlots(1, parent)
        val auxIndex = groups.auxIndex(parentGroupAddress)
        val auxAddress = dataIndexToDataAddress(auxIndex)
        if (currentSlot > auxIndex) {
            // One or more values were inserted into the slot table before the aux value, we need
            // to move them. Currently we only will run into one or two slots (the recompose
            // scope inserted by a restart group and the lambda value in a composableLambda
            // instance) so this is the only case currently supported.
            val slotsToMove = currentSlot - auxIndex
            check(slotsToMove < 3) { "Moving more than two slot not supported" }
            if (slotsToMove > 1) {
                slots[auxAddress + 2] = slots[auxAddress + 1]
            }
            slots[auxAddress + 1] = slots[auxAddress]
        }
        groups.addAux(parentGroupAddress)
        slots[auxAddress] = value
        currentSlot++
    }

    /**
     * Updates the node for the current node group to [value].
     */
    fun updateNode(value: Any?) = updateNodeOfGroup(currentGroup, value)

    /**
     * Update the node of a the group at [anchor] to [value].
     */
    fun updateNode(anchor: Anchor, value: Any?) = updateNodeOfGroup(anchor.toIndexFor(this), value)

    /**
     * Updates the node of the parent group.
     */
    fun updateParentNode(value: Any?) = updateNodeOfGroup(parent, value)

    /**
     * Set the value at the groups current data slot
     */
    fun set(value: Any?) {
        runtimeCheck(currentSlot <= currentSlotEnd) {
            "Writing to an invalid slot"
        }
        slots[dataIndexToDataAddress(currentSlot - 1)] = value
    }

    /**
     * Set the group's slot at [index] to [value]. Returns the previous value.
     */
    fun set(index: Int, value: Any?): Any? {
        val address = groupIndexToAddress(currentGroup)
        val slotsStart = groups.slotIndex(address)
        val slotsEnd = groups.dataIndex(groupIndexToAddress(currentGroup + 1))
        val slotsIndex = slotsStart + index
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        runtimeCheck(slotsIndex >= slotsStart && slotsIndex < slotsEnd) {
            "Write to an invalid slot index $index for group $currentGroup"
        }
        val slotAddress = dataIndexToDataAddress(slotsIndex)
        val result = slots[slotAddress]
        slots[slotAddress] = value
        return result
    }

    /**
     * Skip the current slot without updating. If the slot table is inserting then and
     * [Composer.Empty] slot is added and [skip] return [Composer.Empty].
     */
    fun skip(): Any? {
        if (insertCount > 0) {
            insertSlots(1, parent)
        }
        return slots[dataIndexToDataAddress(currentSlot++)]
    }

    /**
     * Read the [index] slot at the group at [anchor]. Returns [Composer.Empty] if the slot is
     * empty (e.g. out of range).
     */
    fun slot(anchor: Anchor, index: Int) = slot(anchorIndex(anchor), index)

    /**
     * Read the [index] slot at the group at index [groupIndex]. Returns [Composer.Empty] if the
     * slot is empty (e.g. out of range).
     */
    fun slot(groupIndex: Int, index: Int): Any? {
        val address = groupIndexToAddress(groupIndex)
        val slotsStart = groups.slotIndex(address)
        val slotsEnd = groups.dataIndex(groupIndexToAddress(groupIndex + 1))
        val slotsIndex = slotsStart + index
        if (slotsIndex !in slotsStart until slotsEnd) {
            return Composer.Empty
        }
        val slotAddress = dataIndexToDataAddress(slotsIndex)
        return slots[slotAddress]
    }

    /**
     * Advance [currentGroup] by [amount]. The [currentGroup] group cannot be advanced outside the
     * currently started [parent].
     */
    fun advanceBy(amount: Int) {
        require(amount >= 0) { "Cannot seek backwards" }
        check(insertCount <= 0) { "Cannot call seek() while inserting" }
        if (amount == 0) return
        val index = currentGroup + amount
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        runtimeCheck(index >= parent && index <= currentGroupEnd) {
            "Cannot seek outside the current group ($parent-$currentGroupEnd)"
        }
        this.currentGroup = index
        val newSlot = groups.dataIndex(groupIndexToAddress(index))
        this.currentSlot = newSlot
        this.currentSlotEnd = newSlot
    }

    /**
     * Seek the current location to [anchor]. The [anchor] must be an anchor to a possibly
     * indirect child of [parent].
     */
    fun seek(anchor: Anchor) = advanceBy(anchor.toIndexFor(this) - currentGroup)

    /**
     * Skip to the end of the current group.
     */
    fun skipToGroupEnd() {
        val newGroup = currentGroupEnd
        currentGroup = newGroup
        currentSlot = groups.dataIndex(groupIndexToAddress(newGroup))
    }

    /**
     * Begin inserting at the current location. beginInsert() can be nested and must be called with
     * a balanced number of endInsert()
     */
    fun beginInsert() {
        if (insertCount++ == 0) {
            saveCurrentGroupEnd()
        }
    }

    /**
     * Ends inserting.
     */
    fun endInsert() {
        check(insertCount > 0) { "Unbalanced begin/end insert" }
        if (--insertCount == 0) {
            runtimeCheck(nodeCountStack.size == startStack.size) {
                "startGroup/endGroup mismatch while inserting"
            }
            restoreCurrentGroupEnd()
        }
    }

    /**
     * Enter the group at current without changing it. Requires not currently inserting.
     */
    fun startGroup() {
        require(insertCount == 0) { "Key must be supplied when inserting" }
        startGroup(key = 0, objectKey = Composer.Empty, isNode = false, aux = Composer.Empty)
    }

    /**
     * Start a group with a integer key
     */
    fun startGroup(key: Int) = startGroup(key, Composer.Empty, isNode = false, aux = Composer.Empty)

    /**
     * Start a group with a data key
     */
    fun startGroup(key: Int, dataKey: Any?) = startGroup(
        key,
        dataKey,
        isNode = false,
        aux = Composer.Empty
    )

    /**
     * Start a node.
     */
    fun startNode(key: Any?) = startGroup(NodeKey, key, isNode = true, aux = Composer.Empty)

    /**
     * Start a node
     */
    fun startNode(key: Any?, node: Any?) = startGroup(NodeKey, key, isNode = true, aux = node)

    /**
     * Start a data group.
     */
    fun startData(key: Int, objectKey: Any?, aux: Any?) = startGroup(
        key,
        objectKey,
        isNode = false,
        aux = aux
    )

    /**
     * Start a data group.
     */
    fun startData(key: Int, aux: Any?) = startGroup(key, Composer.Empty, isNode = false, aux = aux)

    private fun startGroup(key: Int, objectKey: Any?, isNode: Boolean, aux: Any?) {
        val inserting = insertCount > 0
        nodeCountStack.push(nodeCount)

        currentGroupEnd = if (inserting) {
            insertGroups(1)
            val current = currentGroup
            val currentAddress = groupIndexToAddress(current)
            val hasObjectKey = objectKey !== Composer.Empty
            val hasAux = !isNode && aux !== Composer.Empty
            groups.initGroup(
                address = currentAddress,
                key = key,
                isNode = isNode,
                hasDataKey = hasObjectKey,
                hasData = hasAux,
                parentAnchor = parent,
                dataAnchor = currentSlot
            )
            currentSlotEnd = currentSlot

            val dataSlotsNeeded = (if (isNode) 1 else 0) +
                (if (hasObjectKey) 1 else 0) +
                (if (hasAux) 1 else 0)
            if (dataSlotsNeeded > 0) {
                insertSlots(dataSlotsNeeded, current)
                val slots = slots
                var currentSlot = currentSlot
                if (isNode) slots[currentSlot++] = aux
                if (hasObjectKey) slots[currentSlot++] = objectKey
                if (hasAux) slots[currentSlot++] = aux
                this.currentSlot = currentSlot
            }
            nodeCount = 0
            val newCurrent = current + 1
            this.parent = current
            this.currentGroup = newCurrent
            newCurrent
        } else {
            val previousParent = parent
            startStack.push(previousParent)
            saveCurrentGroupEnd()
            val currentGroup = currentGroup
            val currentGroupAddress = groupIndexToAddress(currentGroup)
            if (aux != Composer.Empty) {
                if (isNode)
                    updateNode(aux)
                else
                    updateAux(aux)
            }
            currentSlot = groups.slotIndex(currentGroupAddress)
            currentSlotEnd = groups.dataIndex(
                groupIndexToAddress(this.currentGroup + 1)
            )
            nodeCount = groups.nodeCount(currentGroupAddress)

            this.parent = currentGroup
            this.currentGroup = currentGroup + 1
            currentGroup + groups.groupSize(currentGroupAddress)
        }
    }

    /**
     * End the current group. Must be called after the corresponding startGroup().
     */
    fun endGroup(): Int {
        val inserting = insertCount > 0
        val currentGroup = currentGroup
        val currentGroupEnd = currentGroupEnd

        val groupIndex = parent
        val groupAddress = groupIndexToAddress(groupIndex)
        val newNodes = nodeCount
        val newGroupSize = currentGroup - groupIndex
        val isNode = groups.isNode(groupAddress)
        if (inserting) {
            groups.updateGroupSize(groupAddress, newGroupSize)
            groups.updateNodeCount(groupAddress, newNodes)
            nodeCount = nodeCountStack.pop() + if (isNode) 1 else newNodes
            parent = groups.parent(groupIndex)
        } else {
            require(currentGroup == currentGroupEnd) {
                "Expected to be at the end of a group"
            }
            // Update group length
            val oldGroupSize = groups.groupSize(groupAddress)
            val oldNodes = groups.nodeCount(groupAddress)
            groups.updateGroupSize(groupAddress, newGroupSize)
            groups.updateNodeCount(groupAddress, newNodes)
            val newParent = startStack.pop()
            restoreCurrentGroupEnd()
            this.parent = newParent
            val groupParent = groups.parent(groupIndex)
            nodeCount = nodeCountStack.pop()
            if (groupParent == newParent) {
                // The parent group was started we just need to update the node count.
                nodeCount += if (isNode) 0 else newNodes - oldNodes
            } else {
                // If we are closing a group for which startGroup was called after calling
                // seek(). startGroup allows the indirect children to be started. If the group
                // has changed size or the number of nodes it contains the groups between the
                // group being closed and the group that is currently open need to be adjusted.
                // This allows the caller to avoid the overhead of needing to start and end the
                // groups explicitly.
                val groupSizeDelta = newGroupSize - oldGroupSize
                var nodesDelta = if (isNode) 0 else newNodes - oldNodes
                if (groupSizeDelta != 0 || nodesDelta != 0) {
                    var current = groupParent
                    while (
                        current != 0 &&
                        current != newParent &&
                        (nodesDelta != 0 || groupSizeDelta != 0)
                    ) {
                        val currentAddress = groupIndexToAddress(current)
                        if (groupSizeDelta != 0) {
                            val newSize = groups.groupSize(currentAddress) + groupSizeDelta
                            groups.updateGroupSize(currentAddress, newSize)
                        }
                        if (nodesDelta != 0) {
                            groups.updateNodeCount(
                                currentAddress,
                                groups.nodeCount(currentAddress) + nodesDelta
                            )
                        }
                        if (groups.isNode(currentAddress)) nodesDelta = 0
                        current = groups.parent(current)
                    }
                }
                nodeCount += nodesDelta
            }
        }
        return newNodes
    }

    /**
     * Wraps every child group of the current group with a group of a different key.
     */
    internal fun bashGroup() {
        startGroup()
        while (!isGroupEnd) {
            insertParentGroup(-3)
            skipGroup()
        }
        endGroup()
    }

    /**
     * If the start of a group was skipped using [skip], calling [ensureStarted] puts the writer
     * into the same state as if [startGroup] or [startNode] was called on the group starting at
     * [index]. If, after starting, the group, [currentGroup] is not at the end of the group or
     * [currentGroup] is not at the start of a group for which [index] is not location the parent
     * group, an exception is thrown.
     *
     * Calling [ensureStarted] implies that an [endGroup] should be called once the end of the
     * group is reached.
     */
    fun ensureStarted(index: Int) {
        require(insertCount <= 0) { "Cannot call ensureStarted() while inserting" }
        val parent = parent
        if (parent != index) {
            // The new parent a child of the current group.
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            require(index >= parent && index < currentGroupEnd) {
                "Started group at $index must be a subgroup of the group at $parent"
            }

            val oldCurrent = currentGroup
            val oldCurrentSlot = currentSlot
            val oldCurrentSlotEnd = currentSlotEnd
            currentGroup = index
            startGroup()
            currentGroup = oldCurrent
            currentSlot = oldCurrentSlot
            currentSlotEnd = oldCurrentSlotEnd
        }
    }

    fun ensureStarted(anchor: Anchor) = ensureStarted(anchor.toIndexFor(this))

    /**
     *  Skip the current group. Returns the number of nodes skipped by skipping the group.
     */
    fun skipGroup(): Int {
        val groupAddress = groupIndexToAddress(currentGroup)
        val newGroup = currentGroup + groups.groupSize(groupAddress)
        this.currentGroup = newGroup
        this.currentSlot = groups.dataIndex(groupIndexToAddress(newGroup))
        return if (groups.isNode(groupAddress)) 1 else groups.nodeCount(groupAddress)
    }

    /**
     * Remove the current group. Returns if any anchors were in the group removed.
     */
    fun removeGroup(): Boolean {
        require(insertCount == 0) { "Cannot remove group while inserting" }
        val oldGroup = currentGroup
        val oldSlot = currentSlot
        val count = skipGroup()

        // Remove any recalculate markers ahead of this delete as they are in the group
        // that is being deleted.
        pendingRecalculateMarks?.let {
            while (it.isNotEmpty() && it.peek() >= oldGroup) {
                it.takeMax()
            }
        }

        val anchorsRemoved = removeGroups(oldGroup, currentGroup - oldGroup)
        removeSlots(oldSlot, currentSlot - oldSlot, oldGroup - 1)
        currentGroup = oldGroup
        currentSlot = oldSlot
        nodeCount -= count
        return anchorsRemoved
    }

    /**
     * Returns an iterator for all the slots of group and all the children of the group.
     */
    fun groupSlots(): Iterator<Any?> {
        val start = groups.dataIndex(groupIndexToAddress(currentGroup))
        val end = groups.dataIndex(
            groupIndexToAddress(currentGroup + groupSize(currentGroup))
        )
        return object : Iterator<Any?> {
            var current = start
            override fun hasNext(): Boolean = current < end
            override fun next(): Any? =
                if (hasNext()) slots[dataIndexToDataAddress(current++)] else null
        }
    }

    /**
     * Move the group at [offset] groups after [currentGroup] to be in front of [currentGroup].
     * After this completes, the moved group will be the current group. [offset] must less than the
     * number of groups after the [currentGroup] left in the [parent] group.
     */
    fun moveGroup(offset: Int) {
        require(insertCount == 0) { "Cannot move a group while inserting" }
        require(offset >= 0) { "Parameter offset is out of bounds" }
        if (offset == 0) return
        val current = currentGroup
        val parent = parent
        val parentEnd = currentGroupEnd

        // Find the group to move
        var count = offset
        var groupToMove = current
        while (count > 0) {
            groupToMove += groups.groupSize(
                address = groupIndexToAddress(groupToMove)
            )
            require(groupToMove <= parentEnd) { "Parameter offset is out of bounds" }
            count--
        }

        val moveLen = groups.groupSize(
            address = groupIndexToAddress(groupToMove)
        )
        val currentSlot = currentSlot
        val dataStart = groups.dataIndex(groupIndexToAddress(groupToMove))
        val dataEnd = groups.dataIndex(
            address = groupIndexToAddress(
                index = groupToMove + moveLen
            )
        )
        val moveDataLen = dataEnd - dataStart

        // The order of operations is important here. Moving a block in the array requires,
        //
        //   1) inserting space for the block
        //   2) copying the block
        //   3) deleting the previous block
        //
        // Inserting into a gap buffer requires moving the gap to the location of the insert and
        // then shrinking the gap. Moving the gap in the slot table requires updating all anchors
        // in the group table that refer to locations that changed by the gap move. For this to
        // work correctly, the groups must be in a valid state. That requires that the slot table
        // must be inserted into first so there are no transitory constraint violations in the
        // groups (that is, there are no invalid, duplicate or out of order anchors). Conversely,
        // removing slots also involves moving the gap requiring the groups to be valid so the
        // slots must be removed after the groups that reference the old locations are removed.
        // So the order of operations when moving groups is,
        //
        //  1) insert space for the slots at the destination (must be first)
        //  2) insert space for the groups at the destination
        //  3) copy the groups to their new location
        //  4) copy the slots to their new location
        //  5) fix-up the moved group anchors to refer to the new slot locations
        //  6) update any anchors in the group being moved
        //  7) remove the old groups
        //  8) fix parent anchors
        //  9) remove the old slots (must be last)

        // 1) insert space for the slots at the destination (must be first)
        insertSlots(moveDataLen, max(currentGroup - 1, 0))

        //  2) insert space for the groups at the destination
        insertGroups(moveLen)

        //  3) copy the groups to their new location
        val groups = groups
        val moveLocationAddress = groupIndexToAddress(groupToMove + moveLen)
        val moveLocationOffset = moveLocationAddress * Group_Fields_Size
        val currentAddress = groupIndexToAddress(current)
        groups.copyInto(
            destination = groups,
            destinationOffset = currentAddress * Group_Fields_Size,
            startIndex = moveLocationOffset,
            endIndex = moveLocationOffset + moveLen * Group_Fields_Size
        )

        //  4) copy the slots to their new location
        if (moveDataLen > 0) {
            val slots = slots
            slots.copyInto(
                destination = slots,
                destinationOffset = currentSlot,
                startIndex = dataIndexToDataAddress(dataStart + moveDataLen),
                endIndex = dataIndexToDataAddress(dataEnd + moveDataLen)
            )
        }
        //  5) fix-up the moved group anchors to refer to the new slot locations
        val dataMoveDistance = (dataStart + moveDataLen) - currentSlot
        val slotsGapStart = slotsGapStart
        val slotsGapLen = slotsGapLen
        val slotsCapacity = slots.size
        val slotsGapOwner = slotsGapOwner
        for (group in current until current + moveLen) {
            val groupAddress = groupIndexToAddress(group)
            val oldIndex = groups.dataIndex(groupAddress)
            val newIndex = oldIndex - dataMoveDistance
            val newAnchor = dataIndexToDataAnchor(
                index = newIndex,
                gapStart = if (slotsGapOwner < groupAddress) 0 else slotsGapStart,
                gapLen = slotsGapLen,
                capacity = slotsCapacity
            )
            groups.updateDataIndex(groupAddress, newAnchor)
        }

        //  6) update any anchors in the group being moved
        moveAnchors(groupToMove + moveLen, current, moveLen)

        //  7) remove the old groups
        val anchorsRemoved = removeGroups(groupToMove + moveLen, moveLen)
        runtimeCheck(!anchorsRemoved) { "Unexpectedly removed anchors" }

        //  8) fix parent anchors
        fixParentAnchorsFor(parent, currentGroupEnd, current)

        //  9) remove the old slots (must be last)
        if (moveDataLen > 0) {
            removeSlots(dataStart + moveDataLen, moveDataLen, groupToMove + moveLen - 1)
        }
    }

    companion object {
        private fun moveGroup(
            fromWriter: SlotWriter,
            fromIndex: Int,
            toWriter: SlotWriter,
            updateFromCursor: Boolean,
            updateToCursor: Boolean
        ): List<Anchor> {
            val groupsToMove = fromWriter.groupSize(fromIndex)
            val sourceGroupsEnd = fromIndex + groupsToMove
            val sourceSlotsStart = fromWriter.dataIndex(fromIndex)
            val sourceSlotsEnd = fromWriter.dataIndex(sourceGroupsEnd)
            val slotsToMove = sourceSlotsEnd - sourceSlotsStart
            val hasMarks = fromWriter.containsAnyGroupMarks(fromIndex)

            // Make room in the slot table
            toWriter.insertGroups(groupsToMove)
            toWriter.insertSlots(slotsToMove, toWriter.currentGroup)

            // If either from gap is before the move, move the gap after the move to simplify
            // the logic of this method.
            if (fromWriter.groupGapStart < sourceGroupsEnd) {
                fromWriter.moveGroupGapTo(sourceGroupsEnd)
            }
            if (fromWriter.slotsGapStart < sourceSlotsEnd) {
                fromWriter.moveSlotGapTo(sourceSlotsEnd, sourceGroupsEnd)
            }

            // Copy the groups and slots
            val groups = toWriter.groups
            val currentGroup = toWriter.currentGroup
            fromWriter.groups.copyInto(
                destination = groups,
                destinationOffset = currentGroup * Group_Fields_Size,
                startIndex = fromIndex * Group_Fields_Size,
                endIndex = sourceGroupsEnd * Group_Fields_Size
            )
            val slots = toWriter.slots
            val currentSlot = toWriter.currentSlot
            fromWriter.slots.copyInto(
                destination = slots,
                destinationOffset = currentSlot,
                startIndex = sourceSlotsStart,
                endIndex = sourceSlotsEnd
            )

            // Fix the parent anchors and data anchors. This would read better as two loops but
            // conflating the loops has better locality of reference.
            val parent = toWriter.parent
            groups.updateParentAnchor(currentGroup, parent)
            val parentDelta = currentGroup - fromIndex
            val moveEnd = currentGroup + groupsToMove
            val dataIndexDelta = currentSlot - with(toWriter) { groups.dataIndex(currentGroup) }
            var slotsGapOwner = toWriter.slotsGapOwner
            val slotsGapLen = toWriter.slotsGapLen
            val slotsCapacity = slots.size
            for (groupAddress in currentGroup until moveEnd) {
                // Update the parent anchor, the first group has already been set.
                if (groupAddress != currentGroup) {
                    val previousParent = groups.parentAnchor(groupAddress)
                    groups.updateParentAnchor(groupAddress, previousParent + parentDelta)
                }

                val newDataIndex = with(toWriter) {
                    groups.dataIndex(groupAddress) + dataIndexDelta
                }
                val newDataAnchor = with(toWriter) {
                    dataIndexToDataAnchor(
                        newDataIndex,
                        // Ensure that if the slotGapOwner is below groupAddress we get an end relative
                        // anchor
                        if (slotsGapOwner < groupAddress) 0 else slotsGapStart,
                        slotsGapLen,
                        slotsCapacity
                    )
                }

                // Update the data index
                groups.updateDataAnchor(groupAddress, newDataAnchor)

                // Move the slotGapOwner if necessary
                if (groupAddress == slotsGapOwner) slotsGapOwner++
            }
            toWriter.slotsGapOwner = slotsGapOwner

            // Extract the anchors in range
            val startAnchors = fromWriter.anchors.locationOf(fromIndex, fromWriter.size)
            val endAnchors = fromWriter.anchors.locationOf(sourceGroupsEnd, fromWriter.size)
            val anchors = if (startAnchors < endAnchors) {
                val sourceAnchors = fromWriter.anchors
                val anchors = ArrayList<Anchor>(endAnchors - startAnchors)

                // update the anchor locations to their new location
                val anchorDelta = currentGroup - fromIndex
                for (anchorIndex in startAnchors until endAnchors) {
                    val sourceAnchor = sourceAnchors[anchorIndex]
                    sourceAnchor.location += anchorDelta
                    anchors.add(sourceAnchor)
                }

                // Insert them into the new table
                val insertLocation = toWriter.anchors.locationOf(
                    toWriter.currentGroup,
                    toWriter.size
                )
                toWriter.anchors.addAll(insertLocation, anchors)

                // Remove them from the old table
                sourceAnchors.subList(startAnchors, endAnchors).clear()

                anchors
            } else emptyList()

            val parentGroup = fromWriter.parent(fromIndex)
            val anchorsRemoved = if (updateFromCursor) {
                // Remove the group using the sequence the writer expects when removing a group, that
                // is the root group and the group's parent group must be correctly started and ended
                // when it is not a root group.
                val needsStartGroups = parentGroup >= 0
                if (needsStartGroups) {
                    // If we are not a root group then we are removing from a group so ensure the
                    // root group is started and then seek to the parent group and start it.
                    fromWriter.startGroup()
                    fromWriter.advanceBy(parentGroup - fromWriter.currentGroup)
                    fromWriter.startGroup()
                }
                fromWriter.advanceBy(fromIndex - fromWriter.currentGroup)
                val anchorsRemoved = fromWriter.removeGroup()
                if (needsStartGroups) {
                    fromWriter.skipToGroupEnd()
                    fromWriter.endGroup()
                    fromWriter.skipToGroupEnd()
                    fromWriter.endGroup()
                }
                anchorsRemoved
            } else {
                // Remove the group directly instead of using cursor operations.
                val anchorsRemoved = fromWriter.removeGroups(fromIndex, groupsToMove)
                fromWriter.removeSlots(sourceSlotsStart, slotsToMove, fromIndex - 1)
                anchorsRemoved
            }

            // Ensure we correctly do not remove anchors with the above delete.
            runtimeCheck(!anchorsRemoved) { "Unexpectedly removed anchors" }

            // Update the node count in the toWriter
            toWriter.nodeCount += if (groups.isNode(currentGroup)) 1 else groups.nodeCount(
                currentGroup
            )

            // Move the toWriter's currentGroup passed the insert
            if (updateToCursor) {
                toWriter.currentGroup = currentGroup + groupsToMove
                toWriter.currentSlot = currentSlot + slotsToMove
            }

            // If the group being inserted has marks then update the toWriter's parent marks
            if (hasMarks) {
                toWriter.updateContainsMark(parent)
            }
            return anchors
        }
    }

    /**
     * Move (insert then delete) the group at [anchor] group into the current insert location of
     * [writer]. All anchors in the group are moved into the slot table of [writer]. [anchor]
     * must be a group contained in the current started group.
     *
     * This requires [writer] be inserting and this writer to not be inserting.
     */
    fun moveTo(anchor: Anchor, offset: Int, writer: SlotWriter): List<Anchor> {
        require(writer.insertCount > 0)
        require(insertCount == 0)
        require(anchor.valid)
        val location = anchorIndex(anchor) + offset
        val currentGroup = currentGroup
        require(location in currentGroup until currentGroupEnd)
        val parent = parent(location)
        val size = groupSize(location)
        val nodes = if (isNode(location)) 1 else nodeCount(location)
        val result = moveGroup(
            fromWriter = this,
            fromIndex = location,
            toWriter = writer,
            updateFromCursor = false,
            updateToCursor = false
        )

        updateContainsMark(parent)

        // Fix group sizes and node counts from the parent of the moved group to the current group
        var current = parent
        var updatingNodes = nodes > 0
        while (current >= currentGroup) {
            val currentAddress = groupIndexToAddress(current)
            groups.updateGroupSize(currentAddress, groups.groupSize(currentAddress) - size)
            if (updatingNodes) {
                if (groups.isNode(currentAddress))
                    updatingNodes = false
                else
                    groups.updateNodeCount(currentAddress, groups.nodeCount(currentAddress) - nodes)
            }
            current = parent(current)
        }
        if (updatingNodes) {
            runtimeCheck(nodeCount >= nodes)
            nodeCount -= nodes
        }

        return result
    }

    /**
     * Move (insert and then delete) the group at [index] from [slots]. All anchors in the range
     * (including [index]) are moved to the slot table for which this is a reader.
     *
     * It is required that the writer be inserting.
     *
     * @return a list of the anchors that were moved
     */
    fun moveFrom(table: SlotTable, index: Int): List<Anchor> {
        require(insertCount > 0)

        if (index == 0 && currentGroup == 0 && this.table.groupsSize == 0) {
            // Special case for moving the entire slot table into an empty table. This case occurs
            // during initial composition.
            val myGroups = groups
            val mySlots = slots
            val myAnchors = anchors
            val groups = table.groups
            val groupsSize = table.groupsSize
            val slots = table.slots
            val slotsSize = table.slotsSize
            this.groups = groups
            this.slots = slots
            this.anchors = table.anchors
            this.groupGapStart = groupsSize
            this.groupGapLen = groups.size / Group_Fields_Size - groupsSize
            this.slotsGapStart = slotsSize
            this.slotsGapLen = slots.size - slotsSize
            this.slotsGapOwner = groupsSize

            table.setTo(myGroups, 0, mySlots, 0, myAnchors)
            return this.anchors
        }

        return table.write { tableWriter ->
            moveGroup(
                tableWriter,
                index,
                this,
                updateFromCursor = true,
                updateToCursor = true
            )
        }
    }

    /**
     * Insert a parent group for the rest of the children in the current group. After this call
     * all remaining children of the current group will be parented by a new group and the
     * [currentSlot] will be moved to after the group inserted.
     */
    fun insertParentGroup(key: Int) {
        runtimeCheck(insertCount == 0) { "Writer cannot be inserting" }
        if (isGroupEnd) {
            beginInsert()
            startGroup(key)
            endGroup()
            endInsert()
        } else {
            val currentGroup = currentGroup
            val parent = groups.parent(currentGroup)
            val currentGroupEnd = parent + groupSize(parent)
            val remainingSize = currentGroupEnd - currentGroup
            var nodeCount = 0
            var currentNewChild = currentGroup
            while (currentNewChild < currentGroupEnd) {
                val newChildAddress = groupIndexToAddress(currentNewChild)
                nodeCount += groups.nodeCount(newChildAddress)
                currentNewChild += groups.groupSize(newChildAddress)
            }
            val currentSlot = groups.dataAnchor(groupIndexToAddress(currentGroup))
            beginInsert()
            insertGroups(1)
            endInsert()
            val currentAddress = groupIndexToAddress(currentGroup)
            groups.initGroup(
                address = currentAddress,
                key = key,
                isNode = false,
                hasDataKey = false,
                hasData = false,
                parentAnchor = parent,
                dataAnchor = currentSlot
            )

            // Update the size of the group to cover the remaining children
            groups.updateGroupSize(currentAddress, remainingSize + 1)
            groups.updateNodeCount(currentAddress, nodeCount)

            // Update the parent to account for the new group
            val parentAddress = groupIndexToAddress(parent)
            addToGroupSizeAlongSpine(parentAddress, 1)
            fixParentAnchorsFor(parent, currentGroupEnd, currentGroup)
            this.currentGroup = currentGroupEnd
        }
    }

    fun addToGroupSizeAlongSpine(address: Int, amount: Int) {
        var current = address
        while (current > 0) {
            groups.updateGroupSize(current, groups.groupSize(current) + amount)
            val parentAnchor = groups.parentAnchor(current)
            val parentGroup = parentAnchorToIndex(parentAnchor)
            val parentAddress = groupIndexToAddress(parentGroup)
            current = parentAddress
        }
    }

    /**
     * Insert the group at [index] in [table] to be the content of [currentGroup] plus [offset]
     * without moving [currentGroup].
     *
     * It is required that the writer is *not* inserting and the [currentGroup] is empty.
     *
     * @return a list of the anchors that were moved.
     */
    fun moveIntoGroupFrom(offset: Int, table: SlotTable, index: Int): List<Anchor> {
        runtimeCheck(insertCount <= 0 && groupSize(currentGroup + offset) == 1)
        val previousCurrentGroup = currentGroup
        val previousCurrentSlot = currentSlot
        val previousCurrentSlotEnd = currentSlotEnd
        advanceBy(offset)
        startGroup()
        beginInsert()
        val anchors = table.write { tableWriter ->
            moveGroup(
                tableWriter,
                index,
                this,
                updateFromCursor = false,
                updateToCursor = true
            )
        }
        endInsert()
        endGroup()
        currentGroup = previousCurrentGroup
        currentSlot = previousCurrentSlot
        currentSlotEnd = previousCurrentSlotEnd
        return anchors
    }

    /**
     * Allocate an anchor to the current group or [index].
     */
    fun anchor(index: Int = currentGroup): Anchor = anchors.getOrAdd(index, size) {
        Anchor(if (index <= groupGapStart) index else -(size - index))
    }

    fun markGroup(group: Int = parent) {
        val groupAddress = groupIndexToAddress(group)
        if (!groups.hasMark(groupAddress)) {
            groups.updateMark(groupAddress, true)
            if (!groups.containsMark(groupAddress)) {
                // This is a new mark, record the parent needs to update its contains mark.
                updateContainsMark(parent(group))
            }
        }
    }

    private fun containsGroupMark(group: Int) =
        group >= 0 && groups.containsMark(groupIndexToAddress(group))

    private fun containsAnyGroupMarks(group: Int) =
        group >= 0 && groups.containsAnyMark(groupIndexToAddress(group))

    private var pendingRecalculateMarks: PrioritySet? = null

    private fun recalculateMarks() {
        pendingRecalculateMarks?.let { set ->
            while (set.isNotEmpty()) {
                updateContainsMarkNow(set.takeMax(), set)
            }
        }
    }

    private fun updateContainsMark(group: Int) {
        if (group >= 0) {
            (pendingRecalculateMarks ?: PrioritySet().also { pendingRecalculateMarks = it })
                .add(group)
        }
    }

    private fun updateContainsMarkNow(group: Int, set: PrioritySet) {
        val groupAddress = groupIndexToAddress(group)
        val containsAnyMarks = childContainsAnyMarks(group)
        val markChanges = groups.containsMark(groupAddress) != containsAnyMarks
        if (markChanges) {
            groups.updateContainsMark(groupAddress, containsAnyMarks)
            val parent = parent(group)
            if (parent >= 0) set.add(parent)
        }
    }

    private fun childContainsAnyMarks(group: Int): Boolean {
        var child = group + 1
        val end = group + groupSize(group)
        while (child < end) {
            if (groups.containsAnyMark(groupIndexToAddress(child))) return true
            child += groupSize(child)
        }
        return false
    }

    /**
     * Return the current anchor location while changing the slot table.
     */
    fun anchorIndex(anchor: Anchor) = anchor.location.let { if (it < 0) size + it else it }

    override fun toString(): String {
        return "SlotWriter(current = $currentGroup end=$currentGroupEnd size = $size " +
            "gap=$groupGapStart-${groupGapStart + groupGapLen})"
    }

    /**
     * Save [currentGroupEnd] to [endStack].
     */
    private fun saveCurrentGroupEnd() {
        // Record the end location as relative to the end of the slot table so when we pop it
        // back off again all inserts and removes that happened while a child group was open
        // are already reflected into its value.
        endStack.push(capacity - groupGapLen - currentGroupEnd)
    }

    /**
     * Restore [currentGroupEnd] from [endStack].
     */
    private fun restoreCurrentGroupEnd(): Int {
        val newGroupEnd = (capacity - groupGapLen) - endStack.pop()
        currentGroupEnd = newGroupEnd
        return newGroupEnd
    }

    /**
     * As groups move their parent anchors need to be updated. This recursively updates the
     * parent anchors [parent] starting at [firstChild] and ending at [endGroup]. These are
     * passed as a parameter to as the [groups] does not contain the current values for [parent]
     * yet.
     */
    private fun fixParentAnchorsFor(parent: Int, endGroup: Int, firstChild: Int) {
        val parentAnchor = parentIndexToAnchor(parent, groupGapStart)
        var child = firstChild
        while (child < endGroup) {
            groups.updateParentAnchor(groupIndexToAddress(child), parentAnchor)
            val childEnd = child + groups.groupSize(groupIndexToAddress(child))
            fixParentAnchorsFor(child, childEnd, child + 1)
            child = childEnd
        }
    }

    /**
     * Move the gap in [groups] to [index].
     */
    private fun moveGroupGapTo(index: Int) {
        val gapLen = groupGapLen
        val gapStart = groupGapStart
        if (gapStart != index) {
            if (anchors.isNotEmpty()) updateAnchors(gapStart, index)
            if (gapLen > 0) {
                val groups = groups
                // Here physical is used to mean an index of the actual first int of the group in the
                // array as opposed ot the logical address which is in groups of Group_Field_Size
                // integers. IntArray.copyInto expects physical indexes.
                val groupPhysicalAddress = index * Group_Fields_Size
                val groupPhysicalGapLen = gapLen * Group_Fields_Size
                val groupPhysicalGapStart = gapStart * Group_Fields_Size
                if (index < gapStart) {
                    groups.copyInto(
                        destination = groups,
                        destinationOffset = groupPhysicalAddress + groupPhysicalGapLen,
                        startIndex = groupPhysicalAddress,
                        endIndex = groupPhysicalGapStart
                    )
                } else {
                    groups.copyInto(
                        destination = groups,
                        destinationOffset = groupPhysicalGapStart,
                        startIndex = groupPhysicalGapStart + groupPhysicalGapLen,
                        endIndex = groupPhysicalAddress + groupPhysicalGapLen
                    )
                }
            }

            // Gap has moved so the anchor for the groups that moved have changed so the parent
            // anchors that refer to these groups must be updated.
            var groupAddress = if (index < gapStart) index + gapLen else gapStart
            val capacity = capacity
            runtimeCheck(groupAddress < capacity)
            while (groupAddress < capacity) {
                val oldAnchor = groups.parentAnchor(groupAddress)
                val oldIndex = parentAnchorToIndex(oldAnchor)
                val newAnchor = parentIndexToAnchor(oldIndex, index)
                if (newAnchor != oldAnchor) {
                    groups.updateParentAnchor(groupAddress, newAnchor)
                }
                groupAddress++
                if (groupAddress == index) groupAddress += gapLen
            }
        }
        this.groupGapStart = index
    }

    /**
     * Move the gap in [slots] to [index] where [group] is expected to receive any new slots added.
     */
    private fun moveSlotGapTo(index: Int, group: Int) {
        val gapLen = slotsGapLen
        val gapStart = slotsGapStart
        val slotsGapOwner = slotsGapOwner
        if (gapStart != index) {
            val slots = slots
            if (index < gapStart) {
                // move the gap down to index by shifting the data up.
                slots.copyInto(
                    destination = slots,
                    destinationOffset = index + gapLen,
                    startIndex = index,
                    endIndex = gapStart
                )
            } else {
                // Shift the data down, leaving the gap at index
                slots.copyInto(
                    destination = slots,
                    destinationOffset = gapStart,
                    startIndex = gapStart + gapLen,
                    endIndex = index + gapLen
                )
            }

            // Clear the gap in the data array
            slots.fill(null, index, index + gapLen)
        }

        // Update the data anchors affected by the move
        val newSlotsGapOwner = min(group + 1, size)
        if (slotsGapOwner != newSlotsGapOwner) {
            val slotsSize = slots.size - gapLen
            if (newSlotsGapOwner < slotsGapOwner) {
                var updateAddress = groupIndexToAddress(newSlotsGapOwner)
                val stopUpdateAddress = groupIndexToAddress(slotsGapOwner)
                val groupGapStart = groupGapStart
                while (updateAddress < stopUpdateAddress) {
                    val anchor = groups.dataAnchor(updateAddress)
                    runtimeCheck(anchor >= 0) {
                        "Unexpected anchor value, expected a positive anchor"
                    }
                    groups.updateDataAnchor(updateAddress, -(slotsSize - anchor + 1))
                    updateAddress++
                    if (updateAddress == groupGapStart) updateAddress += groupGapLen
                }
            } else {
                var updateAddress = groupIndexToAddress(slotsGapOwner)
                val stopUpdateAddress = groupIndexToAddress(newSlotsGapOwner)
                while (updateAddress < stopUpdateAddress) {
                    val anchor = groups.dataAnchor(updateAddress)
                    runtimeCheck(anchor < 0) {
                        "Unexpected anchor value, expected a negative anchor"
                    }
                    groups.updateDataAnchor(updateAddress, slotsSize + anchor + 1)
                    updateAddress++
                    if (updateAddress == groupGapStart) updateAddress += groupGapLen
                }
            }
            this.slotsGapOwner = newSlotsGapOwner
        }
        this.slotsGapStart = index
    }

    /**
     * Insert [size] number of groups in front of [currentGroup]. These groups are implicitly a
     * child of [parent].
     */
    private fun insertGroups(size: Int) {
        if (size > 0) {
            val currentGroup = currentGroup
            moveGroupGapTo(currentGroup)
            val gapStart = groupGapStart
            var gapLen = groupGapLen
            val oldCapacity = groups.size / Group_Fields_Size
            val oldSize = oldCapacity - gapLen
            if (gapLen < size) {
                // Create a bigger gap
                val groups = groups

                // Double the size of the array, but at least MinGrowthSize and >= size
                val newCapacity = max(
                    max(oldCapacity * 2, oldSize + size),
                    MinGroupGrowthSize
                )
                val newGroups = IntArray(newCapacity * Group_Fields_Size)
                val newGapLen = newCapacity - oldSize
                val oldGapEndAddress = gapStart + gapLen
                val newGapEndAddress = gapStart + newGapLen

                // Copy the old arrays into the new arrays
                groups.copyInto(
                    destination = newGroups,
                    destinationOffset = 0,
                    startIndex = 0,
                    endIndex = gapStart * Group_Fields_Size
                )
                groups.copyInto(
                    destination = newGroups,
                    destinationOffset = newGapEndAddress * Group_Fields_Size,
                    startIndex = oldGapEndAddress * Group_Fields_Size,
                    endIndex = oldCapacity * Group_Fields_Size
                )

                // Update the gap and slots
                this.groups = newGroups
                gapLen = newGapLen
            }

            // Move the currentGroupEnd to account for inserted groups.
            val currentEnd = currentGroupEnd
            if (currentEnd >= gapStart) this.currentGroupEnd = currentEnd + size

            // Update the gap start and length
            this.groupGapStart = gapStart + size
            this.groupGapLen = gapLen - size

            // Replicate the current group data index to the new slots
            val index = if (oldSize > 0) dataIndex(currentGroup + size) else 0

            // If the slotGapOwner is before the current location ensure we get end relative offsets
            val anchor = dataIndexToDataAnchor(
                index,
                if (slotsGapOwner < gapStart) 0 else slotsGapStart,
                slotsGapLen,
                slots.size
            )
            for (groupAddress in gapStart until gapStart + size) {
                groups.updateDataAnchor(groupAddress, anchor)
            }
            val slotsGapOwner = slotsGapOwner
            if (slotsGapOwner >= gapStart) {
                this.slotsGapOwner = slotsGapOwner + size
            }
        }
    }

    /**
     * Insert room into the slot table. This is performed by first moving the gap to [currentSlot]
     * and then reducing the gap [size] slots. If the gap is smaller than [size] the gap is grown
     * to at least accommodate [size] slots. The new slots are associated with [group].
     */
    private fun insertSlots(size: Int, group: Int) {
        if (size > 0) {
            moveSlotGapTo(currentSlot, group)
            val gapStart = slotsGapStart
            var gapLen = slotsGapLen
            if (gapLen < size) {
                val slots = slots

                // Create a bigger gap
                val oldCapacity = slots.size
                val oldSize = oldCapacity - gapLen

                // Double the size of the array, but at least MinGrowthSize and >= size
                val newCapacity = max(
                    max(oldCapacity * 2, oldSize + size),
                    MinSlotsGrowthSize
                )
                val newData = Array<Any?>(newCapacity) { null }
                val newGapLen = newCapacity - oldSize
                val oldGapEndAddress = gapStart + gapLen
                val newGapEndAddress = gapStart + newGapLen

                // Copy the old arrays into the new arrays
                slots.copyInto(
                    destination = newData,
                    destinationOffset = 0,
                    startIndex = 0,
                    endIndex = gapStart
                )
                slots.copyInto(
                    destination = newData,
                    destinationOffset = newGapEndAddress,
                    startIndex = oldGapEndAddress,
                    endIndex = oldCapacity
                )

                // Update the gap and slots
                this.slots = newData
                gapLen = newGapLen
            }
            val currentDataEnd = currentSlotEnd
            if (currentDataEnd >= gapStart) this.currentSlotEnd = currentDataEnd + size
            this.slotsGapStart = gapStart + size
            this.slotsGapLen = gapLen - size
        }
    }

    /**
     * Remove [len] group from [start].
     */
    private fun removeGroups(start: Int, len: Int): Boolean {
        return if (len > 0) {
            var anchorsRemoved = false
            val anchors = anchors

            // Move the gap to start of the removal and grow the gap
            moveGroupGapTo(start)
            if (anchors.isNotEmpty()) anchorsRemoved = removeAnchors(start, len)
            groupGapStart = start
            val previousGapLen = groupGapLen
            val newGapLen = previousGapLen + len
            groupGapLen = newGapLen

            // Adjust the gap owner if necessary.
            val slotsGapOwner = slotsGapOwner
            if (slotsGapOwner > start) {
                // Use max here as if we delete the current owner this group becomes the owner.
                this.slotsGapOwner = max(start, slotsGapOwner - len)
            }
            if (currentGroupEnd >= groupGapStart) currentGroupEnd -= len

            // Update markers if necessary
            if (containsGroupMark(parent)) {
                updateContainsMark(parent)
            }
            anchorsRemoved
        } else false
    }

    /**
     * Remove [len] slots from [start].
     */
    private fun removeSlots(start: Int, len: Int, group: Int) {
        if (len > 0) {
            val gapLen = slotsGapLen
            val removeEnd = start + len
            moveSlotGapTo(removeEnd, group)
            slotsGapStart = start
            slotsGapLen = gapLen + len
            slots.fill(null, start, start + len)
            val currentDataEnd = currentSlotEnd
            if (currentDataEnd >= start) this.currentSlotEnd = currentDataEnd - len
        }
    }

    /**
     * A helper function to update the number of nodes in a group.
     */
    private fun updateNodeOfGroup(index: Int, value: Any?) {
        val address = groupIndexToAddress(index)
        runtimeCheck(address < groups.size && groups.isNode(address)) {
            "Updating the node of a group at $index that was not created with as a node group"
        }
        slots[dataIndexToDataAddress(groups.nodeIndex(address))] = value
    }

    /**
     * A helper function to update the anchors as the gap in [groups] moves.
     */
    private fun updateAnchors(previousGapStart: Int, newGapStart: Int) {
        val gapLen = groupGapLen
        val size = capacity - gapLen
        if (previousGapStart < newGapStart) {
            // Gap is moving up
            // All anchors between the new gap and the old gap switch to be anchored to the
            // front of the table instead of the end.
            var index = anchors.locationOf(previousGapStart, size)
            while (index < anchors.size) {
                val anchor = anchors[index]
                val location = anchor.location
                if (location < 0) {
                    val newLocation = size + location
                    if (newLocation < newGapStart) {
                        anchor.location = size + location
                        index++
                    } else break
                } else break
            }
        } else {
            // Gap is moving down. All anchors between newGapStart and previousGapStart need now
            // to be anchored to the end of the table instead of the front of the table.
            var index = anchors.locationOf(newGapStart, size)
            while (index < anchors.size) {
                val anchor = anchors[index]
                val location = anchor.location
                if (location >= 0) {
                    anchor.location = -(size - location)
                    index++
                } else break
            }
        }
    }

    /**
     * A helper function to remove the anchors for groups that are removed.
     */
    private fun removeAnchors(gapStart: Int, size: Int): Boolean {
        val gapLen = groupGapLen
        val removeEnd = gapStart + size
        val groupsSize = capacity - gapLen
        var index = anchors.locationOf(gapStart + size, groupsSize).let {
            if (it >= anchors.size) it - 1 else it
        }
        var removeAnchorEnd = 0
        var removeAnchorStart = index + 1
        while (index >= 0) {
            val anchor = anchors[index]
            val location = anchorIndex(anchor)
            if (location >= gapStart) {
                if (location < removeEnd) {
                    anchor.location = Int.MIN_VALUE
                    removeAnchorStart = index
                    if (removeAnchorEnd == 0) removeAnchorEnd = index + 1
                }
                index--
            } else break
        }
        return (removeAnchorStart < removeAnchorEnd).also {
            if (it) anchors.subList(removeAnchorStart, removeAnchorEnd).clear()
        }
    }

    /**
     * A helper function to update anchors for groups that have moved.
     */
    private fun moveAnchors(originalLocation: Int, newLocation: Int, size: Int) {
        val end = originalLocation + size
        val groupsSize = this.size

        // Remove all the anchors in range from the original location
        val index = anchors.locationOf(originalLocation, groupsSize)
        val removedAnchors = mutableListOf<Anchor>()
        if (index >= 0) {
            while (index < anchors.size) {
                val anchor = anchors[index]
                val location = anchorIndex(anchor)
                @Suppress("ConvertTwoComparisonsToRangeCheck")
                if (location >= originalLocation && location < end) {
                    removedAnchors.add(anchor)
                    anchors.removeAt(index)
                } else break
            }
        }

        // Insert the anchors into there new location
        val moveDelta = newLocation - originalLocation
        removedAnchors.fastForEach { anchor ->
            val anchorIndex = anchorIndex(anchor)
            val newAnchorIndex = anchorIndex + moveDelta
            if (newAnchorIndex >= groupGapStart) {
                anchor.location = -(groupsSize - newAnchorIndex)
            } else {
                anchor.location = newAnchorIndex
            }
            val insertIndex = anchors.locationOf(newAnchorIndex, groupsSize)
            anchors.add(insertIndex, anchor)
        }
    }

    /**
     * A debugging aid that emits [groups] as a string.
     */
    @Suppress("unused")
    fun groupsAsString(): String = buildString {
        for (index in 0 until size) {
            groupAsString(index)
            append('\n')
        }
    }

    /**
     * A debugging aid that emits a group as a string into a string builder.
     */
    private fun StringBuilder.groupAsString(index: Int) {
        val address = groupIndexToAddress(index)
        append("Group(")
        if (index < 10) append(' ')
        if (index < 100) append(' ')
        if (index < 1000) append(' ')
        append(index)
        if (address != index) {
            append("(")
            append(address)
            append(")")
        }
        append('#')
        append(groups.groupSize(address))
        fun isStarted(index: Int): Boolean =
            index < currentGroup && (index == parent || startStack.indexOf(index) >= 0 ||
                isStarted(parent(index)))
        val openGroup = isStarted(index)
        if (openGroup) append('?')
        append('^')
        append(parentAnchorToIndex(groups.parentAnchor(address)))
        append(": key=")
        append(groups.key(address))
        append(", nodes=")
        append(groups.nodeCount(address))
        if (openGroup) append('?')
        append(", dataAnchor=")
        append(groups.dataAnchor(address))
        append(", parentAnchor=")
        append(groups.parentAnchor(address))
        if (groups.isNode(address)) {
            append(
                ", node=${
                    slots[
                        dataIndexToDataAddress(groups.nodeIndex(address))
                    ]
                }"
            )
        }

        val startData = groups.slotIndex(address)
        val endData = groups.dataIndex(address + 1)
        if (endData > startData) {
            append(", [")
            for (dataIndex in startData until endData) {
                if (dataIndex != startData) append(", ")
                val dataAddress = dataIndexToDataAddress(dataIndex)
                append("${slots[dataAddress]}")
            }
            append(']')
        }
        append(")")
    }

    internal fun verifyDataAnchors() {
        var previousDataIndex = 0
        val owner = slotsGapOwner
        var ownerFound = false
        val slotsSize = slots.size - slotsGapLen
        for (index in 0 until size) {
            val address = groupIndexToAddress(index)
            val dataAnchor = groups.dataAnchor(address)
            val dataIndex = groups.dataIndex(address)
            check(dataIndex >= previousDataIndex) {
                "Data index out of order at $index, previous = $previousDataIndex, current = " +
                    "$dataIndex"
            }
            check(dataIndex <= slotsSize) {
                "Data index, $dataIndex, out of bound at $index"
            }
            if (dataAnchor < 0 && !ownerFound) {
                check(owner == index) {
                    "Expected the slot gap owner to be $owner found gap at $index"
                }
                ownerFound = true
            }
            previousDataIndex = dataIndex
        }
    }

    @Suppress("unused")
    internal fun verifyParentAnchors() {
        val gapStart = groupGapStart
        val gapLen = groupGapLen
        val capacity = capacity
        for (groupAddress in 0 until gapStart) {
            val parentAnchor = groups.parentAnchor(groupAddress)
            check(parentAnchor > parentAnchorPivot) {
                "Expected a start relative anchor at $groupAddress"
            }
        }
        for (groupAddress in gapStart + gapLen until capacity) {
            val parentAnchor = groups.parentAnchor(groupAddress)
            val parentIndex = parentAnchorToIndex(parentAnchor)
            if (parentIndex < gapStart) {
                check(parentAnchor > parentAnchorPivot) {
                    "Expected a start relative anchor at $groupAddress"
                }
            } else {
                check(parentAnchor <= parentAnchorPivot) {
                    "Expected an end relative anchor at $groupAddress"
                }
            }
        }
    }

    internal val size get() = capacity - groupGapLen
    private val capacity get() = groups.size / Group_Fields_Size

    private fun groupIndexToAddress(index: Int) =
        if (index < groupGapStart) index else index + groupGapLen

    private fun dataIndexToDataAddress(dataIndex: Int) =
        if (dataIndex < slotsGapStart) dataIndex else dataIndex + slotsGapLen

    private fun IntArray.parent(index: Int) =
        parentAnchorToIndex(parentAnchor(groupIndexToAddress(index)))

    private fun dataIndex(index: Int) = groups.dataIndex(groupIndexToAddress(index))

    private fun IntArray.dataIndex(address: Int) =
        if (address >= capacity) slots.size - slotsGapLen
        else dataAnchorToDataIndex(dataAnchor(address), slotsGapLen, slots.size)

    private fun IntArray.slotIndex(address: Int) =
        if (address >= capacity) slots.size - slotsGapLen
        else dataAnchorToDataIndex(slotAnchor(address), slotsGapLen, slots.size)

    private fun IntArray.updateDataIndex(address: Int, dataIndex: Int) {
        updateDataAnchor(
            address,
            dataIndexToDataAnchor(dataIndex, slotsGapStart, slotsGapLen, slots.size)
        )
    }

    private fun IntArray.nodeIndex(address: Int) = dataIndex(address)
    private fun IntArray.auxIndex(address: Int) =
        dataIndex(address) + countOneBits(groupInfo(address) shr (Aux_Shift + 1))

    @Suppress("unused")
    private fun IntArray.dataIndexes() = groups.dataAnchors().let {
        it.slice(0 until groupGapStart) +
            it.slice(groupGapStart + groupGapLen until (size / Group_Fields_Size))
    }.fastMap { anchor -> dataAnchorToDataIndex(anchor, slotsGapLen, slots.size) }

    @Suppress("unused")
    private fun keys() = groups.keys().fastFilterIndexed { index, _ ->
        index < groupGapStart || index >= groupGapStart + groupGapLen
    }

    private fun dataIndexToDataAnchor(index: Int, gapStart: Int, gapLen: Int, capacity: Int) =
        if (index > gapStart) -((capacity - gapLen) - index + 1) else index

    private fun dataAnchorToDataIndex(anchor: Int, gapLen: Int, capacity: Int) =
        if (anchor < 0) (capacity - gapLen) + anchor + 1 else anchor

    private fun parentIndexToAnchor(index: Int, gapStart: Int) =
        if (index < gapStart) index else -(size - index - parentAnchorPivot)

    private fun parentAnchorToIndex(index: Int) =
        if (index > parentAnchorPivot) index else size + index - parentAnchorPivot
}

private class GroupIterator(
    val table: SlotTable,
    start: Int,
    val end: Int
) : Iterator<CompositionGroup> {
    private var index = start
    private val version = table.version

    init {
        if (table.writer) throw ConcurrentModificationException()
    }

    override fun hasNext() = index < end

    override fun next(): CompositionGroup {
        validateRead()
        val group = index

        index += table.groups.groupSize(group)
        return object : CompositionGroup, Iterable<CompositionGroup> {
            override val isEmpty: Boolean get() = table.groups.groupSize(group) == 0

            override val key: Any
                get() = if (table.groups.hasObjectKey(group))
                    table.slots[table.groups.objectKeyIndex(group)]!!
                else table.groups.key(group)

            override val sourceInfo: String?
                get() = if (table.groups.hasAux(group))
                    table.slots[table.groups.auxIndex(group)] as? String
                else null

            override val node: Any?
                get() = if (table.groups.isNode(group))
                    table.slots[table.groups.nodeIndex(group)] else
                    null

            override val data: Iterable<Any?> get() = DataIterator(table, group)

            override val identity: Any
                get() {
                    validateRead()
                    return table.read { it.anchor(group) }
                }

            override val compositionGroups: Iterable<CompositionGroup> get() = this

            override fun iterator(): Iterator<CompositionGroup> {
                validateRead()
                return GroupIterator(
                    table,
                    group + 1,
                    group + table.groups.groupSize(group)
                )
            }
        }
    }

    private fun validateRead() {
        if (table.version != version) {
            throw ConcurrentModificationException()
        }
    }
}

private class DataIterator(
    val table: SlotTable,
    val group: Int,
) : Iterable<Any?>, Iterator<Any?> {
    val start = table.groups.dataAnchor(group)
    val end = if (group + 1 < table.groupsSize)
        table.groups.dataAnchor(group + 1) else table.slotsSize
    var index = start
    override fun iterator(): Iterator<Any?> = this
    override fun hasNext(): Boolean = index < end
    override fun next(): Any? = (
        if (index >= 0 && index < table.slots.size)
            table.slots[index]
        else null
    ).also { index++ }
}

// Parent -1 is reserved to be the root parent index so the anchor must pivot on -2.
private const val parentAnchorPivot = -2

// Group layout
//  0             | 1             | 2             | 3             | 4             |
//  Key           | Group info    | Parent anchor | Size          | Data anchor   |
private const val Key_Offset = 0
private const val GroupInfo_Offset = 1
private const val ParentAnchor_Offset = 2
private const val Size_Offset = 3
private const val DataAnchor_Offset = 4
private const val Group_Fields_Size = 5

// Key is the key parameter passed into startGroup

// Group info is laid out as follows,
// 31 30 29 28_27 26 25 24_23 22 21 20_19 18 17 16__15 14 13 12_11 10 09 08_07 06 05 04_03 02 01 00
// 0  n  ks ds m  cm|                                node count                                    |
// where n is set when the group represents a node
// where ks is whether the group has a object key slot
// where ds is whether the group has a group data slot
// where m is whether the group is marked
// where cm is whether the group contains a mark

// Parent anchor is a group anchor to the parent, as the group gap is moved this value is updated to
// refer to the parent.

// Slot count is the total number of group slots, including itself, occupied by the group.

// Data anchor is an anchor to the group data. The value is positive if it is before the data gap
// and it is negative if it is after the data gap. As gaps are moved, these values are updated.

// Masks and flags
private const val NodeBit_Mask = 0b0100_0000_0000_0000__0000_0000_0000_0000
private const val ObjectKey_Mask = 0b0010_0000_0000_0000__0000_0000_0000_0000
private const val ObjectKey_Shift = 29
private const val Aux_Mask = 0b0001_0000_0000_0000__0000_0000_0000_0000
private const val Aux_Shift = 28
private const val Mark_Mask = 0b0000_1000_0000_0000__0000_0000_0000_0000
private const val ContainsMark_Mask = 0b0000_0100_0000_0000__0000_0000_0000_0000
private const val Slots_Shift = Aux_Shift
private const val NodeCount_Mask = 0b0000_0011_1111_1111__1111_1111_1111_1111

// Special values

// The minimum number of groups to allocate the group table
private const val MinGroupGrowthSize = 32

// The minimum number of data slots to allocate in the data slot table
private const val MinSlotsGrowthSize = 32

// The key to used for nodes
private const val NodeKey = 125

private fun IntArray.groupInfo(address: Int): Int =
    this[address * Group_Fields_Size + GroupInfo_Offset]

private fun IntArray.isNode(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and NodeBit_Mask != 0

private fun IntArray.nodeIndex(address: Int) = this[address * Group_Fields_Size + DataAnchor_Offset]
private fun IntArray.hasObjectKey(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and ObjectKey_Mask != 0

private fun IntArray.objectKeyIndex(address: Int) = (address * Group_Fields_Size).let { slot ->
    this[slot + DataAnchor_Offset] +
        countOneBits(this[slot + GroupInfo_Offset] shr (ObjectKey_Shift + 1))
}

private fun IntArray.hasAux(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and Aux_Mask != 0

private fun IntArray.addAux(address: Int) {
    val arrayIndex = address * Group_Fields_Size + GroupInfo_Offset
    this[arrayIndex] = this[arrayIndex] or Aux_Mask
}

private fun IntArray.hasMark(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and Mark_Mask != 0

private fun IntArray.updateMark(address: Int, value: Boolean) {
    val arrayIndex = address * Group_Fields_Size + GroupInfo_Offset
    if (value) {
        this[arrayIndex] = this[arrayIndex] or Mark_Mask
    } else {
        this[arrayIndex] = this[arrayIndex] and Mark_Mask.inv()
    }
}

private fun IntArray.containsMark(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and ContainsMark_Mask != 0

private fun IntArray.updateContainsMark(address: Int, value: Boolean) {
    val arrayIndex = address * Group_Fields_Size + GroupInfo_Offset
    if (value) {
        this[arrayIndex] = this[arrayIndex] or ContainsMark_Mask
    } else {
        this[arrayIndex] = this[arrayIndex] and ContainsMark_Mask.inv()
    }
}

private fun IntArray.containsAnyMark(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and
        (ContainsMark_Mask or Mark_Mask) != 0

private fun IntArray.auxIndex(address: Int) = (address * Group_Fields_Size).let { slot ->
    if (slot >= size) size
    else this[slot + DataAnchor_Offset] +
        countOneBits(this[slot + GroupInfo_Offset] shr (Aux_Shift + 1))
}

private fun IntArray.slotAnchor(address: Int) = (address * Group_Fields_Size).let { slot ->
    this[slot + DataAnchor_Offset] +
        countOneBits(this[slot + GroupInfo_Offset] shr Slots_Shift)
}

// Count the 1 bits of value less than 8
private fun countOneBits(value: Int) = when (value) {
    0 -> 0
    1 -> 1
    2 -> 1
    3 -> 2
    4 -> 1
    5 -> 2
    6 -> 2
    else -> 3
}

// Key access
private fun IntArray.key(address: Int) = this[address * Group_Fields_Size]
private fun IntArray.keys(len: Int = size) =
    slice(Key_Offset until len step Group_Fields_Size)

// Node count access
private fun IntArray.nodeCount(address: Int) =
    this[address * Group_Fields_Size + GroupInfo_Offset] and NodeCount_Mask

private fun IntArray.updateNodeCount(address: Int, value: Int) {
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    require(value >= 0 && value < NodeCount_Mask)
    this[address * Group_Fields_Size + GroupInfo_Offset] =
        (this[address * Group_Fields_Size + GroupInfo_Offset] and NodeCount_Mask.inv()) or value
}

private fun IntArray.nodeCounts(len: Int = size) =
    slice(GroupInfo_Offset until len step Group_Fields_Size)
        .fastMap { it and NodeCount_Mask }

// Parent anchor
private fun IntArray.parentAnchor(address: Int) =
    this[address * Group_Fields_Size + ParentAnchor_Offset]

private fun IntArray.updateParentAnchor(address: Int, value: Int) {
    this[address * Group_Fields_Size + ParentAnchor_Offset] = value
}

private fun IntArray.parentAnchors(len: Int = size) =
    slice(ParentAnchor_Offset until len step Group_Fields_Size)

// Slot count access
private fun IntArray.groupSize(address: Int) = this[address * Group_Fields_Size + Size_Offset]
private fun IntArray.updateGroupSize(address: Int, value: Int) {
    require(value >= 0)
    this[address * Group_Fields_Size + Size_Offset] = value
}

private fun IntArray.slice(indices: Iterable<Int>): List<Int> {
    val list = mutableListOf<Int>()
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

@Suppress("unused")
private fun IntArray.groupSizes(len: Int = size) =
    slice(Size_Offset until len step Group_Fields_Size)

// Data anchor access
private fun IntArray.dataAnchor(address: Int) =
    this[address * Group_Fields_Size + DataAnchor_Offset]

private fun IntArray.updateDataAnchor(address: Int, anchor: Int) {
    this[address * Group_Fields_Size + DataAnchor_Offset] = anchor
}

private fun IntArray.dataAnchors(len: Int = size) =
    slice(DataAnchor_Offset until len step Group_Fields_Size)

// Update data
private fun IntArray.initGroup(
    address: Int,
    key: Int,
    isNode: Boolean,
    hasDataKey: Boolean,
    hasData: Boolean,
    parentAnchor: Int,
    dataAnchor: Int
) {
    val nodeBit = if (isNode) NodeBit_Mask else 0
    val dataKeyBit = if (hasDataKey) ObjectKey_Mask else 0
    val dataBit = if (hasData) Aux_Mask else 0
    val arrayIndex = address * Group_Fields_Size
    this[arrayIndex + Key_Offset] = key
    this[arrayIndex + GroupInfo_Offset] = nodeBit or dataKeyBit or dataBit
    this[arrayIndex + ParentAnchor_Offset] = parentAnchor
    this[arrayIndex + Size_Offset] = 0
    this[arrayIndex + DataAnchor_Offset] = dataAnchor
}

private fun IntArray.updateGroupKey(
    address: Int,
    key: Int,
) {
    val arrayIndex = address * Group_Fields_Size
    this[arrayIndex + Key_Offset] = key
}

private inline fun ArrayList<Anchor>.getOrAdd(
    index: Int,
    effectiveSize: Int,
    block: () -> Anchor
): Anchor {
    val location = search(index, effectiveSize)
    return if (location < 0) {
        val anchor = block()
        add(-(location + 1), anchor)
        anchor
    } else get(location)
}

/**
 * This is inlined here instead to avoid allocating a lambda for the compare when this is used.
 */
private fun ArrayList<Anchor>.search(location: Int, effectiveSize: Int): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid).location.let { if (it < 0) effectiveSize + it else it }
        val cmp = midVal.compareTo(location)

        when {
            cmp < 0 -> low = mid + 1
            cmp > 0 -> high = mid - 1
            else -> return mid // key found
        }
    }
    return -(low + 1) // key not found
}

/**
 * A wrapper on [search] that always returns an index in to [this] even if [index] is not in the
 * array list.
 */
private fun ArrayList<Anchor>.locationOf(index: Int, effectiveSize: Int) =
    search(index, effectiveSize).let { if (it >= 0) it else -(it + 1) }

/**
 * PropertySet implements a set which allows recording integers into a set an efficiently
 * extracting the greatest max value out of the set. It does this using the heap structure from a
 * heap sort that ensures that adding or removing a value is O(log N) operation even if values are
 * repeatedly added and removed.
 */
internal class PrioritySet(private val list: MutableList<Int> = mutableListOf()) {
    // Add a value to the heap
    fun add(value: Int) {
        // Filter trivial duplicates
        if (list.isNotEmpty() && (list[0] == value || list[list.size - 1] == value)) return

        var index = list.size
        list.add(value)

        // Shift the value up the heap.
        while (index > 0) {
            val parent = ((index + 1) ushr 1) - 1
            val parentValue = list[parent]
            if (value > parentValue) {
                list[index] = parentValue
            } else break
            index = parent
        }
        list[index] = value
    }

    fun isEmpty() = list.isEmpty()
    fun isNotEmpty() = list.isNotEmpty()
    fun peek() = list.first()

    // Remove a de-duplicated value from the heap
    fun takeMax(): Int {
        runtimeCheck(list.size > 0) { "Set is empty" }
        val value = list[0]

        // Skip duplicates. It is not time efficient to remove duplicates from the list while
        // adding so remove them when they leave the list. This also implies that the underlying
        // list's size is not an accurate size of the list so this set doesn't implement size.
        // If size is needed later consider de-duping on insert which might require companion map.
        while (list.isNotEmpty() && list[0] == value) {
            // Shift the last value down.
            list[0] = list.last()
            list.removeAt(list.size - 1)
            var index = 0
            val size = list.size
            val max = list.size ushr 1
            while (index < max) {
                val indexValue = list[index]
                val left = (index + 1) * 2 - 1
                val leftValue = list[left]
                val right = (index + 1) * 2
                if (right < size) {
                    // Note: only right can exceed size because of the constraint on index being
                    // less than floor(list.size / 2)
                    val rightValue = list[right]
                    if (rightValue > leftValue) {
                        if (rightValue > indexValue) {
                            list[index] = rightValue
                            list[right] = indexValue
                            index = right
                            continue
                        } else break
                    }
                }
                if (leftValue > indexValue) {
                    list[index] = leftValue
                    list[left] = indexValue
                    index = left
                } else break
            }
        }
        return value
    }

    fun validateHeap() {
        val size = list.size
        for (index in 0 until size / 2) {
            val left = (index + 1) * 2 - 1
            val right = (index + 1) * 2
            check(list[index] >= list[left])
            check(right >= size || list[index] >= list[right])
        }
    }
}