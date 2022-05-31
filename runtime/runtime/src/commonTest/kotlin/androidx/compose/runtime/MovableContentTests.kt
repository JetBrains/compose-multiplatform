/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.mock.view
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Stable
class MovableContentTests {

    @Test
    fun testMovableContentSharesState() = compositionTest {
        var lastPrivateState: State<Int> = mutableStateOf(0)
        var portrait by mutableStateOf(false)

        val content = movableContentOf {
            val privateState = remember { mutableStateOf(0) }
            lastPrivateState = privateState
            Text("Some text")
            Text("Some other text")
        }

        @Composable
        fun Test() {
            if (portrait) {
                Column {
                    content()
                }
            } else {
                Row {
                    content()
                }
            }
        }

        compose {
            Test()
        }

        validate {
            fun MockViewValidator.value() {
                Text("Some text")
                Text("Some other text")
            }

            if (portrait) {
                Column {
                    this.value()
                }
            } else {
                Row {
                    this.value()
                }
            }
        }

        val firstPrivateState = lastPrivateState
        portrait = true
        Snapshot.sendApplyNotifications()

        expectChanges()
        revalidate()

        assertSame(firstPrivateState, lastPrivateState, "The state should be shared")
    }

    @Test
    fun movableContentPreservesNodes() = compositionTest {
        var portrait by mutableStateOf(false)

        val content = movableContentOf {
            Text("Some text")
            Text("Some other text")
        }

        @Composable
        fun Test() {
            if (portrait) {
                Column {
                    content()
                }
            } else {
                Row {
                    content()
                }
            }
        }

        compose {
            Test()
        }

        fun MockViewValidator.value() {
            Text("Some text")
            Text("Some other text")
        }

        validate {
            if (portrait) {
                Column {
                    this.value()
                }
            } else {
                Row {
                    this.value()
                }
            }
        }

        val firstText = root.findFirst { it.name == "Text" }
        portrait = true

        expectChanges()
        revalidate()

        // Nodes should be shared
        val newFirstText = root.findFirst { it.name == "Text" }
        assertSame(firstText, newFirstText, "Text instance should be identical")
    }

    @Test
    fun movingContent_mainComposer() = compositionTest {
        val rememberedObject = mutableListOf<RememberedObject>()

        @Composable
        fun addRememberedObject() {
            remember {
                RememberedObject().also { rememberedObject.add(it) }
            }
        }

        val content = movableContentOf {
            Row {
                addRememberedObject()
                Text("Some text")
                Marker()
            }
        }

        fun MockViewValidator.validateContent() {
            Row {
                Text("Some text")
                Marker()
            }
        }

        var first by mutableStateOf(true)

        compose {
            Row {
                if (first) content()
                Text("Some other text")
            }
            Row {
                Text("Some more text")
                if (!first) content()
            }
        }

        val marker: View = root.findFirst { it.name == "Marker" }

        fun validate() {
            validate {
                Row {
                    if (first) validateContent()
                    Text("Some other text")
                }
                Row {
                    Text("Some more text")
                    if (!first) validateContent()
                }
            }

            assertEquals(
                expected = marker,
                actual = root.findFirst { it.name == "Marker" },
                message = "Expected marker node to move with the movable content"
            )
            assertTrue("Expected all remember observers to be kept alive") {
                rememberedObject.all { it.isLive }
            }
        }

        validate()

        first = false
        expectChanges()
        validate()

        first = true
        expectChanges()
        validate()
    }

