package com.google.r4a

/**
 * A buffer-gap editor implementation of a composition slot space. A slot space can be thought of as a custom List<Any?>
 * that optimizes around inserts and removes.
 *
 * Slots stores slots, groups, memos, items, and nodes.
 *
 *   Slot  - A slot is the primitive base type of the slot space. It is of type Any? and can hold any value.
 *   Group - A group is a group of slots. The group counts the number of slots and nodes it contains.
 *   Item  - An item is a key slot followed by an optional memo and then a group. The key is intended to identify the
 *           content of a group. Key values are opaque to the slots and are to be interpreted by the code that uses the
 *           slot space.
 *   Node  - A node is a special group that is counted by the containing groups.
 *   Memo  - A memo is a special group that follows a key and precedes the group of an item.
 *
 * All groups, memos, items, and nodes are just grouping of slots and use slots to describe the groups. At the root
 * of a slot space is a group. Groups count the number nodes that are in the group. An item is defined by a key and
 * an optional memo followed by a group or node. A node only counts as one node in its group regardless of the number of
 * nodes it contains.
 *
 * ASIDE:
 *  The intent is for items to represent memoized function calls and nodes represent views. For example,
 *
 *   <LinearLayout>
 *       <Contact contact={jim} />
 *       <Contact contact={bob} />
 *   </LinearLayout>
 *
 *  the <LinearLayout> tag here would be a node (the linear layout view). Its memo would be empty as there is no
 *  parameters. The node contains the items for the child views of the linear layout.
 *
 *  If contact's composition looks like:
 *
 *    fun Contact(contact: Contact) {
 *      <TextView text={contact.name} />
 *      <TextView text={contact.email} />
 *    }
 *
 *  then composing contact into the linear layout would add two views to the linear layout's children. The composition
 *  of contact creates an item which with the contact value as a memo and then contains two nodes, one for each text
 *  view. The items for each contact would be able to report that it produces two views (that is the group created
 *  for Contact has two nodes). Summing the nodes in the items group produces the number of views (as each node
 *  corresponds to a view).
 *
 *  If the order that jim and bob change above,
 *
 *   <LinearLayout>
 *       <Contact contact={bob} />
 *       <Contact contact={jim} />
 *   </LinearLayout>
 *
 *  the previous result can be reused by moving the views generated bob's item before jim's (or vis versa). A
 *  composition algorithm could use the key information for each item to determine if they can be switched. For example,
 *  since the first contact's group has two nodes the composition algorithm can infer that the beginning of jim's
 *  views starts at 2 and contains 2 view. To move jim in front of bob, move the 2 views from offset 2 to offset 0.
 *  The memo can then be used to determine if the function call's effect will the same as the previous result. If
 *  contact is immutable, for example, Contact would only need to be recomposed if the value of jim or bob change.
 *
 * The slot space can be in one of three sub-modes, read-only, inserting and empty. Normally a slot array can be
 * arbitrarily navigated and modified. If the slot array is in read-only mode, trying to update, insert, or remove slots
 * will throw.
 *
 *  ASIDE: This is intended as a debugging aid for composition which should only read the slot array, not update it.
 *
 * If in insert mode, calling next will insert an empty slot (and return EMPTY). When in empty mode, next() will return
 * EMPTY and will not advance the cursor.
 *
 *  ASIDE: The is intended to allow the same generated code to both to create and update views. Empty mode is used
 *  during composition, insert mode is used during apply.
 */
class SlotTable(private var slots: Array<Any?> = arrayOf()) {
    var current = 0
    internal val startStack = IntStack()
    private var gapStart = 0
    private var gapLen = 0
    private var insertCount = 0
    private var emptyCount = 0
    private var readingCount = 0
    private val groupKindStack = IntStack()
    private val nodeCountStack = IntStack()
    private var currentEnd =  slots.size
    private val endStack = IntStack()
    private var nodeCount = 0
    private var pendingClear = false
    private var uncertainCount = false

    private var anchors = ArrayList<Anchor>()

    /**
     * Get the value at the current slot
     */
    fun get() = if (emptyCount > 0) EMPTY else slots[effectiveIndex(current - 1)]
    fun get(anchor: Anchor) = if (anchor.loc >= 0) slots[anchor.loc] else EMPTY

