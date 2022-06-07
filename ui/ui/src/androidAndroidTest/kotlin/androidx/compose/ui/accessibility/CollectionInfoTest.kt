/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.accessibility

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.OpenComposeView
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.accessibility.hasCollectionInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CollectionInfoTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var container: OpenComposeView
    private lateinit var accessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var info: AccessibilityNodeInfoCompat

    @Before
    fun setup() {
        container = OpenComposeView(rule.activity)

        rule.runOnUiThread {
            rule.activity.setContentView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val composeView = container.getChildAt(0) as AndroidComposeView
        accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(composeView).apply {
            accessibilityForceEnabledForTesting = true
        }
        info = AccessibilityNodeInfoCompat.obtain()
    }

    @After
    fun cleanup() {
        info.recycle()
    }

    // Collection Info tests
    @Test
    fun testCollectionInfo_withSelectableGroup() {
        setContent {
            Column(Modifier.selectableGroup().testTag("collection")) {
                Box(Modifier.size(50.dp).selectable(selected = true, onClick = {}))
                Box(Modifier.size(50.dp).selectable(selected = false, onClick = {}))
            }
        }
        val collectionNode = rule.onNodeWithTag("collection").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(collectionNode)

        val resultCollectionInfo = info.collectionInfo
        Assert.assertEquals(2, resultCollectionInfo.rowCount)
        Assert.assertEquals(1, resultCollectionInfo.columnCount)
        Assert.assertEquals(false, resultCollectionInfo.isHierarchical)
    }

    @Test
    fun testDefaultCollectionInfo_lazyList() {
        val tag = "LazyColumn"
        setContent {
            LazyColumn(Modifier.testTag(tag)) {
                items(2) { BasicText("Text") }
            }
        }

        val itemNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionInfo = info.collectionInfo
        Assert.assertEquals(-1, resultCollectionInfo.rowCount)
        Assert.assertEquals(1, resultCollectionInfo.columnCount)
        Assert.assertEquals(false, resultCollectionInfo.isHierarchical)
    }

    @Test
    fun testCollectionInfo_lazyList() {
        val tag = "LazyColumn"
        setContent {
            LazyColumn(
                Modifier
                    .testTag(tag)
                    .semantics { collectionInfo = CollectionInfo(2, 1) }
            ) {
                items(2) { BasicText("Text") }
            }
        }

        val itemNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionInfo = info.collectionInfo
        Assert.assertEquals(2, resultCollectionInfo.rowCount)
        Assert.assertEquals(1, resultCollectionInfo.columnCount)
        Assert.assertEquals(false, resultCollectionInfo.isHierarchical)
    }

    @Test
    fun testCollectionInfo_withSelectableGroup_andDefaultLazyListSemantics() {
        val tag = "LazyColumn"
        setContent {
            LazyColumn(Modifier.testTag(tag).selectableGroup()) {
                items(2) { BasicText("Text") }
            }
        }

        val itemNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionInfo = info.collectionInfo
        Assert.assertEquals(-1, resultCollectionInfo.rowCount)
        Assert.assertEquals(1, resultCollectionInfo.columnCount)
        Assert.assertEquals(false, resultCollectionInfo.isHierarchical)
    }

    @Test
    fun testCollectionInfo_withSelectableGroup_andLazyListSemantics() {
        val tag = "LazyColumn"
        setContent {
            LazyColumn(
                Modifier
                    .testTag(tag)
                    .selectableGroup()
                    .semantics { collectionInfo = CollectionInfo(2, 1) }
            ) {
                items(2) { BasicText("Text") }
            }
        }

        val itemNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionInfo = info.collectionInfo
        Assert.assertEquals(2, resultCollectionInfo.rowCount)
        Assert.assertEquals(1, resultCollectionInfo.columnCount)
        Assert.assertEquals(false, resultCollectionInfo.isHierarchical)
    }

    // Collection Item Info tests
    @Test
    fun testCollectionItemInfo_withSelectableGroup() {
        setContent {
            Column(Modifier.selectableGroup()) {
                Box(Modifier.selectable(selected = true, onClick = {}).testTag("item"))
                Box(Modifier.selectable(selected = false, onClick = {}))
            }
        }

        val itemNode = rule.onNodeWithTag("item").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionItemInfo = info.collectionItemInfo
        Assert.assertEquals(0, resultCollectionItemInfo.rowIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.rowSpan)
        Assert.assertEquals(0, resultCollectionItemInfo.columnIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.columnSpan)
        Assert.assertEquals(true, resultCollectionItemInfo.isSelected)
    }

    @Test
    fun testNoCollectionItemInfo_lazyList() {
        setContent {
            LazyColumn {
                itemsIndexed(listOf("Text", "Text")) { index, item -> BasicText(item + index) }
            }
        }

        val itemNode = rule.onNodeWithText("Text0").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        Assert.assertNull(info.collectionItemInfo)
    }

    @Test
    fun testCollectionItemInfo_defaultLazyListSemantics() {
        setContent {
            LazyColumn {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }

        val itemNode = rule.onNodeWithText("Text0").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionItemInfo = info.collectionItemInfo
        Assert.assertEquals(0, resultCollectionItemInfo.rowIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.rowSpan)
        Assert.assertEquals(0, resultCollectionItemInfo.columnIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.columnSpan)
    }

    @Test
    fun testCollectionItemInfo_lazyList() {
        setContent {
            LazyColumn(Modifier.semantics { collectionInfo = CollectionInfo(2, 1) }) {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }

        val itemNode = rule.onNodeWithText("Text0").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionItemInfo = info.collectionItemInfo
        Assert.assertEquals(0, resultCollectionItemInfo.rowIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.rowSpan)
        Assert.assertEquals(0, resultCollectionItemInfo.columnIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.columnSpan)
    }

    @Test
    fun testCollectionItemInfo_withSelectableGroup_andDefaultLazyListSemantics() {
        setContent {
            LazyColumn(Modifier.selectableGroup()) {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }

        val itemNode = rule.onNodeWithText("Text0").fetchSemanticsNode()
        populateAccessibilityNodeInfoProperties(itemNode)

        val resultCollectionItemInfo = info.collectionItemInfo
        Assert.assertEquals(0, resultCollectionItemInfo.rowIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.rowSpan)
        Assert.assertEquals(0, resultCollectionItemInfo.columnIndex)
        Assert.assertEquals(1, resultCollectionItemInfo.columnSpan)
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_whenProvidedDirectly() {
        val tag = "column"
        setContent {
            Column(Modifier.testTag(tag).semantics { collectionInfo = CollectionInfo(1, 1) }) {
                // items
            }
        }

        val semanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        Assert.assertTrue(semanticsNode.hasCollectionInfo())
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_whenProvidedViaSelectableGroup() {
        val tag = "column"
        setContent {
            Column(Modifier.testTag(tag).selectableGroup()) {
                // items
            }
        }

        val semanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        Assert.assertTrue(semanticsNode.hasCollectionInfo())
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_falseWhenNotProvided() {
        val tag = "column"
        setContent {
            Column(Modifier.testTag(tag)) {
                // items
            }
        }

        val semanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        Assert.assertFalse(semanticsNode.hasCollectionInfo())
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.runOnIdle {
            container.setContent(content)
        }
    }

    private fun populateAccessibilityNodeInfoProperties(node: SemanticsNode) {
        rule.runOnIdle {
            accessibilityDelegate.populateAccessibilityNodeInfoProperties(node.id, info, node)
        }
    }
}