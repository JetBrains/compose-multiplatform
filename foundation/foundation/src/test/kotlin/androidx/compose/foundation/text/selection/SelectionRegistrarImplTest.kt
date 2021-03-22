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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.layout.LayoutCoordinates
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionRegistrarImplTest {
    @Test
    fun subscribe() {
        val handlerId1 = 1L
        val handlerId2 = 2L
        val handler1: Selectable = mockSelectable(handlerId1)
        val handler2: Selectable = mockSelectable(handlerId2)

        val selectionRegistrar = SelectionRegistrarImpl()

        val key1 = selectionRegistrar.subscribe(handler1)
        val key2 = selectionRegistrar.subscribe(handler2)

        assertThat(key1).isEqualTo(handler1)
        assertThat(key2).isEqualTo(handler2)
        assertThat(selectionRegistrar.selectables.size).isEqualTo(2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun subscribe_with_same_key_throws_exception() {
        val handlerId1 = 1L
        val handler1: Selectable = mockSelectable(handlerId1)

        val handlerId2 = 1L
        val handler2: Selectable = mockSelectable(handlerId2)

        val selectionRegistrar = SelectionRegistrarImpl()

        selectionRegistrar.subscribe(handler1)
        selectionRegistrar.subscribe(handler2)
    }

    @Test
    fun unsubscribe() {
        val handler1: Selectable = mock()
        val handler2: Selectable = mock()
        val handlerId1 = 1L
        val handlerId2 = 2L
        whenever(handler1.selectableId).thenReturn(handlerId1)
        whenever(handler2.selectableId).thenReturn(handlerId2)
        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler1)
        val id2 = selectionRegistrar.subscribe(handler2)

        selectionRegistrar.unsubscribe(id2)

        assertThat(selectionRegistrar.selectables.size).isEqualTo(1)
    }

    @Test
    fun sort() {
        val handlerId0 = 1L
        val handlerId1 = 2L
        val handlerId2 = 3L
        val handlerId3 = 4L

        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val layoutCoordinates1 = mock<LayoutCoordinates>()
        val layoutCoordinates2 = mock<LayoutCoordinates>()
        val layoutCoordinates3 = mock<LayoutCoordinates>()

        // Setup.
        val handler0 = mockSelectable(handlerId0, layoutCoordinates0)
        val handler1 = mockSelectable(handlerId1, layoutCoordinates1)
        val handler2 = mockSelectable(handlerId2, layoutCoordinates2)
        val handler3 = mockSelectable(handlerId3, layoutCoordinates3)

        // The order of the 4 handlers should be 1, 0, 3, 2.
        val relativeCoordinates0 = Offset(20f, 12f)
        val relativeCoordinates1 = Offset(5f, 12f)
        val relativeCoordinates2 = Offset(20f, 24f)
        val relativeCoordinates3 = Offset(5f, 24f)

        val containerLayoutCoordinates = mock<LayoutCoordinates> {
            on { localPositionOf(layoutCoordinates0, Offset.Zero) } doAnswer relativeCoordinates0
            on { localPositionOf(layoutCoordinates1, Offset.Zero) } doAnswer relativeCoordinates1
            on { localPositionOf(layoutCoordinates2, Offset.Zero) } doAnswer relativeCoordinates2
            on { localPositionOf(layoutCoordinates3, Offset.Zero) } doAnswer relativeCoordinates3
        }

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.subscribe(handler1)
        selectionRegistrar.subscribe(handler2)
        selectionRegistrar.subscribe(handler3)

        // Act.
        selectionRegistrar.sort(containerLayoutCoordinates)

        // Assert.
        assertThat(selectionRegistrar.selectables[0]).isEqualTo(handler1)
        assertThat(selectionRegistrar.selectables[1]).isEqualTo(handler0)
        assertThat(selectionRegistrar.selectables[2]).isEqualTo(handler3)
        assertThat(selectionRegistrar.selectables[3]).isEqualTo(handler2)
        assertThat(selectionRegistrar.sorted).isTrue()
    }

    @Test
    fun unsubscribe_after_sorting() {
        val handlerId0 = 1L
        val handlerId1 = 2L
        val handlerId2 = 3L
        val handlerId3 = 4L

        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val layoutCoordinates1 = mock<LayoutCoordinates>()
        val layoutCoordinates2 = mock<LayoutCoordinates>()
        val layoutCoordinates3 = mock<LayoutCoordinates>()

        // Setup.
        val handler0 = mockSelectable(handlerId0, layoutCoordinates0)
        val handler1 = mockSelectable(handlerId1, layoutCoordinates1)
        val handler2 = mockSelectable(handlerId2, layoutCoordinates2)
        val handler3 = mockSelectable(handlerId3, layoutCoordinates3)

        // The order of the 4 handlers should be 1, 0, 3, 2.
        val relativeCoordinates0 = Offset(20f, 12f)
        val relativeCoordinates1 = Offset(5f, 12f)
        val relativeCoordinates2 = Offset(20f, 24f)
        val relativeCoordinates3 = Offset(5f, 24f)

        val containerLayoutCoordinates = mock<LayoutCoordinates> {
            on { localPositionOf(layoutCoordinates0, Offset.Zero) } doAnswer relativeCoordinates0
            on { localPositionOf(layoutCoordinates1, Offset.Zero) } doAnswer relativeCoordinates1
            on { localPositionOf(layoutCoordinates2, Offset.Zero) } doAnswer relativeCoordinates2
            on { localPositionOf(layoutCoordinates3, Offset.Zero) } doAnswer relativeCoordinates3
        }

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.subscribe(handler1)
        selectionRegistrar.subscribe(handler2)
        selectionRegistrar.subscribe(handler3)

        selectionRegistrar.sort(containerLayoutCoordinates)

        // Act.
        selectionRegistrar.unsubscribe(handler0)

        // Assert.
        assertThat(selectionRegistrar.selectables[0]).isEqualTo(handler1)
        assertThat(selectionRegistrar.selectables[1]).isEqualTo(handler3)
        assertThat(selectionRegistrar.selectables[2]).isEqualTo(handler2)
    }

    @Test
    fun subscribe_after_sorting() {
        // Setup.
        val handlerId0 = 1L
        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val handler0 = mockSelectable(handlerId0, layoutCoordinates0)
        val containerLayoutCoordinates = mock<LayoutCoordinates> {
            on { localPositionOf(layoutCoordinates0, Offset.Zero) } doAnswer Offset.Zero
        }

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.sort(containerLayoutCoordinates)
        assertThat(selectionRegistrar.sorted).isTrue()

        val selectableId = 2L
        val selectable = mockSelectable(selectableId)
        // Act.
        selectionRegistrar.subscribe(selectable)

        // Assert.
        assertThat(selectionRegistrar.sorted).isFalse()
    }

    @Test
    fun layoutCoordinates_changed_after_sorting() {
        // Setup.
        val handlerId0 = 1L
        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val handler0 = mockSelectable(handlerId0, layoutCoordinates0)
        val containerLayoutCoordinates = mock<LayoutCoordinates> {
            on { localPositionOf(layoutCoordinates0, Offset.Zero) } doAnswer Offset.Zero
        }

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.sort(containerLayoutCoordinates)
        assertThat(selectionRegistrar.sorted).isTrue()

        // Act.
        selectionRegistrar.notifyPositionChange(handlerId0)

        // Assert.
        assertThat(selectionRegistrar.sorted).isFalse()
    }
}

private fun mockSelectable(
    selectableId: Long,
    layoutCoordinates: LayoutCoordinates? = null
): Selectable {
    val selectable: Selectable = mock()
    whenever(selectable.selectableId).thenReturn(selectableId)
    layoutCoordinates?.let {
        whenever(selectable.getLayoutCoordinates()).thenReturn(it)
    }
    return selectable
}
