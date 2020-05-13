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

class SlotReader(val table: SlotTable) {
    var current = 0
        private set
    var currentEnd = table.size
        private set
    var nodeIndex: Int = 0
        private set

    internal val startStack = IntStack()
    private val slots = table.slots
    private var currentGroup: Group? = null
    private var emptyCount = 0
    private val nodeIndexStack = IntStack()

    init {
        require(table.gapStart == currentEnd) { "Gap is not at the end of the slot table" }
    }

    /**
     * Determine the slot at [current] is the start of a group and not at the end of the current
     * group.
     */
    val isGroup get() = current < currentEnd && calculateCurrentGroup() != null

    internal val group get() = assumeGroup()
    internal fun group(location: Int) = slots[location].asGroup
    internal val parentGroup get() = slots[parentLocation].asGroup

    /**
     * Determine if the slot at [index] is the start of a group
     */
    fun isGroup(index: Int) = slots[index] is Group

    /**
     * Determine if the slot is start of a node.
     */
    val isNode get() = calculateCurrentGroup()?.isNode ?: false

    /**
     * Determine if the slot at [location] is a node group. This will throw if the slot at
     * [location] is not a node.
     */
    fun isNode(location: Int) = slots[location].asGroup.isNode

    /**
     * Determine if the reader is at the end of a group and an [endGroup] or [endNode] is expected.
     */
    val isGroupEnd get() = inEmpty || current == currentEnd

    /**
     * Determine if a [beginEmpty] has been called.
     */
    val inEmpty get() = emptyCount > 0

    /**
     * Get the size of the group at [current]. Will throw an exception if the [isGroup] is `false`.
     */
    val groupSize get() = assumeGroup().slots

    /**
     * Get the size of the group at [index]. Will throw an exception if [index] is not a group
     * start.
     */
    fun groupSize(index: Int) = slots[index].asGroup.slots

    /**
     * Get location the end of the currently started group.
     */
    val groupEnd get() = currentEnd

    /**
     * Get location of the end of the group at [index].
     */
    fun groupEnd(index: Int) = index + slots[index].asGroup.slots + 1

    /**
     * Get the data for the current group. Returns null if [current] is not a group
     */
    val groupData get() = if (current < currentEnd) calculateCurrentGroup()?.data else null

    /**
     * Get the key of the current group. Returns [EMPTY] if the [current] is not a group.
     */
    val groupKey get() = if (current < currentEnd) calculateCurrentGroup()?.key ?: EMPTY else EMPTY

    /**
     * Get the node associated with the group if there is one.
     */
    val groupNode get() = (assumeGroup() as? NodeGroup)?.node

    /**
     * Get the key of the group at [index]. Will throw an exception if [index] is not a group
     * start.
     */
    fun groupKey(index: Int) = slots[index].asGroup.key

    /**
     * Return the location of the parent group of the [current]
     */
    val parentLocation: Int get() = startStack.peekOr(0)

    /**
     * Return the number of nodes where emitted into the current group.
     */
    val parentNodes: Int get() =
        if (startStack.isEmpty()) 0 else slots[startStack.peek()].asGroup.nodes

    /**
     * Return the number of slots are in the current group.
     */
    val parentSlots: Int get() =
        if (startStack.isEmpty()) 0 else slots[startStack.peek()].asGroup.slots

    /**
     * Get the value stored at [anchor].
     */
    @Suppress("KotlinOperator")
    fun get(anchor: Anchor) = if (anchor.loc >= 0) slots[anchor.loc] else EMPTY

    /**
     * Get the value stored at [index].
     */
    @Suppress("KotlinOperator")
    fun get(index: Int) = if (emptyCount > 0) EMPTY else slots[index]

    /**
     * Get the value of the slot at [current] or [EMPTY] if at then end of a group. During empty
     * mode this value is  always [EMPTY] which is the value a newly inserted slot.
     */
    fun next(): Any? {
        if (emptyCount > 0) return EMPTY
        currentGroup = null
        return if (current < currentEnd) slots[current++] else EMPTY
    }

