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

@file:Suppress("unused", "UNUSED_PARAMETER")

package androidx.compose.integration.docs.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/lists
 *
 * No action required if it's modified.
 */

private object ListsSnippetsColumn {
    @Composable
    fun MessageList(messages: List<Message>) {
        Column {
            messages.forEach { message ->
                MessageRow(message)
            }
        }
    }
}

@Composable
private fun ListsSnippetsLazyListScope1() {
    LazyColumn {
        // Add a single item
        item {
            Text(text = "First item")
        }

        // Add 5 items
        items(5) { index ->
            Text(text = "Item: $index")
        }

        // Add another single item
        item {
            Text(text = "Last item")
        }
    }
}

private object ListsSnippetsLazyListScope2 {
    @Composable
    fun MessageList(messages: List<Message>) {
        LazyColumn {
            items(messages) { message ->
                MessageRow(message)
            }
        }
    }
}

@Composable
private fun ListsSnippetsContentPadding() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ...
    }
}

@Composable
private fun ListsSnippetsContentSpacing1() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ...
    }
}

@Composable
private fun ListsSnippetsContentSpacing2() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ...
    }
}

private object ListsSnippetsStickyHeaders1 {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ListWithHeader(items: List<Item>) {
        LazyColumn {
            stickyHeader {
                Header()
            }

            items(items) { item ->
                ItemRow(item)
            }
        }
    }
}

private object ListsSnippetsStickyHeaders2 {
    // TODO: This ideally would be done in the ViewModel
    val grouped = contacts.groupBy { it.firstName[0] }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ContactsList(grouped: Map<Char, List<Contact>>) {
        LazyColumn {
            grouped.forEach { (initial, contactsForInitial) ->
                stickyHeader {
                    CharacterHeader(initial)
                }

                items(contactsForInitial) { contact ->
                    ContactListItem(contact)
                }
            }
        }
    }
}

private object ListsSnippetsGrids {
    @Composable
    fun PhotoGrid(photos: List<Photo>) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp)
        ) {
            items(photos) { photo ->
                PhotoItem(photo)
            }
        }
    }
}

private object ListsSnippetsReactingScrollPosition1 {
    @Composable
    fun MessageList(messages: List<Message>) {
        // Remember our own LazyListState
        val listState = rememberLazyListState()

        // Provide it to LazyColumn
        LazyColumn(state = listState) {
            // ...
        }
    }
}

private object ListsSnippetsReactingScrollPosition2 {
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun MessageList(messages: List<Message>) {
        Box {
            val listState = rememberLazyListState()

            LazyColumn(state = listState) {
                // ...
            }

            // Show the button if the first visible item is past
            // the first item. We use a remembered derived state to
            // minimize unnecessary compositions
            val showButton by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0
                }
            }

            AnimatedVisibility(visible = showButton) {
                ScrollToTopButton()
            }
        }
    }
}

@Composable
private fun ListsSnippetsReactingScrollPosition3(messages: List<Message>) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        // ...
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it == true }
            .collect {
                MyAnalyticsService.sendScrolledPastFirstItemEvent()
            }
    }
}

private object ListsSnippetsControllingScrollPosition {
    @Composable
    fun MessageList(messages: List<Message>) {
        val listState = rememberLazyListState()
        // Remember a CoroutineScope to be able to launch
        val coroutineScope = rememberCoroutineScope()

        LazyColumn(state = listState) {
            // ...
        }

        ScrollToTopButton(
            onClick = {
                coroutineScope.launch {
                    // Animate scroll to the first item
                    listState.animateScrollToItem(index = 0)
                }
            }
        )
    }
}

private object ListsSnippetsPaging {
    @Composable
    fun MessageList(pager: Pager<Int, Message>) {
        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

        LazyColumn {
            items(lazyPagingItems) { message ->
                if (message != null) {
                    MessageRow(message)
                } else {
                    MessagePlaceholder()
                }
            }
        }
    }
}

private object ListsSnippetsItemKeys {
    @Composable
    fun MessageList(messages: List<Message>) {
        LazyColumn {
            items(
                items = messages,
                key = { message ->
                    // Return a stable + unique key for the item
                    message.id
                }
            ) { message ->
                MessageRow(message)
            }
        }
    }
}

// ========================
// Fakes below
// ========================

private class Message(val id: Long)
private class Item

private data class Contact(val firstName: String)
private val contacts = listOf<Contact>()

private class Photo
private val photos = listOf<Photo>()

@Composable
private fun MessageRow(message: Message) = Unit

@Composable
private fun MessagePlaceholder() = Unit

@Composable
private fun ItemRow(item: Item) = Unit

@Composable
private fun Header() = Unit

@Composable
private fun CharacterHeader(initial: Char) = Unit

@Composable
private fun ContactListItem(contact: Contact) = Unit

@Composable
private fun PhotoItem(photo: Photo) = Unit

@Composable
private fun ScrollToTopButton(onClick: () -> Unit = {}) = Unit

private object MyAnalyticsService {
    fun sendScrolledPastFirstItemEvent() = Unit
}