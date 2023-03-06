package com.example.common

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Test {

    @Test
    fun testEmptyPlainTextNode() {
        val root = composeText {}
        assertEquals("root:{}", root.dump())
    }

    @Test
    fun testPlainTextNode() {
        val root = composeText {
            TextLeafNode("Hello World!")
        }
        assertEquals("root:{Hello World!}", root.dump())
    }

    @Test
    fun testTextContainerNodeEmpty() {
        val root = composeText {
            TextContainerNode("abc") {}
        }
        assertEquals("root:{abc:{}}", root.dump())
    }
    @Test
    fun testTextContainerNode() {
        val root = composeText {
            TextContainerNode("abc") {
                TextLeafNode("Hello World!")
            }
        }
        assertEquals("root:{abc:{Hello World!}}", root.dump())
    }

    @Test
    fun testRecomposition() = runTest {
        val index = mutableStateOf(1)

        val job = Job()
        val root = composeText(coroutineContext + job) {
            TextContainerNode("abc${index.value}") {
                TextLeafNode("Hello World!")
            }
        }

        assertEquals("root:{abc1:{Hello World!}}", root.dump())

        index.value = 2
        testScheduler.advanceUntilIdle()
        assertEquals("root:{abc2:{Hello World!}}", root.dump())

        index.value = 3
        testScheduler.advanceUntilIdle()
        assertEquals("root:{abc3:{Hello World!}}", root.dump())

        job.cancel()
    }
}