    /**
     * Begin reporting empty for all calls to next() or get(). beginEmpty() can be nested and must
     * be called with a balanced number of endEmpty()
     */
    fun beginEmpty() { emptyCount++ }

    /**
     * End reporting [EMPTY] for calls to [next] and [get],
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
     * Start a group. Passing an EMPTY as the key will enter the group without validating the key.
     */
    fun startGroup(key: Any) = startGroup(key, GROUP)

    /**
     * Start a node.
     */
    fun startNode(key: Any) = startGroup(key, NODE)

    /**
     *  Skip a group. Must be called at the start of a group.
     */
    fun skipGroup(): Int {
        require(emptyCount == 0) { "Cannot skip while in an empty region" }
        val group = assumeGroup()
        current += group.slots + 1
        currentGroup = null
        val count = if (group.isNode) 1 else group.nodes
        nodeIndex += count
        return count
    }

    /**
     * Start a node.
     */
    fun skipNode() = skipGroup()

    /**
     * Skip to the end of the current group.
     */
    fun skipToGroupEnd() {
        require(emptyCount == 0) { "Cannot skip the enclosing group while in an empty region" }
        require(startStack.isNotEmpty()) { "No enclosing group to skip" }
        nodeIndex = slots[startStack.peek()].asGroup.nodes + nodeIndexStack.peek()
        currentGroup = null
        current = currentEnd
    }

    fun reposition(value: Int) {
        current = value
        currentGroup = null
    }
    /**
     * End the current group. Must be called after the corresponding [startGroup].
     */
    fun endGroup() {
        if (emptyCount == 0) {
            require(current == currentEnd)
            val startLocation = startStack.pop()
            if (startStack.isEmpty()) return
            val parentLocation = startStack.peekOr(0)
            val group = slots[startLocation].asGroup
            val parentGroup = slots[parentLocation].asGroup
            nodeIndex = nodeIndexStack.pop() + if (group.isNode) 1 else nodeIndex
            currentEnd = parentGroup.slots + parentLocation + 1
            currentGroup = null
        }
    }

    /**
     * End a node
     */
    fun endNode() = endGroup()

    /**
     * Extract the keys from this point to the end of the group. The current is left unaffected.
     * Must be called inside a group.
     */
    fun extractKeys(): MutableList<KeyInfo> {
        val result = mutableListOf<KeyInfo>()
        if (emptyCount > 0) return result
        val oldCurrent = current
        val oldNodeIndex = nodeIndex
        var index = 0
        while (current < currentEnd) {
            val location = current
            val group = slots[location].asGroup
            result.add(KeyInfo(group.key, location, skipGroup(), index++, group))
        }
        current = oldCurrent
        this.nodeIndex = oldNodeIndex
        return result
    }

    override fun toString(): String = "SlotReader(current=$current, emptyCount=$emptyCount)"

    private fun startGroup(key: Any, kind: GroupKind) {
        if (emptyCount <= 0) {
            startStack.push(current)
            nodeIndexStack.push(nodeIndex)
            nodeIndex = 0
            val group = assumeGroup()
            currentEnd = current + group.slots + 1
            require(group.kind == kind || key == EMPTY) { "Group kind changed" }
            require(key == EMPTY || key == group.key) { "Group key changed" }
            current++
            currentGroup = null
        }
    }

    private fun calculateCurrentGroup(): Group? =
        (currentGroup ?: slots[current] as? Group)?.also { currentGroup = it }
    private fun assumeGroup(): Group = calculateCurrentGroup()
        ?: error("Expected a group start")
}

@PublishedApi
internal val EMPTY = SlotTable.EMPTY

class SlotWriter internal constructor(val table: SlotTable) {
    var current = 0

