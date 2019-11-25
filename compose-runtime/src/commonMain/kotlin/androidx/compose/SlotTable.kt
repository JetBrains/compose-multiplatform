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

package androidx.compose

/**
 * A gap buffer implementation of the composition slot space. A slot space can be thought of as
 * a custom List<Any?> that optimizes around inserts and removes.
 *
 * Slots stores slots, groups, and nodes.
 *
 *   Slot  - A slot is the primitive base type of the slot space. It is of type Any? and can hold
 *           any value.
 *   Group - A group is a keyed group of slots. The group counts the number of slots and nodes it
 *           contains.
 *   Node  - A node is a special group that is counted by the containing groups.
 *
 * All groups and nodes are just grouping of slots and use slots to describe the groups. At
 * the root of a slot space is a group. Groups count the number nodes that are in the group. A node
 * only counts as one node in its group regardless of the number of nodes it contains.
 *
 * ASIDE:
 * The intent is that groups represent memoized function calls and nodes represent views. For
 * example:
 *
 * @sample androidx.compose.samples.initialGroup
 *
 * the `LinearLayout` here would be a node (the linear layout view). The node contains the
 * groups for the child views of the linear layout.
 *
 * If contact's composition looks like:
 *
 * @sample androidx.compose.samples.contactSample
 *
 * then composing contact into the linear layout would add two views to the linear layout's
 * children. The composition of contact creates groups, one for each text view. The groups for each
 * contact would be able to report that it produces two views (that is the group created for
 * Contact has two nodes). Summing the nodes in the group produces the number of views (as
 * each node corresponds to a view).
 *
 * If the order of jim and bob change:
 *
 * @sample androidx.compose.samples.reorderedGroup
 *
 * the previous result can be reused by moving the views generated bob's group before jim's (or vis
 * versa). A composition algorithm could use the key information for each group to determine if they
 * can be switched. For example, since the first contact's group has two nodes the composition
 * algorithm can infer that the beginning of jim's views starts at 2 and contains 2 view. To move
 * jim in front of bob, move the 2 views from offset 2 to offset 0. If contact is immutable, for
 * example, Contact would only need to be recomposed if the value of jim or bob change.
 */
open class SlotEditor internal constructor(val table: SlotTable) {
    var current = 0
    internal val slots get() = table.slots
    internal fun effectiveIndex(index: Int) = table.effectiveIndex(index)
    internal var currentEnd = table.slots.size
    internal var nodeCount = 0
    internal var startStack = IntStack()
    internal val groupKindStack = IntStack()
    internal val nodeCountStack = IntStack()
    internal val endStack = IntStack()
    internal val keyStack = Stack<Any>()

    /**
     * Return true if the current slot starts a group
     */
    val isGroup get() = current < currentEnd && get(current) is GroupStart

    /**
     * Return true if the slot at index starts a gorup
     */
    fun isGroup(index: Int) = get(index) is GroupStart

    /**
     * Return true if the current slot starts a node. A node is a kind of group so this will
     * return true for isGroup as well.
     */
    val isNode get() = current < currentEnd && (get(current) as? GroupStart)?.isNode ?: false

    /**
     * Return the number of nodes in the group. isGroup must be true or this will throw.
     */
    val groupSize get() = get(current).asGroupStart.slots

    /**
     * Return the size of the group at index. isGroup(index) must be true of this will throw.
     */
    fun groupSize(index: Int): Int = get(index).asGroupStart.slots

    /**
     * Get the number of nodes emitted to the group prior to the current slot.
     */
    val nodeIndex get() = nodeCount

    /**
     * Get the total number of nodes emitted in the group containing the current.
     */
    val parentNodes: Int
        get() {
            return if (startStack.isEmpty()) 0
            else slots[effectiveIndex(startStack.peek())].asGroupStart.nodes
        }

    val parentIndex: Int get() = if (startStack.isEmpty()) 0 else startStack.peek()

    /**
     * Get the value at an Anchor
     */
    fun get(anchor: Anchor) = if (anchor.loc >= 0) slots[anchor.loc] else SlotTable.EMPTY

    /**
     * Get the value at the index'th slot.
     */
    fun get(index: Int) = effectiveIndex(index).let {
        if (it < slots.size) slots[it] else SlotTable.EMPTY
    }