    @Test
    fun moveContent_subcompose() = compositionTest {
        val rememberObservers = mutableListOf<RememberedObject>()

        @Composable
        fun addRememberObject() {
            remember {
                RememberedObject().also { rememberObservers.add(it) }
            }
        }

        val content = movableContentOf {
            Row {
                addRememberObject()
                Text("Text from value")
                Marker()
            }
        }

        fun MockViewValidator.validateContent() {
            Row {
                Text("Text from value")
                Marker()
            }
        }

        val inMain = 0
        val inSubcompose1 = 1
        val inSubcompose2 = 2

        var position by mutableStateOf(inMain)

        compose {
            Row {
                if (position == inMain) content()
                Subcompose {
                    Row {
                        if (position == inSubcompose1) content()
                        Text("Some other text")
                    }
                }
                Subcompose {
                    Row {
                        Text("Some more text")
                        if (position == inSubcompose2) content()
                    }
                }
            }
        }

        val marker: View = root.findFirst { it.name == "Marker" }

        fun validate() {
            validate {
                Row {
                    if (position == inMain) validateContent()
                    Subcompose {
                        Row {
                            if (position == inSubcompose1) validateContent()
                            Text("Some other text")
                        }
                    }
                    Subcompose {
                        Row {
                            Text("Some more text")
                            if (position == inSubcompose2) validateContent()
                        }
                    }
                }
            }

            assertEquals(
                expected = marker,
                actual = root.findFirst { it.name == "Marker" },
                message = "Expected marker node to move with the movable content"
            )
            assertTrue("Expected all remember observers to be kept alive") {
                rememberObservers.all { it.isLive }
            }
        }

        validate()

        for (newPosition in listOf(
            inSubcompose1,
            inSubcompose2,
            inSubcompose1,
            inMain,
            inSubcompose2,
            inMain
        )) {
            position = newPosition
            expectChanges()
            validate()
        }
    }

    @Test
    fun normalMoveWithContentMove() = compositionTest {
        val random = Random(1337)
        val list = mutableStateListOf(
            *List(10) { it }.toTypedArray()
        )

        val content = movableContentOf { Marker() }
        var position by mutableStateOf(-1)

        compose {
            Column {
                if (position == -1) content()
                for (item in list) {
                    key(item) {
                        Text("Item $item")
                        if (item == position) content()
                    }
                }
            }
        }

        val marker: View = root.findFirst { it.name == "Marker" }

        fun validate() {
            validate {
                Column {
                    if (position == -1) Marker()
                    for (item in list) {
                        Text("Item $item")
                        if (item == position) Marker()
                    }
                }
            }

            assertEquals(
                expected = marker,
                actual = root.findFirst { it.name == "Marker" },
                message = "Expected marker node to move with the movable content"
            )
        }

        validate()

        repeat(10) {
            position = it
            list.shuffle(random)
            expectChanges()
            validate()
        }

        position = -1
        list.shuffle(random)
        expectChanges()
        validate()
    }

    @Test
    fun removeAndInsertWithMoveAway() = compositionTest {
        var position by mutableStateOf(0)
        var skipItem by mutableStateOf(5)

        val content = movableContentOf { Marker() }
        compose {
            Row {
                if (position == -1) content()
                Column {
                    repeat(10) { item ->
                        key(item) {
                            if (skipItem != item)
                                Text("Item $item")
                            if (position == item)
                                content()
                        }
                    }
                }
            }
        }

        val marker: View = root.findFirst { it.name == "Marker" }

        fun validate() {
            validate {
                Row {
                    if (position == -1) Marker()
                    Column {
                        repeat(10) { item ->
                            if (skipItem != item)
                                Text("Item $item")
                            if (position == item)
                                Marker()
                        }
                    }
                }
            }
            assertEquals(
                expected = marker,
                actual = root.findFirst { it.name == "Marker" },
                message = "Expected marker node to move with the movable content"
            )
        }

        validate()

        repeat(10) { markerPosition ->
            repeat(10) { skip ->
                position = -1
                skipItem = -1
                expectChanges()
                validate()

                // Move the marker and delete an item.
                position = markerPosition
                skipItem = skip
                expectChanges()
                validate()

                // Move the marker away and insert an item
                position = -1
                skipItem = -1
                expectChanges()

                // Move the marker back
                position = markerPosition
                expectChanges()
                validate()

                // Move the marker way and delete an item
                position = -1
                skipItem = skip
                expectChanges()
                validate()
            }
        }
    }