    internal val slots get() = table.slots
    internal fun effectiveIndex(index: Int) = table.effectiveIndex(index)
    internal var currentEnd = table.slots.size

    private var startStack = IntStack()
    private val nodeCountStack = IntStack()
    private val endStack = IntStack()
    private var nodeCount = 0
    private var insertCount = 0
    private var pendingClear = false

    /**
     * Return true if the current slot starts a group
     */
    val isGroup get() = current < currentEnd && get(current) is Group

    /**
     * Return true if the slot at index starts a gorup
     */
    fun isGroup(index: Int) = get(index) is Group

    internal fun group(location: Int) = slots[effectiveIndex(location)].asGroup

    internal val parentGroup: Group get() = group(parentLocation)

    /**
     * Return true if the current slot starts a node. A node is a kind of group so this will
     * return true for isGroup as well.
     */
    val isNode get() = current < currentEnd && (get(current) as? Group)?.isNode ?: false

    /**
     * Return the number of nodes in the group. isGroup must be true or this will throw.
     */
    val groupSize get() = get(current).asGroup.slots

    /**
     * Return the size of the group at index. isGroup(index) must be true of this will throw.
     */
    fun groupSize(index: Int): Int = get(index).asGroup.slots

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
            else slots[effectiveIndex(startStack.peek())].asGroup.nodes
        }

    /**
     * Return the start location of the nearest group that contains [current].
     */
    val parentLocation: Int get() = startStack.peekOr(-1)

    /**
     * True if the writer has been closed
     */
    var closed = false
        private set

    /**
     * Return the start location of the nearest group that contains the slot at [anchor].
     */
    fun parentIndex(anchor: Anchor): Int {
        val group = get(anchor).asGroup
        val location = table.anchorLocation(anchor)
        val parent = group.parent
        if (parent != null) {
            // Scan the slot table for the parent slot.
            // The parent is, at most parent.slots - group.slots - 1 before location.
            val start = (location - (parent.slots - group.slots) - 1).let { if (it < 0) 0 else it }
            for (probe in start until location) {
                if (get(probe) === parent) return probe
            }
        }
        error("Could not find parent of group at $location")
    }

    /**
     * Get the value at an Anchor
     */
    @Suppress("KotlinOperator")
    fun get(anchor: Anchor) = if (anchor.loc >= 0) slots[anchor.loc] else SlotTable.EMPTY

    /**
     * Get the value at the index'th slot.
     */
    @Suppress("KotlinOperator")
    fun get(index: Int) = effectiveIndex(index).let {
        if (it < slots.size) slots[it] else SlotTable.EMPTY
    }

    fun close() {
        closed = true
        table.close(this)
        // Ensure, for readers, there is no gap
        moveGapTo(table.size)
    }

    /**
     * Set the value of the next slot.
     */
    fun update(value: Any?): Any? {
        val result = skip()
        set(value)
        return result
    }

    /**
     * Updates the data for a data group
     */
    fun updateData(value: Any?) {
        (get(current) as? DataGroup ?: error("Expected a data group")).data = value
    }

    /**
     * Set the value at the slot previous to current.
     */
    fun set(value: Any?) {
        slots[effectiveIndex(current - 1)] = value
    }

    /**
     * Skip the current slot without updating. If the slot table is inserting then and [EMPTY] slot
     * is added and [skip] return [EMPTY].
     */
    fun skip(): Any? {
        if (insertCount > 0) {
            insert(1)
        }
        val index = current++
        return slots[table.effectiveIndex(index)]
    }

    /**
     * Skip [amount] slots in the slot table. If the slot table is inserting then this
     * adds [amount] [EMPTY] slots.
     *
     * Skip cannot skip outside the current group.
     */
    fun skip(amount: Int) {
        if (insertCount > 0) {
            insert(amount)
        } else {
            val location = current + amount
            require(location <= currentEnd) {
                "Cannot skip outside the current group ($currentEnd)"
            }
            current = location
        }
    }

    /**
     * Skip to the end of the current group.
     */
    fun skipToGroupEnd() { current = currentEnd }

    /**
     * If the start of a group was skipped using [skip], calling [ensureStarted] puts the writer
     * into the same state as if [startGroup] or [startNode] was called on the group starting at
     * [location]. If, after starting, the group, [current] is not a the end of the group or
     * [current] is not at the start of a group for which [location] is not location the parent
     * group, an exception is thrown.
     *
     * Calling [ensureStarted] implies that an [endGroup] should be called once the end of the
     * group is reached.
     */
    fun ensureStarted(location: Int) {
        require(insertCount <= 0) { "Cannot call ensureStarted() while inserting" }
        require(location in 0 until current) { "$location is out of range 0..${current - 1}" }
        val parentLoc = parentLocation
        if (parentLoc != location) {
            if (startStack.isEmpty() && location > 0) ensureStarted(0)
            val currentParent = if (parentLoc >= 0) get(parentLocation).asGroup else null
            val newParent = get(location).asGroup

            // The new parent must be a (possibly indirect) child of the current parent
            require(newParent.isDecendentOf(currentParent)) {
                "Started group must be a subgroup of the group at $parentLocation"
            }

            val oldCurrent = current
            current = location
            startGroup(newParent.key, newParent.kind, newParent.data)
            current = oldCurrent
        }
    }

    fun ensureStarted(anchor: Anchor) = ensureStarted(anchor.location(table))

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
    fun startGroup(key: Any) = startGroup(key, GROUP, null)

    private fun startGroup(key: Any, kind: GroupKind, data: Any?) {
        val inserting = insertCount > 0
        val parent = if (startStack.isEmpty()) null else get(startStack.peek()).asGroup
        startStack.push(current)
        nodeCountStack.push(nodeCount)

        // Record the end location as relative to the end of the slot table so when we pop it back
        // off again all inserts and removes that happened while a child group was open are already
        // reflected into its value.
        endStack.push(slots.size - table.gapLen - currentEnd)
        currentEnd = if (inserting) {
            require(key != SlotTable.EMPTY) { "Inserting an EMPTY key" }
            update(Group(kind, key, parent, data))
            nodeCount = 0
            current
        } else {
            val group = advance().asGroup
            require(group.kind == kind) { "Group kind changed" }
            require(key == SlotTable.EMPTY || group.key == key) { "Group key changed" }
            if (kind == DATA) {
                (group as? DataGroup ?: error("Expected a data group")).data = data
            } else if (kind == NODE && data != null) {
                (group as? NodeGroup ?: error("Expected a node group")).node = data
            }
            nodeCount = group.nodes
            current + group.slots
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
    fun endGroup(): Int {
        require(startStack.isNotEmpty()) {
            "Invalid state. Unbalanced calls to startGroup() and endGroup()"
        }
        val inserting = insertCount > 0
        require(inserting || current == currentEnd) { "Expected to be at the end of a group" }

        // Update group length
        val startLocation = startStack.pop()
        val group = get(startLocation).asGroup
        val cur = current
        val oldSlots = group.slots
        val oldNodes = group.nodes
        val newSlots = cur - startLocation - 1
        val newNodes = nodeCount
        group.slots = newSlots
        group.nodes = newNodes
        currentEnd = (slots.size - table.gapLen) - endStack.pop()
        if (nodeCountStack.isEmpty()) {
            table.clearGap()
        } else if (startStack.isNotEmpty()) {
            nodeCount = nodeCountStack.pop()
            val parent = get(startStack.peek()).asGroup
            if (group.parent == parent) {
                nodeCount += if (inserting) {
                    if (group.isNode) 1 else newNodes
                } else {
                    if (group.isNode) 0 else newNodes - oldNodes
                }
            } else {
                // If we are closing a group whose parent is not the now current group then the
                // slot writer was seek'ed to the group and the parents of this group need to be
                // updated to reflect any changes to the groups nodes or slots.
                val slotsDelta = newSlots - oldSlots
                var nodesDelta = if (group.isNode) 0 else newNodes - oldNodes
                if (slotsDelta != 0 || nodesDelta != 0) {
                    var currentGroup = group.parent
                    while (
                        currentGroup != null &&
                        currentGroup != parent &&
                        (nodesDelta != 0 || slotsDelta != 0)
                    ) {
                        currentGroup.slots += slotsDelta
                        currentGroup.nodes += nodesDelta
                        if (currentGroup.isNode) nodesDelta = 0
                        currentGroup = currentGroup.parent
                    }
                }
                nodeCount += nodesDelta
            }
        }
        return newNodes
    }

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
    fun startNode(key: Any) = startGroup(key, NODE, null)

    /**
     * Start a node
     */
    fun startNode(key: Any, node: Any?) = startGroup(key, NODE, node)

    /**
     * End a node
     */
    fun endNode() = endGroup()

    /**
     * Start a data node.
     */
    fun startData(key: Any, data: Any?) = startGroup(key, DATA, data)

    /**
     * End a data node
     */
    fun endData() = endGroup()

    /**
     * Skip a node
     */
    fun skipNode() = skipGroup()

    /**
     * Move (insert and then delete) the group at [location] from [slots]. All anchors in the range
     * (including [location]) are moved to the slot table for which this is a reader.
     *
     * It is required that the writer be inserting.
     *
     * @return a list of the anchors that were moved
     */
    fun moveFrom(table: SlotTable, location: Int): List<Anchor> {
        require(insertCount > 0)

        if (location == 0 && current == 0 && this.table.size == 0) {
            // Special moving the entire slot table into an empty table, just swap the slots
            // and the anchors.
            table.write {
                val sourceSlots = table.slots
                val sourceAnchors = table.anchors
                val sourceGapStart = table.gapStart
                val sourceGapLen = table.gapLen
                val destTable = this.table
                val destSlots = destTable.slots
                val destAnchors = destTable.anchors
                destTable.slots = sourceSlots
                destTable.anchors = sourceAnchors
                destTable.gapStart = sourceGapStart
                destTable.gapLen = sourceGapLen
                table.slots = destSlots
                table.anchors = destAnchors
                table.gapStart = 0
                table.gapLen = 0
            }
            return this.table.anchors
        }

        return table.write { tableWriter ->
            val sourceStart = location
            val slotsToMove = tableWriter.groupSize(sourceStart) + 1

            // Make room in the table
            insert(slotsToMove)

            // Copy the slots to the
            val sourceSlots = table.slots
            val destSlots = slots
            val destStart = current
            val sourceEnd = sourceStart + slotsToMove
            // Move the gap to make the location contiguous. The remove at the end will do this
            // as well so doing this early makes this code easier as the gap can be ignored.
            tableWriter.moveGapTo(sourceEnd)
            sourceSlots.copyInto(destSlots, current, sourceStart, sourceEnd)

            val group = get(destStart).asGroup

            // Update the sizes of the parents of the group that was moved.
            var currentGroup = group.parent
            val slotsDelta = group.slots + 1
            var nodesDelta = if (group.isNode) 1 else group.nodes
            while (currentGroup != null) {
                currentGroup.slots -= slotsDelta
                currentGroup.nodes -= nodesDelta
                if (currentGroup.isNode) nodesDelta = 0
                currentGroup = currentGroup.parent
            }

            // Update the parent of the group moved.
            group.parent = get(startStack.peek()).asGroup

            // Extract the anchors in range
            val startAnchors = table.anchors.locationOf(sourceStart)
            val endAnchors = table.anchors.locationOf(sourceEnd)
            val anchors = if (startAnchors < endAnchors) {
                val sourceAnchors = table.anchors
                val anchors = ArrayList<Anchor>(endAnchors - startAnchors)

                // update the anchor locations to their new location
                for (index in startAnchors until endAnchors) {
                    val sourceAnchor = sourceAnchors[index]
                    sourceAnchor.loc = sourceAnchor.loc - sourceStart + destStart
                    anchors.add(sourceAnchor)
                }

                // Insert them into the new table
                val insertLocation = this.table.anchors.locationOf(current)
                this.table.anchors.addAll(insertLocation, anchors)

                // Remove them from the old table
                sourceAnchors.subList(startAnchors, endAnchors).clear()

                anchors
            } else emptyList<Anchor>()

            // Now remove the range from the table.
            val anchorsRemoved = tableWriter.remove(sourceStart, slotsToMove)
            require(!anchorsRemoved) { "Removing anchors that should have been moved" }

            // Update the node count.
            nodeCount += group.nodes

            // Move current passed the insert
            current += slotsToMove

            anchors
        }
    }

    /**
     * Allocate an anchor for a location. As content is inserted and removed from the slot table the
     * anchor is updated to reflect those changes. For example, if an anchor is requested for an
     * group, the anchor will report the location of that group even if the group is moved in the slot
     * table. If the position referenced by the anchor is removed, the anchor location is set to -1.
     */
    fun anchor(index: Int = current): Anchor = table.anchor(index)

    private fun advance(): Any? {
        if (current >= currentEnd) {
            return SlotTable.EMPTY
        }
        val index = current++
        return slots[effectiveIndex(index)]
    }

    private fun advanceToNextGroup(): Int {
        val groupStart = advance().asGroup
        current += groupStart.slots

        return if (groupStart.isNode) 1 else groupStart.nodes
    }

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
        val gap = if (table.gapLen > 0)
            "${table.gapStart}-${table.gapStart + table.gapLen - 1}"
        else
            "none"
        return "SlotWriter" +
                "(current=$current, " +
                "size=${slots.size - table.gapLen}, " +
                "gap=${gap}${if (insertCount > 0) ", inserting" else ""})"
    }
}

