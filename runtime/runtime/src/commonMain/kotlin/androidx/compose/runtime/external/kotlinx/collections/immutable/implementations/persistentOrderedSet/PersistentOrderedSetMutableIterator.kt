/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedSet

internal class PersistentOrderedSetMutableIterator<E>(private val builder: PersistentOrderedSetBuilder<E>)
    : PersistentOrderedSetIterator<E>(builder.firstElement, builder.hashMapBuilder), MutableIterator<E> {

    private var lastIteratedElement: E? = null
    private var nextWasInvoked = false
    private var expectedModCount = builder.hashMapBuilder.modCount

    override fun next(): E {
        checkForComodification()
        val next = super.next()
        lastIteratedElement = next
        nextWasInvoked = true
        return next
    }

    override fun remove() {
        checkNextWasInvoked()
        builder.remove(lastIteratedElement)
        lastIteratedElement = null
        nextWasInvoked = false
        expectedModCount = builder.hashMapBuilder.modCount
        index--
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.hashMapBuilder.modCount != expectedModCount)
            throw ConcurrentModificationException()
    }
}