    @Test
    fun invalidationsMoveWithContent() = compositionTest {
        var data by mutableStateOf(0)
        var position by mutableStateOf(-1)
        val content = movableContentOf {
            Text("data = $data")
        }

        compose {
            Row {
                if (position == -1) content()
                repeat(10) { item ->
                    key(item) {
                        Text("Item $item")
                        if (position == item) content()
                    }
                }
            }
        }

        validate {
            fun MockViewValidator.content() {
                Text("data = $data")
            }
            Row {
                if (position == -1) this.content()
                repeat(10) { item ->
                    Text("Item $item")
                    if (position == item) this.content()
                }
            }
        }

        repeat(10) { newData ->
            data = newData
            position = newData
            expectChanges()
            revalidate()
        }
    }

    @Test
    fun projectedBinaryTree() = compositionTest {
        class Node(value: Int, left: Node? = null, right: Node? = null) {
            var value by mutableStateOf(value)
            var left by mutableStateOf(left)
            var right by mutableStateOf(right)

            fun validateNode(validator: MockViewValidator) {
                with(validator) {
                    Marker(value)
                }
                left?.validateNode(validator)
                right?.validateNode(validator)
            }

            fun forEach(block: (node: Node) -> Unit) {
                block(this)
                left?.forEach(block)
                right?.forEach(block)
            }

            fun swap() {
                val oldLeft = left
                val oldRight = right
                left = oldRight
                right = oldLeft
            }

            override fun toString(): String = "$value($left, $right)"
        }

        fun buildTree(level: Int): Node {
            var index = 0
            fun build(level: Int): Node =
                if (level > 1) Node(index++, build(level - 1), build(level - 1)) else Node(index++)
            return build(level)
        }

        val tree = buildTree(6)

        val contents = mutableMapOf<Node?, @Composable () -> Unit>()
        tree.forEach { node ->
            contents[node] = movableContentOf {
                Marker(node.value)
                contents[node.left]?.invoke()
                contents[node.right]?.invoke()
            }
        }

        compose {
            contents[tree]?.invoke()
        }

        validate {
            tree.validateNode(this)
        }

        tree.forEach { it.swap() }

        expectChanges()

        revalidate()

        tree.forEach { it.swap() }

        expectChanges()

        revalidate()
    }

    @Test
    fun multipleContentsMovingIntoCommonParent() = compositionTest {

        val content1 = movableContentOf {
            Text("1-1")
            Text("1-2")
            Text("1-3")
        }
        val content2 = movableContentOf {
            Text("2-4")
            Text("2-5")
            Text("2-6")
        }
        val content3 = movableContentOf {
            Text("3-7")
            Text("3-8")
            Text("3-9")
        }

        var case by mutableStateOf(0)
        var level by mutableStateOf(0)

        @Composable
        fun sep() {
            Text("-----")
        }

        @Composable
        fun cases() {
            when (case) {
                0 -> {
                    sep()
                    content1()
                    sep()
                    content2()
                    sep()
                    content3()
                    sep()
                }
                1 -> {
                    content2()
                    sep()
                    content3()
                    sep()
                    content1()
                }
                2 -> {
                    sep()
                    content3()
                    content1()
                    content2()
                    sep()
                }
            }
        }

        compose {
            Column {
                if (level == 0) {
                    cases()
                }
                Column {
                    if (level == 1) {
                        cases()
                    }
                }
            }
        }

        validate {
            fun MockViewValidator.sep() {
                Text("-----")
            }

            fun MockViewValidator.value1() {
                Text("1-1")
                Text("1-2")
                Text("1-3")
            }

            fun MockViewValidator.value2() {
                Text("2-4")
                Text("2-5")
                Text("2-6")
            }

            fun MockViewValidator.value3() {
                Text("3-7")
                Text("3-8")
                Text("3-9")
            }

            fun MockViewValidator.cases() {
                when (case) {
                    0 -> {
                        this.sep()
                        this.value1()
                        this.sep()
                        this.value2()
                        this.sep()
                        this.value3()
                        this.sep()
                    }
                    1 -> {
                        this.value2()
                        this.sep()
                        this.value3()
                        this.sep()
                        this.value1()
                    }
                    2 -> {
                        this.sep()
                        this.value3()
                        this.value1()
                        this.value2()
                        this.sep()
                    }
                }
            }

            Column {
                if (level == 0) {
                    this.cases()
                }
                Column {
                    if (level == 1) {
                        this.cases()
                    }
                }
            }
        }

        fun textMap(): Map<String?, View> {
            val result = mutableMapOf<String?, View>()
            fun collect(view: View) {
                if (view.name == "Text") {
                    if (view.text?.contains('-') == false)
                        result[view.text] = view
                }
                for (child in view.children) {
                    collect(child)
                }
            }
            collect(root)
            return result
        }

        val initialMap = textMap()

        fun validateInstances() {
            val currentMap = textMap()
            for (entry in currentMap) {
                if (initialMap[entry.key] !== entry.value) {
                    error("The text value ${entry.key} had a different instance created")
                }
            }
        }

        fun test(l: Int, c: Int) {
            case = c
            level = l
            advance(ignorePendingWork = true)
            revalidate()
            validateInstances()
        }

        test(0, 0)
        test(1, 1)
        test(0, 2)
        test(1, 0)
        test(0, 1)
        test(1, 2)
    }