    internal fun advance(): Any? {
        if (current >= currentEnd) {
            return SlotTable.EMPTY
        }
        val index = current++
        return slots[effectiveIndex(index)]
    }

    internal fun recordStartGroup(key: Any, kind: GroupKind, validate: Boolean) {
        startStack.push(current)
        groupKindStack.push(kind)
        nodeCountStack.push(nodeCount)
        keyStack.push(if (key == SlotTable.EMPTY) get(current).asGroupStart.key else key)
        // Record the end location as relative to the end of the slot table so when we pop it back
        // off again all inserts and removes that happened while a child group was open are already
        // reflected into its value.
        endStack.push(slots.size - table.gapLen - currentEnd)
        nodeCount = 0
        if (validate) {
            val groupStart = advance().asGroupStart
            require(groupStart.kind == kind) { "Group kind changed" }
            require(key == SlotTable.EMPTY || groupStart.key == key) { "Group key changed" }
            currentEnd = current + groupStart.slots
        }
    }

    internal fun advanceToNextGroup(): Int {
        val groupStart = advance().asGroupStart
        current += groupStart.slots

        val count = if (groupStart.isNode) 1 else groupStart.nodes
        nodeCount += count

        return count
    }

    internal fun recordEndGroup(writing: Boolean, inserting: Boolean, uncertain: Boolean): Int {
        var count = nodeCount
        require(startStack.isNotEmpty()) {
            "Invalid state. Unbalanced calls to startGroup() and endGroup()"
        }
        require(inserting || current == currentEnd) { "Expected to be at the end of a group" }

        // Update group length
        val startLocation = startStack.pop()
        val groupKind = groupKindStack.pop()
        val effectiveStartLocation = effectiveIndex(startLocation)
        val key = keyStack.pop()
        require(slots[effectiveStartLocation] === SlotTable.EMPTY ||
                slots[effectiveStartLocation] is GroupStart
        ) {
            "Invalid state. Start location stack doesn't refer to a start location"
        }

        val len = current - startLocation - 1
        if (writing) {
            slots[effectiveStartLocation] = GroupStart(groupKind, key, len, nodeCount)
        } else {
            val start = slots[effectiveStartLocation].asGroupStart
            // A node count < 0 means that it was reported as uncertain while reading
            require(start.slots == len && (nodeCount == start.nodes || uncertain)) {
                "Invalid endGroup call, expected ${start.slots} slots and ${
                start.nodes} nodes but received, $len slots and $nodeCount nodes"
            }
            count = start.nodes
        }
        nodeCount = nodeCountStack.pop() + if (groupKind == NODE) 1 else nodeCount
        currentEnd = (slots.size - table.gapLen) - endStack.pop()
        if (writing && nodeCountStack.isEmpty()) table.clearGap()
        return count
    }
}

class SlotReader internal constructor(table: SlotTable) : SlotEditor(table) {
    private var emptyCount = 0
    private var uncertainCount = false

    /**
     * Return true if the current location is at the end of a group
     */
    val isGroupEnd get() = inEmpty || current == currentEnd

    /**
     * Get the value at the current slot
     */
    fun get() = if (emptyCount > 0) SlotTable.EMPTY else slots[effectiveIndex(current - 1)]

    /**
     * Return the key for the current group or EMPTY
     */
    val groupKey get() = (get(current) as? GroupStart)?.key ?: SlotTable.EMPTY

    /**
     * Return the group key at index. isGroup(index) must be true or this will throw.
     */
    fun groupKey(index: Int) = get(index).asGroupStart.key

    /**
     * Get the value of the next slot. During empty mode this value is always EMPTY which is the
     * value a newly inserted slot.
     */
    fun next(): Any? {
        if (emptyCount > 0) {
            return SlotTable.EMPTY
        }
        return advance()
    }

    /**
     * Backup one slot. For example, we ran into a key of a keyed group we don't want, this backs up
     * current to be before the key.
     */
    fun previous() {
        if (emptyCount <= 0) {
            require(current > 0) { "Invalid call to previous" }
            current--
        }
    }

    /**
     * Begin reporting empty for all calls to next() or get(). beginEmpty() can be nested and must
     * be called with a balanced number of endEmpty()
     */
    fun beginEmpty() {
        emptyCount++
    }

    val inEmpty get() = emptyCount > 0

    /**
     * End reporting empty for calls to net() and get().
     */
    fun endEmpty() {
        require(emptyCount > 0) { "Unbalanced begin/end empty" }
        emptyCount--
    }