private fun Group.isDecendentOf(parent: Group?): Boolean {
    if (parent == null) return true
    var current = this.parent
    while (current != null) {
        if (current == parent) return true
        current = current.parent
    }
    return false
}

private val Any?.asGroup: Group
    get() = this as? Group ?: error("Expected a group")

internal fun Group(kind: GroupKind, key: Any, parent: Group?, data: Any?) =
    when (kind) {
        NODE -> NodeGroup(key, parent).also { it.node = data }
        DATA -> DataGroup(key, parent, data)
        else -> Group(key, parent)
    }

internal open class Group(
    val key: Any,
    var parent: Group?
) {
    var slots: Int = 0
    var nodes: Int = 0
    open val kind: GroupKind get() = GROUP
    open val isNode get() = false
    open val node: Any? get() = null
    open val data: Any? get() = null
}

internal class NodeGroup(
    key: Any,
    parent: Group?
) : Group(key, parent) {
    override val kind: GroupKind get() = NODE
    override val isNode get() = true
    override var node: Any? = null
}

internal class DataGroup(
    key: Any,
    parent: Group?,
    override var data: Any?
) : Group(key, parent) {
    override val kind: GroupKind get() = DATA
}

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
class SlotTable(internal var slots: Array<Any?> = arrayOf()) {
    private var readers = 0
    private var writer = false
    internal var gapStart: Int = 0
    internal var gapLen: Int = 0
    internal var anchors: ArrayList<Anchor> = arrayListOf()