    @Test
    fun childIndexesAreCorrectlyCalculated() = compositionTest {
        val content = movableContentOf {
            Marker(0)
        }

        var vertical by mutableStateOf(false)
        compose {
            if (vertical) {
                Row {
                    Empty()
                    content()
                }
            } else {
                Column {
                    Empty()
                    content()
                }
            }
        }

        validate {
            if (vertical) {
                Row {
                    Marker(0)
                }
            } else {
                Column {
                    Marker(0)
                }
            }
        }

        vertical = true
        expectChanges()
        revalidate()
    }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun validateRecomposeScopesDoNotGetLost() = compositionTest {
        var isHorizontal by mutableStateOf(false)
        val displayValue = mutableStateOf(0)
        val content = movableContentOf {
            DisplayInt(displayValue)
        }

        compose {
            Stack(isHorizontal) {
                Row {
                    content()
                }
            }
        }

        validate {
            Stack(isHorizontal) {
                Row {
                    DisplayInt(displayValue)
                }
            }
        }

        displayValue.value++
        expectChanges()
        revalidate()

        isHorizontal = true
        Snapshot.sendApplyNotifications()
        testCoroutineScheduler.advanceTimeBy(10)

        displayValue.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun compositionLocalsShouldBeAvailable() = compositionTest {
        var someValue by mutableStateOf(0)
        val local = staticCompositionLocalOf<Int> {
            error("No value provided for local")
        }

        compose {
            Wrap(20) {
                CompositionLocalProvider(local provides 10) {
                    // Remember is missing intentionally so it creates a new value to ensure the
                    // new values see the  correct provider scope.
                    val content = movableContentOf {
                        Text("Local = ${local.current}")
                        Text("SomeValue = $someValue")
                    }
                    if (someValue % 2 == 0)
                        content()
                    else
                        content()
                }
            }
        }

        validate {
            Text("Local = 10")
            Text("SomeValue = $someValue")
        }

        someValue++
        advance()

        revalidate()
    }

    @Test
    fun compositionLocalsShouldBeAvailableInNestedContent() = compositionTest {
        var someValue by mutableStateOf(0)
        val local = staticCompositionLocalOf<Int> {
            error("No value provided for local")
        }

        val parent = movableContentOf<@Composable () -> Unit> { child ->
            Wrap {
                child()
            }
        }

        val child = movableContentOf {
            Text("Local = ${local.current}")
            Text("SomeValue = $someValue")
        }

        compose {
            Wrap {
                CompositionLocalProvider(local provides 10) {
                    // Remember is missing intentionally so it creates a new value to ensure the
                    // new values see the  correct provider scope.
                    if (someValue % 2 == 0)
                        parent {
                            Wrap {
                                Text("One")
                                child()
                            }
                        }
                    else
                        parent {
                            child()
                            Text("Two")
                        }
                }
            }
        }

        validate {
            if (someValue % 2 == 0) {
                Text("One")
                Text("Local = 10")
                Text("SomeValue = $someValue")
            } else {
                Text("Local = 10")
                Text("SomeValue = $someValue")
                Text("Two")
            }
        }

        someValue++
        advance()

        revalidate()
    }

    @Test
    fun subcomposeLifetime_no_movable_content() = compositionTest {
        val rememberObject = RememberedObject()
        var useInMain by mutableStateOf(false)
        var useInSub1 by mutableStateOf(false)
        var useInSub2 by mutableStateOf(false)

        @Composable fun use() { remember(rememberObject) { 1 } }
        compose {
            if (useInMain) use()
            Subcompose {
                if (useInSub1) use()
            }
            Subcompose {
                if (useInSub2) use()
            }
        }

        fun expectUnused() {
            advance()
            assertFalse(rememberObject.isLive, "RememberObject unexpectedly used")
        }
        fun expectUsed() {
            advance()
            assertTrue(rememberObject.isLive, "Expected RememberObject to be used")
        }

        expectUnused()

        // Add a use in main
        useInMain = true
        expectUsed()

        // Add in sub-composes
        useInSub1 = true
        useInSub2 = true
        expectUsed()

        // Remove it from main
        useInMain = false
        expectUsed()

        // Remove it from sub1
        useInSub1 = false
        expectUsed()

        // Transfer it from sub1 to sub2
        useInSub1 = false
        useInSub2 = true
        expectUsed()

        // Remove it altogether
        useInMain = false
        useInSub1 = false
        useInSub2 = false
        expectUnused()
    }

    @Test
    fun subcomposeLifetime_with_movable_content() = compositionTest {
        val rememberObject = RememberedObject()
        var useInMain by mutableStateOf(false)
        var useInSub1 by mutableStateOf(false)
        var useInSub2 by mutableStateOf(false)

        @Suppress("UNUSED_EXPRESSION")
        val rememberTheObject = movableContentOf {
            remember(rememberObject) { 1 }
        }

        @Composable fun use() { rememberTheObject() }
        compose {
            if (useInMain) use()
            Subcompose {
                if (useInSub1) use()
            }
            Subcompose {
                if (useInSub2) use()
            }
        }

        fun expectUnused() {
            advance()
            assertFalse(rememberObject.isLive, "RememberObject unexpectedly used")
        }
        fun expectUsed() {
            advance()
            assertTrue(rememberObject.isLive, "Expected RememberObject to be used")
        }

        expectUnused()

        // Add a use in main
        useInMain = true
        expectUsed()

        // Add in sub-composes
        useInSub1 = true
        useInSub2 = true
        expectUsed()

        // Remove it from main
        useInMain = false
        expectUsed()

        // Remove it from sub1
        useInSub1 = false
        expectUsed()

        // Transfer it from sub1 to sub2
        useInSub1 = false
        useInSub2 = true
        expectUsed()

        // Remove it altogether
        useInMain = false
        useInSub1 = false
        useInSub2 = false
        expectUnused()
    }

    @Test
    fun movableContentParameters_One() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentOf<Int> { p1 ->
            Text("Value p1=$p1, data=${data.value}")
        }

        compose {
            content(1)
            content(2)
        }

        validate {
            Text("Value p1=1, data=${data.value}")
            Text("Value p1=2, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentParameters_Two() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentOf<Int, Int> { p1, p2 ->
            Text("Value p1=$p1, p2=$p2, data=${data.value}")
        }

        compose {
            content(1, 2)
            content(3, 4)
        }

        validate {
            Text("Value p1=1, p2=2, data=${data.value}")
            Text("Value p1=3, p2=4, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentParameters_Three() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentOf<Int, Int, Int> { p1, p2, p3 ->
            Text("Value p1=$p1, p2=$p2, p3=$p3, data=${data.value}")
        }

        compose {
            content(1, 2, 3)
            content(4, 5, 6)
        }

        validate {
            Text("Value p1=1, p2=2, p3=3, data=${data.value}")
            Text("Value p1=4, p2=5, p3=6, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentParameters_Four() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentOf<Int, Int, Int, Int> { p1, p2, p3, p4 ->
            Text("Value p1=$p1, p2=$p2, p3=$p3, p4=$p4, data=${data.value}")
        }

        compose {
            content(1, 2, 3, 4)
            content(5, 6, 7, 8)
        }

        validate {
            Text("Value p1=1, p2=2, p3=3, p4=4, data=${data.value}")
            Text("Value p1=5, p2=6, p3=7, p4=8, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentReceiver_None() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentWithReceiverOf<Int>() {
            Text("Value this=$this, data=${data.value}")
        }
        val receiver1 = 100
        val receiver2 = 200

        compose {
            receiver1.content()
            receiver2.content()
        }

        validate {
            Text("Value this=100, data=${data.value}")
            Text("Value this=200, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentReceiver_One() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentWithReceiverOf<Int, Int>() { p1 ->
            Text("Value this=$this, p1=$p1, data=${data.value}")
        }
        val receiver1 = 100
        val receiver2 = 200

        compose {
            receiver1.content(1)
            receiver2.content(2)
        }

        validate {
            Text("Value this=100, p1=1, data=${data.value}")
            Text("Value this=200, p1=2, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentReceiver_Two() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentWithReceiverOf<Int, Int, Int>() { p1, p2 ->
            Text("Value this=$this, p1=$p1, p2=$p2, data=${data.value}")
        }
        val receiver1 = 100
        val receiver2 = 200

        compose {
            receiver1.content(1, 2)
            receiver2.content(3, 4)
        }

        validate {
            Text("Value this=100, p1=1, p2=2, data=${data.value}")
            Text("Value this=200, p1=3, p2=4, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentReceiver_Three() = compositionTest {
        val data = mutableStateOf(0)
        val content = movableContentWithReceiverOf<Int, Int, Int, Int>() { p1, p2, p3 ->
            Text("Value this=$this, p1=$p1, p2=$p2, p3=$p3, data=${data.value}")
        }
        val receiver1 = 100
        val receiver2 = 200

        compose {
            receiver1.content(1, 2, 3)
            receiver2.content(4, 5, 6)
        }

        validate {
            Text("Value this=100, p1=1, p2=2, p3=3, data=${data.value}")
            Text("Value this=200, p1=4, p2=5, p3=6, data=${data.value}")
        }

        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentParameters_changedParameter() = compositionTest {
        val data = mutableStateOf(0)
        val location = mutableStateOf(0)
        val content = movableContentOf<Int> { d ->
            Text("d=$d")
        }

        compose {
            if (location.value == 0) content(data.value)
            Column {
                if (location.value == 1) content(data.value)
            }
            Row {
                if (location.value == 2) content(data.value)
            }
        }

        validate {
            if (location.value == 0) Text("d=${data.value}")
            Column {
                if (location.value == 1) Text("d=${data.value}")
            }
            Row {
                if (location.value == 2) Text("d=${data.value}")
            }
        }

        location.value++
        data.value++
        expectChanges()
        revalidate()

        location.value++
        expectChanges()
        revalidate()

        location.value++
        data.value++
        expectChanges()
        revalidate()
    }

    @Test
    fun movableContentOfTheSameFunctionShouldHaveStableKeys() = compositionTest {
        val hashList1 = mutableListOf<Int>()
        val hashList2 = mutableListOf<Int>()
        val composable1: @Composable () -> Unit = {
            hashList1.add(currentCompositeKeyHash)
        }
        val composable2: @Composable () -> Unit = {
            hashList2.add(currentCompositeKeyHash)
        }
        val movableContent1A = movableContentOf(composable1)
        val movableContent1B = movableContentOf(composable1)
        val movableContent2A = movableContentOf(composable2)
        val movableContent2B = movableContentOf(composable2)
        compose {
            movableContent1A()
            movableContent1B()
            movableContent1A()
            movableContent1B()
            movableContent2A()
            movableContent2B()
            movableContent2A()
            movableContent2B()
        }

        fun List<Int>.assertAllTheSame() = forEach { assertEquals(it, first()) }
        hashList1.assertAllTheSame()
        hashList2.assertAllTheSame()
        assertNotEquals(hashList1.first(), hashList2.first())
    }

    @Test
    fun keyInsideMovableContentShouldntChangeWhenRecomposed() = compositionTest {
        val hashList = mutableListOf<Int>()
        val counter = mutableStateOf(0)
        val movableContent = movableContentOf {
            hashList.add(currentCompositeKeyHash)
            Text("counter=${counter.value}")
        }
        compose {
            movableContent()
        }

        validate {
            Text("counter=${counter.value}")
        }

        counter.value++
        expectChanges()
        revalidate()

        assertEquals(2, hashList.size)
        assertEquals(hashList[0], hashList[1])
    }
}

@Composable
private fun Row(content: @Composable () -> Unit) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "Row" } },
        update = { }
    ) {
        content()
    }
}

private fun MockViewValidator.Row(block: MockViewValidator.() -> Unit) {
    view("Row", block)
}

@Composable
private fun Column(content: @Composable () -> Unit) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "Column" } },
        update = { }
    ) {
        content()
    }
}

@Composable
private fun Empty() { }

private fun MockViewValidator.Column(block: MockViewValidator.() -> Unit) {
    view("Column", block)
}

@Composable
private fun Text(text: String) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "Text" } },
        update = {
            set(text) { attributes["text"] = it }
        }
    )
}