    /**
     * Get the value at the index'th slot.
     */
    fun get(index: Int) = effectiveIndex(index).let { if (it < slots.size) slots[it] else EMPTY }

    /**
     * Set the value of the next slot.
     */
    fun update(value: Any?) {
        next()
        set(value)
    }

    /**
     * Set the value at the current slot.
     */
    fun set(value: Any?) {
        assert(readingCount <= 0) { "Cannot set or update while in reading mode" }
        assert(emptyCount <= 0) { "Cannot set or update in an empty region" }
        slots[effectiveIndex(current - 1)] = value
    }

    /**
     * Get the value of the next slot. If inserting this value is always EMPTY which is the value of the
     * newly created slot.
     */
    fun next(): Any? {
        if (emptyCount > 0) {
            return EMPTY
        }
        if (insertCount > 0) {
            insert(1)
        }
        if (current >= currentEnd) {
            return EMPTY
        }
        val index = current++
        return slots[effectiveIndex(index)]
    }

    /**
     * Backup one slot. For example, we ran into a key of a keyed group we don't want, this backs up current to be
     * before the key.
     */
    fun previous() {
        if (emptyCount <= 0) {
            assert(current > 0) { "Invalid call to previous" }
            current--
        }
    }

    /**
     * Begin inserting at the current location. beginInsert() can be nested and must be called with a balanced number
     * of endInsert()
     */
    fun beginInsert() {
        assert(readingCount == 0) { "Cannot insert while reading" }
        insertCount++
    }

    /**
     * Ends inserting.
     */
    fun endInsert() {
        assert(insertCount > 0) { "Unbalanced begin/end insert" }
        insertCount--
    }

    /**
     * Begin reporting empty for all calls to next() or get(). beginEmpty() can be nested and must be called with a
     * balanced number of endEmpty()
     */
    fun beginEmpty() {
        emptyCount++
    }

    val inEmpty get() = emptyCount > 0

    /**
     * End reporting empty for calls to net() and get().
     */
    fun endEmpty() {
        assert(emptyCount > 0) { "Unbalanced begin/end empty" }
        emptyCount--
    }

    /**
     * Throw if a modification of this slot space is attempted. beginReading can be nested and must be called with a
     * balanced number of endReading().
     */
    fun beginReading() {
        readingCount++
    }


    /**
     * Allow mutating the slot space.
     */
    fun endReading() {
        assert(readingCount > 0) { "Unbalanced begin/end reading" }
        readingCount--
    }

    val isReading get() = readingCount > 0

    /**
     * Reset current to the first slot.
     */
    fun reset() {
        assert(insertCount == 0) { "Cannot reset while inserting" }
        assert(startStack.isEmpty()) { "Cannot reset in an open group" }
        assert(emptyCount == 0) { "Cannot reset in an empty region" }
        assert(readingCount == 0) { "Cannot reset when in reading mode" }
        current = 0
        nodeCount = 0
        nodeCountStack.clear()
    }

    /**
     * Start a group
     */

    fun startGroup() {
        startGroup(GROUP)
    }

    private fun startGroup(kind: GroupKind) {
        if (emptyCount <= 0) {
            startStack.push(current)
            groupKindStack.push(kind)
            nodeCountStack.push(nodeCount)
            // Record the end location as relative to the end of the slot table so when we pop it back off again
            // all inserts and removes that happened while a child group was open are already reflected into its value.
            endStack.push(slots.size - gapLen - currentEnd)
            nodeCount = 0
            if (insertCount > 0) {
                next() // Skip a slot for the GroupStart added by endGroup.
                currentEnd = current
            } else {
                val groupStart = next().asGroupStart
                assert(groupStart.kind == kind) { "Group kind changed" }
                currentEnd = current + groupStart.slots
            }
        }
    }

    /**
     *  Skip a group. Must be called at the start of a group.
     */
    fun skipGroup(): Int {
        assert(insertCount == 0) { "Cannot skip while inserting" }
        assert(emptyCount == 0) { "Cannot skip while in an empty region" }
        val groupStart = next().asGroupStart
        current += groupStart.slots

        val count = if (groupStart.isNode) 1 else groupStart.nodes
        nodeCount += count

        return count
    }