    fun close() = table.close(this)

    /**
     * Start a group. Passing an EMPTY as the key will enter the group without validating the key.
     */
    fun startGroup(key: Any) = startGroup(key, GROUP)

    private fun startGroup(key: Any, kind: GroupKind) {
        if (emptyCount <= 0) {
            recordStartGroup(key, kind, validate = true)
        }
    }

    /**
     *  Skip a group. Must be called at the start of a group.
     */
    fun skipGroup(): Int {
        require(emptyCount == 0) { "Cannot skip while in an empty region" }
        return advanceToNextGroup()
    }

    /**
     * Skip the to the end of the group.
     */
    fun skipEnclosingGroup(): Int {
        require(emptyCount == 0) { "Cannot skip the enclosing group while in an empty region" }
        require(startStack.isNotEmpty()) { "No enclosing group to skip" }
        val startLocation = startStack.peek()
        val start = get(startLocation).asGroupStart
        current = currentEnd
        uncertainCount = true
        return start.nodes
    }

    /**
     * End the current group. Must be called after the corresponding startGroup().
     */
    fun endGroup(): Int {
        if (emptyCount <= 0) {
            return recordEndGroup(writing = false, inserting = false, uncertain = uncertainCount)
        } else return 0
    }

    /**
     * Start a node.
     */
    fun startNode(key: Any) = startGroup(key, NODE)

    /**
     * End a node
     */
    fun endNode() = endGroup()

    /**
     * Skip a node
     */
    fun skipNode() = skipGroup()

    fun reportUncertainNodeCount() {
        uncertainCount = true
    }

    /**
     * Extract the keys from this point to the end of the group. The current is left unaffected.
     * Must be called inside a group.
     */
    fun extractKeys(): MutableList<KeyInfo> {
        val result = mutableListOf<KeyInfo>()
        if (emptyCount > 0) return result
        val oldCurrent = current
        val oldNodeCount = nodeCount
        var index = 0
        while (current < currentEnd) {
            val location = current
            val key = get(location).asGroupStart.key
            result.add(KeyInfo(key, location, skipGroup(), index++))
        }
        current = oldCurrent
        this.nodeCount = oldNodeCount
        return result
    }

    override fun toString(): String {
        return "SlotReader" +
                "(current=$current, " +
                "size=${slots.size - table.gapLen}, " +
                "gap=${
        if (table.gapLen > 0) "$table.gapStart-${table.gapStart + table.gapLen - 1}" else "none"}${
        if (inEmpty) ", in empty" else ""})"
    }
}

class SlotWriter internal constructor(table: SlotTable) : SlotEditor(table) {
    private var insertCount = 0
    private var pendingClear = false

    fun close() = table.close(this)

    /**
     * Set the value of the next slot.
     */
    fun update(value: Any?): Any? {
        val result = skip()
        set(value)
        return result
    }

    /**
     * Set the value at the current slot.
     */
    fun set(value: Any?) {
        slots[effectiveIndex(current - 1)] = value
    }

    /**
     * Skip the current slot without updating
     */
    fun skip(): Any? {
        if (insertCount > 0) {
            insert(1)
        }
        val index = current++
        return slots[table.effectiveIndex(index)]
    }

    /**
     * Backup one slot. For example, we ran into a key of a keyed group we don't want, this backs up
     * current to be before the key.
     */
    fun previous() {
        require(current > 0) { "Invalid call to previous" }
        current--
    }

    /**
     * Begin inserting at the current location. beginInsert() can be nested and must be called with
     * a balanced number of endInsert()
     */
    fun beginInsert() {
        insertCount++
    }

    /**
     * Ends inserting.
     */
    fun endInsert() {
        require(insertCount > 0) { "Unbalenced begin/end insert" }
        insertCount--
    }

    /**
     * Start a group.
     *
     * @param key The group key. Passing EMPTY will retain as was written last time.
     *            An EMPTY key is not valid when inserting groups.
     */
    fun startGroup(key: Any) = startGroup(key, GROUP)

    private fun startGroup(key: Any, kind: GroupKind) {
        val inserting = insertCount > 0
        recordStartGroup(key, kind, validate = !inserting)
        if (inserting) {
            require(key != SlotTable.EMPTY) { "Inserting an EMPTY key" }
            skip() // Skip a slot for the GroupStart added by endGroup.
            currentEnd = current
        }
    }

