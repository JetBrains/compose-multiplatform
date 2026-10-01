/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.dom

/**
 * Request-local attribute storage. Small HTML attribute sets need neither hashing nor one
 * linked-map entry object per attribute. Fall back to a linked map above 16 distinct names
 * to keep arbitrary user-provided attribute sets from becoming quadratic.
 */
internal class SsrAttributeMap : AbstractMutableMap<String, String>() {
    private val names = ArrayList<String>(8)
    private val storedValues = ArrayList<String>(8)
    private var large: MutableMap<String, String>? = null

    override val size: Int get() = large?.size ?: names.size
    override fun containsKey(key: String): Boolean = large?.containsKey(key) ?: names.contains(key)
    override fun get(key: String): String? {
        large?.let { return it[key] }
        val index = names.indexOf(key)
        return if (index < 0) null else storedValues[index]
    }

    override fun put(key: String, value: String): String? {
        large?.let { return it.put(key, value) }
        val index = names.indexOf(key)
        if (index >= 0) return storedValues.set(index, value)
        if (names.size == 16) {
            val map = linkedMapOf<String, String>()
            for (i in names.indices) map[names[i]] = storedValues[i]
            map[key] = value
            names.clear()
            storedValues.clear()
            large = map
        } else {
            names.add(key)
            storedValues.add(value)
        }
        return null
    }

    override fun remove(key: String): String? {
        large?.let { return it.remove(key) }
        val index = names.indexOf(key)
        if (index < 0) return null
        names.removeAt(index)
        return storedValues.removeAt(index)
    }

    override fun clear() {
        names.clear()
        storedValues.clear()
        large?.clear()
    }

    inline fun forEachAttribute(action: (String, String) -> Unit) {
        val map = large
        if (map != null) {
            for ((name, value) in map) action(name, value)
        } else {
            for (i in names.indices) action(names[i], storedValues[i])
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<String, String>>() {
            override val size: Int get() = this@SsrAttributeMap.size
            override fun add(element: MutableMap.MutableEntry<String, String>): Boolean =
                throw UnsupportedOperationException()
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> =
                large?.entries?.iterator() ?: object : MutableIterator<MutableMap.MutableEntry<String, String>> {
                    private var nextIndex = 0
                    private var canRemove = false
                    override fun hasNext(): Boolean = nextIndex < names.size
                    override fun next(): MutableMap.MutableEntry<String, String> {
                        if (!hasNext()) throw NoSuchElementException()
                        val name = names[nextIndex++]
                        canRemove = true
                        return object : MutableMap.MutableEntry<String, String> {
                            override val key: String = name
                            override val value: String get() = getValue(name)
                            override fun setValue(newValue: String): String = put(name, newValue)!!
                            override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> &&
                                key == other.key && value == other.value
                            override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                        }
                    }
                    override fun remove() {
                        check(canRemove)
                        names.removeAt(--nextIndex)
                        storedValues.removeAt(nextIndex)
                        canRemove = false
                    }
                }
        }
}

internal inline fun Map<String, String>.forEachHtmlAttribute(action: (String, String) -> Unit) {
    if (this is SsrAttributeMap) forEachAttribute(action)
    else for ((name, value) in this) action(name, value)
}