    /**
     * Skip the to the end of the group.
     */
    fun skipEnclosingGroup(): Int {
        assert(insertCount == 0) { "Cannot skip the enclosing group while inserting" }
        assert(emptyCount == 0) { "Cannot skip the enclosing group while in an empty region" }
        assert(readingCount != 0) { "Can only skip the enclosing group in reading mode as the node counts are uncertain" }
        assert(startStack.isNotEmpty()) { "No enclosing group to skip" }
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
            var count = nodeCount
            assert(!startStack.isEmpty()) { "Invalid state. Unbalanced calls to startGroup() and endGroup()" }
            assert(insertCount > 0 || current == currentEnd) { "Expected to be at the end of a group" }

            // Update group length
            val startLocation = startStack.pop()
            val groupKind = groupKindStack.pop()
            val effectiveStartLocation = effectiveIndex(startLocation)
            assert(slots[effectiveStartLocation] === EMPTY || slots[effectiveStartLocation] is GroupStart) { "Invalid state. Start location stack doesn't refer to a start location" }
            val len = current - startLocation - 1
            if (readingCount > 0) {
                val start = slots[effectiveStartLocation].asGroupStart
                // A node count < 0 means that it was reported as uncertain while reading
                assert(start.slots == len && (nodeCount == start.nodes || uncertainCount)) { "Invalid endGroup call, expected ${start.slots} slots and ${start.nodes} nodes but received, $len slots and $nodeCount nodes" }
                count = start.nodes
            } else {
                slots[effectiveStartLocation] = GroupStart(groupKind, len, nodeCount)
            }
            nodeCount = nodeCountStack.pop() + if (groupKind == NODE) 1 else nodeCount
            currentEnd = (slots.size - gapLen) - endStack.pop()
            if (readingCount <= 0 && nodeCountStack.isEmpty()) clearGap()
            return count
        } else return 0
    }

    val isGroup get() = current < currentEnd && get(current) is GroupStart
    fun isGroup(index: Int) = get(index) is GroupStart
    val isNode get() = current < currentEnd && (get(current) as? GroupStart)?.isNode ?: false
    val isMemoGroup get() = current < currentEnd && (get(current) as? GroupStart)?.isMemo ?: false
    val groupSize get() = get(current).asGroupStart.slots
    fun groupSize(index: Int): Int = get(index).asGroupStart.slots
    val isGroupEnd get() = inEmpty || current == currentEnd
    val nodeIndex get() = nodeCount
    val parentNodes: Int get() {
        assert(readingCount > 0) { "Cannot read parenNodes count while updating"}
        return if (startStack.isEmpty()) 0 else  slots[effectiveIndex(startStack.peek())].asGroupStart.nodes
    }

    /**
     * Move the offset'th group after the current item to the current location. Must be called when a keyed group is
     * expected.
     */
    fun moveItem(offset: Int) {
        assert(readingCount == 0) { "Cannot move while reading" }
        assert(insertCount == 0) { "Cannot move an item while inserting" }
        assert(emptyCount == 0) { "Cannot move an item in an empty region" }
        val oldCurrent = current
        val oldNodeCount = nodeCount

        // Find the item to move
        var count = offset
        while (count > 0) {
            skipItem()
            count--
        }

        // Move the current one here by first inserting room for it then copying it over the spot then removing the
        // old slot.
        val moveLocation = current
        skipItem()
        val moveLen = current - moveLocation
        current = oldCurrent
        insert(moveLen)
        val newMoveLocation = moveLocation + moveLen // insert inserted moveLen slots which moved moveLocation
        current = oldCurrent
        nodeCount = oldNodeCount
        System.arraycopy(slots, effectiveIndex(newMoveLocation), slots, effectiveIndex(current), moveLen)
        remove(moveLocation + moveLen, moveLen)
    }

    /**
     * Remove an item. Must be called at the startGroup of an item.
     */
    fun removeItem() {
        assert(readingCount == 0) { "Cannot remove while reading" }
        assert(insertCount == 0) { "Cannot remove and item while inserting" }
        assert(emptyCount == 0) { "Cannot remove an item in an empty region" }
        val oldCurrent = current
        val count = skipItem()
        remove(oldCurrent, current - oldCurrent)
        current = oldCurrent
        nodeCount -= count
    }

    /**
     * Start a node.
     */
    fun startNode() {
        startGroup(NODE)
    }

    /**
     * End a node
     */
    fun endNode() = endGroup()

    /**
     * Skip a node
     */
    fun skipNode() = skipGroup()

    /**
     * Skip the current item
     */
    fun skipItem(): Int {
        assert(insertCount == 0) { "Cannot skip an item while inserting" }
        assert(emptyCount == 0) { "Cannot skip an item in an empty region" }
        skipItemHeader()
        return skipGroup()
    }

    // Skip the context key, content key and the memo of an item
    private fun skipItemHeader() {
        next()
        skipMemos()
    }

    fun reportUncertainNodeCount() {
        assert(readingCount > 0) { "Can only report an uncertain count while reading" }
        uncertainCount = true
    }

    /**
     * Start memo
     */
    fun startMemo() = startGroup(MEMO)

    /**
     * End a memo
     */
    fun endMemo() = endGroup()

    /**
     * Skip all memos
     */
    fun skipMemos() {
        assert(insertCount == 0 && emptyCount == 0) { "Cannot skip while inserting or in empty mode" }
        while (isMemoGroup) {
            skipMemo()
        }
    }

    /**
     * Skip one memo
     */
    fun skipMemo() {
        assert(insertCount == 0 && emptyCount == 0) { "Cannot skip while inserting or in empty mode" }
        if (isMemoGroup) skipGroup()
    }

    /**
     * Extract the keys from this point to the end of the group. The current is left unaffected.
     * Must be called inside a group at the beginning of an item
     */
    fun extractItemKeys(): MutableList<KeyInfo> {
        assert(insertCount == 0) { "Cannot extract keys while inserting" }
        val result = mutableListOf<KeyInfo>()
        if (emptyCount > 0) return result
        val oldCurrent = current
        val oldNodeCount = nodeCount
        var index = 0
        while (current < currentEnd) {
            val location = current
            val key = next()!!
            skipMemo()
            result.add(KeyInfo(key, location, skipGroup(), index++))
        }
        current = oldCurrent
        this.nodeCount = oldNodeCount
        return result
    }

    private fun effectiveIndex(index: Int) = if (index < gapStart) index else gapLen + index
    private val Any?.asGroupStart: GroupStart get() = this as? GroupStart ?: error("Expected a group start ")

    private fun moveGapTo(index: Int) {
        if (gapLen > 0 && gapStart != index) {
            pendingClear = false
            if (anchors.isNotEmpty()) updateAnchors(index)
            if (index < gapStart) {
                val len = gapStart - index
                System.arraycopy(slots, index, slots, index + gapLen, len)
            } else {
                val len = index - gapStart
                System.arraycopy(slots, gapStart + gapLen, slots, gapStart, len)
            }
            gapStart = index
            pendingClear = true
        } else {
            gapStart = index
        }
    }

    private fun insert(size: Int) {
        if (size > 0) {
            pendingClear = false
            moveGapTo(current)
            if (gapLen < size) {
                // Create a bigger gap
                val oldSize = slots.size
                // Double the size of the array, but at least MIN_GROWTH_SIZE and >= size
                val newGapLen = Math.max(Math.max(oldSize, size), MIN_GROWTH_SIZE - oldSize)
                val newSlots = arrayOfNulls<Any?>(oldSize + newGapLen)
                val oldGapEnd = gapStart + gapLen
                val newGapEnd = gapStart + newGapLen
                // Copy the old array into the new array
                System.arraycopy(slots, 0, newSlots, 0, gapStart)
                System.arraycopy(slots, oldGapEnd, newSlots, newGapEnd, oldSize - oldGapEnd)

                // Update the anchors
                if (anchors.isNotEmpty()) anchorGapResize(newGapLen - gapLen)

                // Update the gap and slots
                slots = newSlots
                gapLen = newGapLen
            }
            if (currentEnd >= gapStart) currentEnd += size
            gapStart += size
            gapLen -= size

            repeat(size) {
                slots[current + it] = EMPTY
            }
            pendingClear = true
        }
    }

    internal fun remove(start: Int, len: Int) {
        if (len > 0) {
            pendingClear = false
            if (gapLen == 0) {
                // If there is no current gap, just make the removed items the gap
                gapStart = start
                if (anchors.isNotEmpty()) removeAnchors(start + len, len)
                gapLen = len
            } else {
                // Move the gap to the startGroup + len location and set the gap startGroup to startGroup and gap len to len + gapLen
                val removeEnd = start + len
                moveGapTo(removeEnd)
                if (anchors.isNotEmpty()) removeAnchors(gapStart, len)
                gapStart = start
                gapLen += len
            }
            if (currentEnd >= gapStart) currentEnd -= len
            pendingClear = true
        }
    }

    private data class GroupStart(val kind: GroupKind, val slots: Int, val nodes: Int) {
        val isNode get() = kind == NODE
        val isMemo get() = kind == MEMO
    }

    /**
     * Allocate an anchor for a location. As content is inserted and removed from the slot table the anchor is updated
     * to reflect those changes. For example, if an anchor is requested for an item, the anchor will report the location
     * of that item even if the item is moved in the slot table. If the position referenced by the anchor is removed,
     * the anchor location is set to -1.
     */
    fun anchor(index: Int = current): Anchor {
        // TODO: Consider a buffer gap list of anchors if middle inserts and deletes are common
        val anchorIndex = effectiveIndex(index)
        val location = anchors.search(anchorIndex)
        return if (location < 0) {
            val anchor = Anchor(anchorIndex)
            anchors.add(-(location + 1), anchor)
            anchor
        } else anchors[location]
    }

    private fun ArrayList<Anchor>.search(index: Int) = binarySearch { it.loc.compareTo(index) }
    private fun ArrayList<Anchor>.locationOf(index: Int) = search(index).let { if (it >= 0) it else -(it + 1) }

    private fun updateAnchors(gapMovedTo: Int) {
        val from = gapStart + gapLen
        val to = if (gapMovedTo < gapStart) gapMovedTo else gapMovedTo + gapLen
        if (gapStart < gapMovedTo) {
            var index = anchors.locationOf(from)
            while (index < anchors.size) {
                val anchor = anchors[index]
                if (anchor.loc < to) {
                    anchor.loc += gapLen
                    index++
                }
                else break
            }
        } else {
            var index = anchors.locationOf(to)
            while (index < anchors.size) {
                val anchor = anchors[index]
                if (anchor.loc > from) {
                    anchor.loc -= gapLen
                    index++
                } else break
            }
        }
    }

    private fun anchorGapResize(delta: Int) {
        val start = anchors.locationOf(gapStart + gapLen)
        for (index in start until anchors.size)
            anchors[index].loc += delta
    }

    private fun removeAnchors(gapStart: Int, size: Int) {
        val removeStart = gapStart - size
        var index = anchors.locationOf(gapStart).let { if (it >= anchors.size) it - 1 else it }
        while (index >= 0) {
            val anchor = anchors[index]
            if (anchor.loc >= removeStart) {
                anchor.loc = -1
                anchors.removeAt(index)
                index--
            } else break
        }
    }

    internal fun anchorLocation(anchor: Anchor) = anchor.loc.let { if (it > gapStart) it - gapLen else it }

    override fun toString(): String {
        if (pendingClear) {
            pendingClear = false
            clearGap()
        }
        return "${javaClass.simpleName}(current=$current, size=${slots.size - gapLen}, gap=${ if (gapLen > 0) "$gapStart-${gapStart + gapLen - 1}" else "none" }${if (isReading) ", reading" else ""}${if (insertCount >0) ", inserting" else ""}${if (inEmpty) ", in empty" else ""})"
    }

    private fun clearGap() {
        repeat(gapLen) { i -> slots[gapStart + i] = null }
    }

    companion object {
        val EMPTY = object {}
    }
}

/**
 * Information about items and their keys.
 */
class KeyInfo(
    /**
     * The key used for an item.
     */
    val key: Any,
    /**
     * The location of the group start of the item.
     */
    val location: Int,

    /**
     * The number of nodes in the group. If the group is a node this is always 1.
     */
    val nodes: Int,

    /**
     * The index of the key info in the list returned by extractItemKeys
     */
    val index: Int
)

class Anchor(internal var loc: Int) {
    fun location(slots: SlotTable) = slots.anchorLocation(this)
}

private typealias GroupKind = Int

private const val GROUP: GroupKind = 0
private const val NODE: GroupKind = 1
private const val MEMO: GroupKind = 2

private const val MIN_GROWTH_SIZE = 128