    /**
     *  Skip a group. Must be called at the start of a group.
     */
    fun skipGroup(): Int {
        require(insertCount == 0) { "Cannot skip while inserting" }
        return advanceToNextGroup()
    }

    /**
     * End the current group. Must be called after the corresponding startGroup().
     */
    fun endGroup(): Int =
        recordEndGroup(writing = true, inserting = insertCount > 0, uncertain = false)

    /**
     * Move the offset'th group after the current group to the current location.
     */
    fun moveGroup(offset: Int) {
        require(insertCount == 0) { "Cannot move a group while inserting" }
        val oldCurrent = current
        val oldNodeCount = nodeCount

        // Find the group to move
        var count = offset
        while (count > 0) {
            advanceToNextGroup()
            count--
        }

        // Move the current one here by first inserting room for it then copying it over the spot
        // then removing the old slot.
        val moveLocation = current
        advanceToNextGroup()
        val moveLen = current - moveLocation
        current = oldCurrent
        insert(moveLen)
        // insert inserted moveLen slots which moved moveLocation
        val newMoveLocation = moveLocation + moveLen
        current = oldCurrent
        nodeCount = oldNodeCount

        slots.copyInto(slots, effectiveIndex(current),
            effectiveIndex(newMoveLocation), effectiveIndex(newMoveLocation) + moveLen)

        // Before we remove the old location, move any anchors
        table.moveAnchors(newMoveLocation, current, moveLen)

        // Remove the now duplicate entries
        val anchorsRemoved = remove(moveLocation + moveLen, moveLen)
        require(!anchorsRemoved) { "Unexpectedly removed anchors" }
    }

    /**
     * Remove a group. Must be called at group.
     */
    fun removeGroup(): Boolean {
        require(insertCount == 0) { "Cannot remove group while inserting" }
        val oldCurrent = current
        val count = advanceToNextGroup()
        val anchorsRemoved = remove(oldCurrent, current - oldCurrent)
        current = oldCurrent
        nodeCount -= count
        return anchorsRemoved
    }

    /**
     * Returns an iterator for the slots of group.
     */
    fun groupSlots(): Iterator<Any?> {
        val start = current
        val oldCount = nodeCount
        advanceToNextGroup()
        val end = current
        current = start
        nodeCount = oldCount
        return object : Iterator<Any?> {
            var current = start + 1
            override fun hasNext(): Boolean = current < end
            override fun next(): Any? = slots[effectiveIndex(current++)]
        }
    }

    /**
     * Start a node.
     */
    fun startNode(key: Any) = startGroup(key, NODE)

    /**
     * End a node
     */
    fun endNode() = endGroup()

    /**
     * Skip a node
     */
    fun skipNode() = skipGroup()

    /**
     * Allocate an anchor for a location. As content is inserted and removed from the slot table the
     * anchor is updated to reflect those changes. For example, if an anchor is requested for an
     * group, the anchor will report the location of that group even if the group is moved in the slot
     * table. If the position referenced by the anchor is removed, the anchor location is set to -1.
     */
    fun anchor(index: Int = current): Anchor = table.anchor(index)

    private fun moveGapTo(index: Int) {
        if (table.gapLen > 0 && table.gapStart != index) {
            trace("SlotTable:moveGap") {
                pendingClear = false
                if (table.anchors.isNotEmpty()) table.updateAnchors(index)
                if (index < table.gapStart) {
                    slots.copyInto(slots, index + table.gapLen,
                        index, table.gapStart)
                } else {
                    slots.copyInto(slots, table.gapStart,
                        table.gapStart + table.gapLen,
                        index + table.gapLen)
                }
                table.gapStart = index
                pendingClear = true
            }
        } else {
            table.gapStart = index
        }
    }

