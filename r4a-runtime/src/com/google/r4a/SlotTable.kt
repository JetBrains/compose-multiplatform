package com.google.r4a

import java.util.Stack

/**
 * A buffer-gap editor implementation of a composition slot space. A slot space can be thought of as a custom List<Any?>
 * that optimizes around inserts and removes.
 *
 * Slots stores slots, groups, memos, items, and containers.
 *
 *   Slot      - A slot is the primitive base type of the slot space. It is of type Any? and can hold any value.
 *   Group     - A group is a group of slots. The group counts the number of slots and containers it contains.
 *   Item      - An item is a key slot followed by an optional memo and then a group. The key is intended to identify
 *               the content of a group. Key values are opaque to the slots and are to be interpreted by the code that
 *               uses the slot space.
 *   Container - A container is a special group that is counted by the containing groups.
 *   Memo      - A memo is a special group that follows a key and precedes the group of an item.
 *
 * All groups, memos, items, and containers are just grouping of slots and use slots to describe the groups. At the root
 * of a slot space is a group. Groups count the number containers that are in the group. An item is defined by a key and
 * an optional memo followed by a group or container. A container only counts as one container in its group regardless
 * of the number of containers it contains.
 *
 * ASIDE:
 *  The intent is for items to represent memoized function calls and containers represent views. For example,
 *
 *   <LinearLayout>
 *       <Contact contact={jim} />
 *       <Contact contact={bob} />
 *   </LinearLayout>
 *
 *  the <LinearLayout> tag here would be a container (the linear layout view). Its memo would be empty as there is no
 *  parameters. The container contains the items for the child views of the linear layout.
 *
 *  If contact's composition looks like:
 *
 *    fun Contact(contact: Contact) {
 *      <TextView text={contact.name} />
 *      <TextView text={contact.email} />
 *    }
 *
 *  then composing contact into the linear layout would add two views to the linear layout's children. The composition
 *  of contact creates an item which with the contact value as a memo and then contains two containers, one for each
 *  text view. The items for each contact would be able to report that it produces two views (that is the group created
 *  for Contact has two containers). Summing the containers in the items of a container produces the number of views (as
 *  each container corresponds to a view).
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
 *  since the first contact's group has two container the composition algorithm can infer that the beginning of jim's
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
class SlotTable(private var slots: Array<Any?> = arrayOf(GroupStart(GROUP, 0, 0), GROUP_END)) {
    var current = 0
    private var gapStart = 0
    private var gapLen = 0
    private var insertCount = 0
    private var emptyCount = 0
    private var readingCount = 0
    private val startStack = Stack<Int>()
    private val groupKindStack = Stack<GroupKind>()
    private val containerCountStack = Stack<Int>()
    private var containerCount = 0

    /**
     * Get the value at the current slot
     */
    fun get() = if (emptyCount > 0) EMPTY else slots[effectiveIndex(current - 1)]

    /**
     * Get the value at the index'th slot.
     */
    fun get(index: Int) = slots[effectiveIndex(index)]

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
        current++
        return get(current - 1)
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
        containerCount = 0
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
            containerCountStack.push(containerCount)
            containerCount = 0
            if (insertCount > 0) {
                next() // Skip a slot for the GroupStart added by endGroup.
            } else {
                val groupStart = next().asGroupStart
                assert(groupStart.kind == kind) { "Group kind changed" }
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
        expectGroupEnd()

        val count = if (groupStart.isContainer) 1 else groupStart.containers
        containerCount += count
        return count
    }

    private fun expectGroupEnd() {
        val groupEnd = next()
        assert(groupEnd === GROUP_END) { "Expected a group end marker" }
    }

    /**
     * End the current group. Must be called after the corresponding startGroup().
     */
    fun endGroup(): Int {
        val count = containerCount
        if (emptyCount <= 0) {
            assert(!startStack.empty()) { "Invalid state. Unbalanced calls to startGroup() and endGroup()" }
            val marker = next()
            if (marker === EMPTY) set(GROUP_END)
            else assert(marker === GROUP_END) { "Expected a group end marker" }

            // Update group length
            val startLocation = startStack.pop()
            val groupKind = groupKindStack.pop()
            val effectiveStartLocation = effectiveIndex(startLocation)
            assert(slots[effectiveStartLocation] === EMPTY || slots[effectiveStartLocation] is GroupStart) { "Invalid state. Start location stack doesn't refer to a start location" }
            val len = current - startLocation - 2
            if (readingCount > 0) {
                val start = slots[effectiveStartLocation].asGroupStart
                // A container count < 0 means that it was reported as uncertain while reading
                assert(start.slots == len && (start.containers == containerCount || containerCount < 0)) { "Invalid endGroup call, expected ${start.slots} slots and ${start.containers} containers but received, $len slots and $containerCount containers" }
            } else {
                slots[effectiveStartLocation] = GroupStart(groupKind, len, containerCount)
            }
            containerCount = containerCountStack.pop() + if (groupKind == CONTAINER) 1 else containerCount
            if (readingCount <= 0 && containerCountStack.isEmpty()) clearGap()
        }
        return count
    }

    val isGroup get() = get(current) is GroupStart
    val isContainer get() = (get(current) as? GroupStart)?.isContainer ?: false
    val isMemoGroup get() = (get(current) as? GroupStart)?.isMemo ?: false
    val groupSize get() = get(current).asGroupStart.slots
    val isGroupEnd get() = get(current) === GROUP_END

    /**
     * Move the offset'th group after the current item to the current location. Must be called when a keyed group is
     * expected.
     */
    fun moveItem(offset: Int) {
        assert(readingCount == 0) { "Cannot move while reading" }
        assert(insertCount == 0) { "Cannot move an item while inserting" }
        assert(emptyCount == 0) { "Cannot move an item in an empty region" }
        val oldCurrent = current

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
        containerCount -= count
    }

    /**
     * Start a container. A container resets the element count stack.
     */
    fun startContainer() {
        startGroup(CONTAINER)
    }

    /**
     * End a container
     */
    fun endContainer() = endGroup()

    /**
     * Skip a container
     */
    fun skipContainer() = skipGroup()

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

    fun reportUncertainContainerCount() {
        assert(readingCount > 0) { "Can only report an uncertain count while reading" }
        containerCount = UNCERTAIN
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
     * Extract the keys from this point to the end of the container. The current is left unaffected.
     * Must be called inside a container at the beginning of an item (or element)
     */
    fun extractItemKeys(): MutableList<KeyInfo> {
        assert(insertCount == 0) { "Cannot extract keys while inserting" }
        val result = mutableListOf<KeyInfo>()
        if (emptyCount > 0) return result
        val oldCurrent = current
        while (true) {
            val location = current
            val key = next()
            if (key !== GROUP_END && key !== EMPTY) {
                skipMemo()
                result.add(KeyInfo(key, location, skipGroup()))
            } else {
                previous()
                break
            }
        }
        current = oldCurrent
        return result
    }

    private fun effectiveIndex(index: Int) = if (index < gapStart) index else gapLen + index
    private val Any?.asGroupStart: GroupStart get() = this as? GroupStart ?: error("Expected a group start ")

    private fun moveGapTo(index: Int) {
        if (gapLen > 0 && gapStart != index) {
            if (index < gapStart) {
                val len = gapStart - index
                System.arraycopy(slots, index, slots, index + gapLen, len)
            } else {
                val len = index - gapStart
                System.arraycopy(slots, gapStart + gapLen, slots, gapStart, len)
            }
            gapStart = index
        } else {
            gapStart = index
        }
    }

    private fun insert(size: Int) {
        if (size > 0) {
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
                // Update the gap and slots
                slots = newSlots
                gapLen = newGapLen
            }
            gapStart += size
            gapLen -= size

            repeat(size) {
                slots[current + it] = EMPTY
            }
        }
    }

    internal fun remove(start: Int, len: Int) {
        if (len > 0) {
            if (gapLen == 0) {
                // If there is no current gap, just make the removed items the gap
                gapStart = start
                gapLen = len
            } else {
                // Move the gap to the startGroup + len location and set the gap startGroup to startGroup and gap len to len + gapLen
                val removeEnd = start + len
                moveGapTo(removeEnd)
                gapStart = start
                gapLen += len
            }
        }
    }

    private data class GroupStart(val kind: GroupKind, val slots: Int, val containers: Int) {
        val isContainer get() = kind == CONTAINER
        val isMemo get() = kind == MEMO
    }

    override fun toString(): String {
        clearGap() // Clearing the gap makes debugging easier
        return "${javaClass.simpleName}(current=${current}, size=${slots.size - gapLen}, gap=${ if (gapLen > 0) "${gapStart}-${gapStart + gapLen - 1}" else "none" }${if (isReading) ", reading" else ""}${if (insertCount >0) ", inserting" else ""}${if (inEmpty) ", in empty" else ""})"
    }

    private fun clearGap() {
        repeat(gapLen) { i -> slots[gapStart + i] = null }
    }

    companion object {
        val EMPTY = object {}
        private val GROUP_END = object {}
    }
}

/**
 * Information about items and their keys.
 */
data class KeyInfo(
    /**
     * The key used for an item.
     */
    val key: Any?,
    /**
     * The location of the group start of the item.
     */
    val location: Int,
    /**
     * The number of containers in the group. If the group is a container this is always 1.
     */
    val containers: Int
)

private typealias GroupKind = Int

private const val GROUP: GroupKind = 0
private const val CONTAINER: GroupKind = 1
private const val MEMO: GroupKind = 2

private const val UNCERTAIN = -(Int.MAX_VALUE / 2)
private const val MIN_GROWTH_SIZE = 128