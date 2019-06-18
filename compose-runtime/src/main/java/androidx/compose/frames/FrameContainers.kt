package androidx.compose.frames

class ModelList<T> : MutableList<T>, Framed {
    private var myFirst: Record =
        ArrayContainer<T>()
    override val firstFrameRecord: Record get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    @Suppress("UNCHECKED_CAST") private val readable: ArrayContainer<T>
        get() =
        _readable(
            myFirst,
            this
        ) as ArrayContainer<T>
    @Suppress("UNCHECKED_CAST") private val writable: ArrayContainer<T>
        get() =
        _writable(
            myFirst,
            this
        ) as ArrayContainer<T>

    fun asMutable(): MutableList<T> = writable.list

    override val size: Int get() = readable.list.size
    override fun add(element: T): Boolean = writable.list.add(element)
    override fun add(index: Int, element: T) = writable.list.add(index, element)
    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        writable.list.addAll(index, elements)
    override fun addAll(elements: Collection<T>): Boolean = writable.list.addAll(elements)
    override fun clear() = writable.list.clear()
    override fun contains(element: T): Boolean = readable.list.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = readable.list.containsAll(elements)
    override fun get(index: Int): T = readable.list.get(index)
    override fun indexOf(element: T): Int = readable.list.indexOf(element)
    override fun isEmpty(): Boolean = readable.list.isEmpty()
    override fun iterator(): MutableIterator<T> =
        immutableIterator(readable.list.iterator())
    override fun lastIndexOf(element: T): Int = readable.list.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> =
        immutableListIterator(readable.list.listIterator())
    override fun listIterator(index: Int): MutableListIterator<T> =
        immutableListIterator(readable.list.listIterator(index))
    override fun remove(element: T): Boolean = writable.list.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = writable.list.removeAll(elements)
    override fun removeAt(index: Int): T = writable.list.removeAt(index)
    override fun retainAll(elements: Collection<T>): Boolean = writable.list.retainAll(elements)
    override fun set(index: Int, element: T): T = writable.list.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        writable.list.subList(fromIndex, toIndex)

    private class ArrayContainer<T> : AbstractRecord() {
        @JvmField var list: ArrayList<T> = arrayListOf<T>()

        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            (value as? ArrayContainer<T>)?.let {
                this.list = it.list.clone() as ArrayList<T>
            }
        }

        override fun create() = ArrayContainer<T>()
    }
}

fun <T> modelListOf() = ModelList<T>()
fun <T> modelListOf(element: T) = ModelList<T>().apply { add(element) }
fun <T> modelListOf(vararg elements: T) = ModelList<T>().apply { addAll(elements) }

class ModelMap<K, V> : MutableMap<K, V>, Framed {
    private var myFirst: Record =
        MapContainer<K, V>()
    override val firstFrameRecord: Record get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    @Suppress("UNCHECKED_CAST")
    private val readable: MapContainer<K, V>
        get() = _readable(
            myFirst,
            this
        ) as MapContainer<K, V>
    @Suppress("UNCHECKED_CAST")
    private val writable: MapContainer<K, V>
        get() = _writable(
            myFirst,
            this
        ) as MapContainer<K, V>

    fun asMutable(): MutableMap<K, V> = writable.map

    override val size: Int get() = readable.map.size
    override fun containsKey(key: K): Boolean = readable.map.containsKey(key)
    override fun containsValue(value: V): Boolean = readable.map.containsValue(value)
    override fun get(key: K): V? = readable.map.get(key)
    override fun isEmpty(): Boolean = readable.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = immutableSet(readable.map.entries)
    override val keys: MutableSet<K>
        get() = immutableSet(readable.map.keys)
    override val values: MutableCollection<V>
        get() = immutableCollection(readable.map.values)

    override fun clear() = writable.map.clear()

    override fun put(key: K, value: V): V? = writable.map.put(key, value)
    override fun putAll(from: Map<out K, V>) = writable.map.putAll(from)
    override fun remove(key: K): V? = writable.map.remove(key)

    private class MapContainer<K, V> : AbstractRecord() {
        @JvmField var map = mutableMapOf<K, V>()

        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            (value as? MapContainer<K, V>)?.let {
                this.map = LinkedHashMap<K, V>(it.map)
            }
        }

        override fun create() = MapContainer<K, V>()
    }
}

fun <K, V> modelMapOf() = ModelMap<K, V>()
fun <K, V> modelMapOf(vararg pairs: Pair<K, V>) = ModelMap<K, V>().apply { putAll(pairs) }

private fun error(): Nothing =
    error("Model sub-collection, iterators, lists and sets are immutable, use asMutable() first")

private fun <T> immutableSet(set: MutableSet<T>): MutableSet<T> = object : MutableSet<T> by set {
    override fun add(element: T): Boolean = error()
    override fun addAll(elements: Collection<T>): Boolean = error()
    override fun clear() = error()
    override fun iterator(): MutableIterator<T> =
        immutableIterator(set.iterator())
    override fun remove(element: T): Boolean = error()
    override fun removeAll(elements: Collection<T>): Boolean = error()
    override fun retainAll(elements: Collection<T>): Boolean = error()
}

private fun <T> immutableIterator(
    iterator: MutableIterator<T>
) = object : MutableIterator<T> by iterator {
    override fun remove() = error()
}

private fun <T> immutableListIterator(
    iterator: MutableListIterator<T>
) = object : MutableListIterator<T> by iterator {
    override fun add(element: T) = error()
    override fun remove() = error()
    override fun set(element: T) = error()
}

private fun <T> immutableCollection(
    collection: MutableCollection<T>
) = object : MutableCollection<T> by collection {
    override fun add(element: T): Boolean = error()
    override fun addAll(elements: Collection<T>): Boolean = error()
    override fun clear() = error()
    override fun iterator(): MutableIterator<T> =
        immutableIterator(collection.iterator())
    override fun remove(element: T): Boolean = error()
    override fun removeAll(elements: Collection<T>): Boolean = error()
    override fun retainAll(elements: Collection<T>): Boolean = error()
}