    private fun insert(size: Int) {
        if (size > 0) {
            moveGapTo(current)
            if (table.gapLen < size) {
                trace("SlotTable:grow") {
                    // Create a bigger gap
                    val oldCapacity = slots.size
                    val oldSize = slots.size - table.gapLen
                    // Double the size of the array, but at least MIN_GROWTH_SIZE and >= size
                    val newCapacity = kotlin.math.max(
                        kotlin.math.max(oldCapacity * 2, oldSize + size),
                        MIN_GROWTH_SIZE
                    )
                    val newSlots = arrayOfNulls<Any?>(newCapacity)
                    val newGapLen = newCapacity - oldSize
                    val oldGapEnd = table.gapStart + table.gapLen
                    val newGapEnd = table.gapStart + newGapLen
                    // Copy the old array into the new array
                    slots.copyInto(newSlots, 0, 0, table.gapStart)
                    slots.copyInto(newSlots, newGapEnd, oldGapEnd, oldCapacity)

                    // Update the anchors
                    if (table.anchors.isNotEmpty()) table.anchorGapResize(newGapLen - table.gapLen)

                    // Update the gap and slots
                    table.slots = newSlots
                    table.gapLen = newGapLen
                }
            }
            if (currentEnd >= table.gapStart) currentEnd += size
            table.gapStart += size
            table.gapLen -= size

            repeat(size) {
                slots[current + it] = SlotTable.EMPTY
            }
            pendingClear = true
        }
    }

    internal fun remove(start: Int, len: Int): Boolean {
        return if (len > 0) {
            pendingClear = false
            var anchorsRemoved = false
            if (table.gapLen == 0) {
                // If there is no current gap, just make the removed slots the gap
                table.gapStart = start
                if (table.anchors.isNotEmpty()) anchorsRemoved = table.removeAnchors(start, len)
                table.gapLen = len
            } else {
                // Move the gap to the startGroup + len location and set the gap startGroup to
                // startGroup and gap len to len + gapLen
                val removeEnd = start + len
                moveGapTo(removeEnd)
                if (table.anchors.isNotEmpty()) anchorsRemoved = table.removeAnchors(start, len)
                table.gapStart = start
                table.gapLen += len
            }
            if (currentEnd >= table.gapStart) currentEnd -= len
            pendingClear = true
            anchorsRemoved
        } else false
    }

    override fun toString(): String {
        if (pendingClear) {
            pendingClear = false
            table.clearGap()
        }
        return "SlotWriter" +
                "(current=$current, " +
                "size=${slots.size - table.gapLen}, " +
                "gap=${
        if (table.gapLen > 0) "$table.gapStart-${table.gapStart + table.gapLen - 1}" else "none"}${
        if (insertCount > 0) ", inserting" else ""})"
    }
}

private val Any?.asGroupStart: GroupStart
    get() = this as? GroupStart ?: error("Expected a group start")

internal data class GroupStart(val kind: GroupKind, val key: Any, val slots: Int, val nodes: Int) {
    val isNode get() = kind == NODE
}

class SlotTable(internal var slots: Array<Any?> = arrayOf()) {
    private var readers = 0
    private var writer = false
    internal var gapStart: Int = 0
    internal var gapLen: Int = 0
    internal var anchors: ArrayList<Anchor> = arrayListOf()

    fun <T> read(block: (reader: SlotReader) -> T): T = openReader().let { reader ->
        try {
            block(reader)
        } finally {
            reader.close()
        }
    }

    fun <T> write(block: (writer: SlotWriter) -> T): T = openWriter().let { writer ->
        try {
            block(writer)
        } finally {
            writer.close()
        }
    }

    fun groupPathTo(location: Int): List<Int> {
        require(location < slots.size - gapLen)
        val path = mutableListOf<Int>()
        read { reader ->
            var current = 0
            loop@ while (true) {
                path.add(current)
                if (current == location) break
                current++
                while (current < location && !reader.isGroup(current)) current++
                if (current == location && !reader.isGroup(current)) break
                while (current <= location) {
                    val end = reader.groupSize(current) + current + 1
                    if (location < end) continue@loop
                    current = end
                }
                break
            }
        }
        return path
    }

    fun openReader(): SlotReader {
        if (writer) error("Cannot read while a writer is pending")
        readers++
        return SlotReader(this)
    }

    fun openWriter(): SlotWriter {
        if (writer) error("Cannot start a writer when another writer is pending")
        if (readers > 0) error("Cannot start a writer when a reader is pending")
        writer = true
        return SlotWriter(this)
    }

    val size: Int get() = slots.size - gapLen

    internal fun close(reader: SlotReader) {
        require(reader.table === this && readers > 0) { "Unexpected reader close()" }
        readers--
    }

    internal fun close(writer: SlotWriter) {
        require(writer.table === this && this.writer) { "Unexpected writer close()" }
        this.writer = false
        clearGap()
    }