private fun MockViewValidator.Text(text: String) {
    view("Text")
    assertEquals(text, view.attributes["text"])
}

@Composable
private fun Marker() {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "Marker" } },
        update = { }
    )
}

private fun MockViewValidator.Marker() {
    view("Marker")
}

@Composable
private fun Marker(value: Int) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "Marker" } },
        update = {
            set(value) { attributes["value"] = it }
        }
    )
}

@Composable
private fun Stack(isHorizontal: Boolean, block: @Composable () -> Unit) {
    if (isHorizontal) {
        Column(block)
    } else {
        Row(block)
    }
}

private fun MockViewValidator.Stack(isHorizontal: Boolean, block: MockViewValidator.() -> Unit) {
    if (isHorizontal) {
        Column(block)
    } else {
        Row(block)
    }
}

@Composable
private fun DisplayInt(value: State<Int>) {
    Text("value=${value.value}")
}

private fun MockViewValidator.DisplayInt(value: State<Int>) {
    Text("value=${value.value}")
}

private fun MockViewValidator.Marker(value: Int) {
    view("Marker")
    assertEquals(value, view.attributes["value"])
}

@Composable
private fun Subcompose(content: @Composable () -> Unit) {
    val host = View().also { it.name = "SubcomposeHost" }
    ComposeNode<View, ViewApplier>(factory = { host }, update = { })
    val parent = rememberCompositionContext()
    val composition = Composition(ViewApplier(host), parent)
    composition.setContent(content)
    DisposableEffect(Unit) {
        onDispose { composition.dispose() }
    }
}

private fun MockViewValidator.Subcompose(content: MockViewValidator.() -> Unit) {
    view("SubcomposeHost", content)
}

class RememberedObject : RememberObserver {
    var count: Int = 0
    val isLive: Boolean get() = count > 0
    private var rememberedCount = 0
    private var forgottenCount = 0
    private var abandonedCount = 0

    private var died: Boolean = false

    override fun onRemembered() {
        check(!died) { "Remember observer was resurrected" }
        rememberedCount++
        count++
    }

    override fun onForgotten() {
        check(count > 0) { "Abandoned or forgotten mor times than remembered" }
        forgottenCount++
        count--
        if (count == 0) died = true
    }

    override fun onAbandoned() {
        check(count > 0) { "Abandoned or forgotten mor times than remembered" }
        abandonedCount++
        count--
        if (count == 0) died = true
    }
}