    /**
     * Read the slot table in [block]. Any number of readers can be created but a slot table cannot
     * be read while it is being written to.
     *
     * @see SlotReader
     */
    fun <T> read(block: (reader: SlotReader) -> T): T = openReader().let { reader ->
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
    fun <T> write(block: (writer: SlotWriter) -> T): T = openWriter().let { writer ->
        try {
            block(writer)
        } finally {
            writer.close()
        }
    }

    /**
     * Return a list of locations of slot table that contain the groups that contain [location].
     *
     * [groupPathTo] creates a reader so it cannot be called when the slot table is being written
     * to.
     */
    fun groupPathTo(location: Int): List<Int> {
        require(location < size)
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

    /**
     * Open a reader. Any number of readers can be created but a slot table cannot be read while
     * it is being written to.
     *
     * @see SlotReader
     */
    fun openReader(): SlotReader {
        if (writer) error("Cannot read while a writer is pending")
        readers++
        return SlotReader(this)
    }

    /**
     * Open a writer. Only one writer can be created for a slot table at a time and all readers
     * must be closed an do readers can be created while the slot table is being written to.
     *
     * @see SlotWriter
     */
    fun openWriter(): SlotWriter {
        if (writer) error("Cannot start a writer when another writer is pending")
        if (readers > 0) error("Cannot start a writer when a reader is pending")
        writer = true
        return SlotWriter(this)
    }

    /**
     * Ensure a slot table is well-formed by verifying the internal structure of the slot table
     * is consistent. This method will throw an exception when it detects inconsistency in the
     * internal structure of the slot table. A slot table can be invalid (contain incorrect
     * information about a composition) but still be well-formed but all valid slot tables are
     * well-formed.
     */
    @TestOnly
    fun verifyWellFormed() {
        var current = 0

        fun validateGroup(parentLocation: Int, parent: Group?): Int {
            val location = current++
            val group = slots[location].asGroup
            require(group.parent == parent) { "Incorrect parent for group at $location" }
            val end = location + group.slots + 1
            val parentEnd = parentLocation + (parent?.slots?.let { it + 1 } ?: size)
            require(end <= size) { "Group extends past then end of its table at $location" }
            require(end <= parentEnd) { "Group extends past its parent at $location" }
            require(!group.isNode || group.node != null) {
                "Node groups must have a node at $location"
            }

            // Find the first child
            while (current < end && slots[current] !is Group) current++

            // Validate the child groups
            var nodeCount = 0
            while (current < end) {
                nodeCount += validateGroup(location, group)
            }
            require(group.nodes == nodeCount) {
                "Incorrect node count for group at $location, expected ${
                    group.nodes
                }, received $nodeCount"
            }
            return if (group.isNode) 1 else nodeCount
        }

        // Verify the groups are well-formed
        require(gapStart == size) { "Gap is not at the end of the table" }
        if (size > 0)
            validateGroup(0, null)

        // Verify the anchors are well-formed
        var lastLocation = -1
        for (anchor in anchors) {
            val location = anchor.location(this)
            require(location in 0..size) { "Location out of bound" }
            require(lastLocation < location) { "Anchor is out of order" }
            lastLocation = location
        }
    }

    /**
     * The number of active slots in the slot table. The current capacity of the slot table is at
     * lease [size].
     */
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
        val EMPTY = object : Any() {
            override fun toString(): String {
                return "EMPTY"
            }
        }
    }
}

private fun ArrayList<Anchor>.locationOf(index: Int) =
    search(index).let { if (it >= 0) it else -(it + 1) }
private fun ArrayList<Anchor>.search(index: Int) = binarySearch { it.loc.compareTo(index) }

/**
 * Information about groups and their keys.
 */
class KeyInfo internal constructor(
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
    val index: Int,

    /**
     * The group
     */
    internal val group: Group
)

class Anchor(internal var loc: Int) {
    val valid get() = loc >= 0
    fun location(slots: SlotTable) = slots.anchorLocation(this)
}

private typealias GroupKind = Int

private const val GROUP: GroupKind = 0
private const val NODE: GroupKind = 1
private const val DATA: GroupKind = 2

private const val MIN_GROWTH_SIZE = 128