    internal fun effectiveIndex(index: Int) = if (index < gapStart) index else gapLen + index

    internal fun clearGap() = repeat(gapLen) { i -> slots[gapStart + i] = null }

    internal fun anchor(index: Int): Anchor {
        // TODO: Consider a buffer gap list of anchors if middle inserts and deletes are common
        val anchorIndex = effectiveIndex(index)
        val location = anchors.search(anchorIndex)
        return if (location < 0) {
            val anchor = Anchor(anchorIndex)
            anchors.add(-(location + 1), anchor)
            anchor
        } else anchors[location]
    }

    internal fun updateAnchors(gapMovedTo: Int) {
        if (gapStart < gapMovedTo) {
            // Gap is moving up
            // All anchors between the new gap and the old gap switch to be anchored to the
            // front of the table instead of the end.
            val rangeStart = gapStart + gapLen
            val rangeEnd = gapMovedTo + gapLen
            var index = anchors.locationOf(rangeStart)
            while (index < anchors.size) {
                val anchor = anchors[index]
                if (anchor.loc < rangeEnd) {
                    anchor.loc -= gapLen
                    index++
                } else break
            }
        } else {
            // Gap is moving down. All anchors between gapMoveTo and gapStart need now to be
            // anchored to the end of the table instead of the front of the table.
            val rangeStart = gapMovedTo
            val rangeEnd = gapStart
            var index = anchors.locationOf(rangeStart)
            while (index < anchors.size) {
                val anchor = anchors[index]
                if (anchor.loc < rangeEnd) {
                    anchor.loc += gapLen
                    index++
                } else break
            }
        }
    }

    internal fun anchorGapResize(delta: Int) {
        val start = anchors.locationOf(gapStart + gapLen)
        for (index in start until anchors.size)
            anchors[index].loc += delta
    }

    internal fun removeAnchors(gapStart: Int, size: Int): Boolean {
        val removeStart = gapStart
        val removeEnd = gapStart + size
        var index = anchors.locationOf(gapStart + size).let {
            if (it >= anchors.size) it - 1 else it
        }
        var anchorsRemoved = false
        while (index >= 0) {
            val anchor = anchors[index]
            if (anchor.loc >= removeStart) {
                if (anchor.loc < removeEnd) {
                    anchor.loc = -1
                    anchors.removeAt(index)
                    anchorsRemoved = true
                }
                index--
            } else break
        }
        return anchorsRemoved
    }

    internal fun moveAnchors(originalLocation: Int, newLocation: Int, size: Int) {
        val effectiveStart = effectiveIndex(originalLocation)
        val effectiveEnd = effectiveIndex(originalLocation + size)

        // Remove all the anchors in range from the original location
        val index = anchors.locationOf(effectiveStart)
        val removedAnchors = mutableListOf<Anchor>()
        if (index >= 0) {
            while (index < anchors.size) {
                val anchor = anchors[index]
                if (anchor.loc >= effectiveStart && anchor.loc < effectiveEnd) {
                    removedAnchors.add(anchor)
                    anchors.removeAt(index)
                } else break
            }
        }

        // Insert the anchors into there new location
        for (anchor in removedAnchors) {
            val location = anchorLocation(anchor)
            val newAnchorLocation = location - originalLocation + newLocation
            val effectiveLocation = effectiveIndex(newAnchorLocation)
            anchor.loc = effectiveLocation
            val insertIndex = anchors.locationOf(effectiveLocation)
            anchors.add(insertIndex, anchor)
        }
    }

    internal fun anchorLocation(anchor: Anchor) = anchor.loc.let {
        if (it > gapStart) it - gapLen else it
    }

    companion object {
        val EMPTY = object {}
    }
}

private fun ArrayList<Anchor>.locationOf(index: Int) =
    search(index).let { if (it >= 0) it else -(it + 1) }
private fun ArrayList<Anchor>.search(index: Int) = binarySearch { it.loc.compareTo(index) }

/**
 * Information about groups and their keys.
 */
class KeyInfo(
    /**
     * The group key.
     */
    val key: Any,
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

class Anchor(internal var loc: Int) {
    val valid get() = loc >= 0
    fun location(slots: SlotTable) = slots.anchorLocation(this)
}

private typealias GroupKind = Int

private const val GROUP: GroupKind = 0
private const val NODE: GroupKind = 1

private const val MIN_GROWTH_SIZE = 128
