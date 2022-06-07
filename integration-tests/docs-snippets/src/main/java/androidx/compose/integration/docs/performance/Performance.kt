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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER")
@file:SuppressLint("FrequentlyChangedStateReadInComposition")

package androidx.compose.integration.docs.performance

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/performance
 *
 * No action required if it's modified.
 */

private object ContactsListBadSort {
    @Composable
    fun ContactList(
        contacts: List<Contact>,
        comparator: Comparator<Contact>,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(modifier) {
            // DON’T DO THIS
            items(contacts.sortedWith(comparator)) { contact ->
                // ...
            }
        }
    }
}

private object ContactsListGoodSort {
    @Composable
    fun ContactList(
        contacts: List<Contact>,
        comparator: Comparator<Contact>,
        modifier: Modifier = Modifier
    ) {
        val sortedContacts = remember(contacts, sortComparator) {
            contacts.sortedWith(sortComparator)
        }

        LazyColumn(modifier) {
            items(sortedContacts) {
                // ...
            }
        }
    }
}

private object NotesListNoKeys {
    @Composable
    fun NotesList(notes: List<Note>) {
        LazyColumn {
            items(
                items = notes
            ) { note ->
                NoteRow(note)
            }
        }
    }
}

private object NotesListKeys {
    @Composable
    fun NotesList(notes: List<Note>) {
        LazyColumn {
            items(
                items = notes,
                key = { note ->
                    // Return a stable, unique key for the note
                    note.id
                }
            ) { note ->
                NoteRow(note)
            }
        }
    }
}

// RememberListStateBadExampleSnippet is an antipattern, the correct way
// to do this is shown in DerivedStateSnippet
@Composable
private fun RememberListStateBadExampleSnippet() {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        // ...
    }

    val showButton = listState.firstVisibleItemIndex > 0

    AnimatedVisibility(visible = showButton) {
        ScrollToTopButton()
    }
}

@Composable
private fun DerivedStateSnippet() {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        // ...
    }

    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    AnimatedVisibility(visible = showButton) {
        ScrollToTopButton()
    }
}

private object DeferredReadPreOptimization {
    @Composable
    fun SnackDetail() {
        // ...

        Box(Modifier.fillMaxSize()) { // Recomposition Scope Start
            val scroll = rememberScrollState(0)
            // ...
            Title(snack, scroll.value)
            // ...
        } // Recomposition Scope End
    }

    @Composable
    private fun Title(snack: Snack, scroll: Int) {
        // ...
        val offset = with(LocalDensity.current) { scroll.toDp() }

        Column(
            modifier = Modifier
                .offset(y = offset)
        ) {
            // ...
        }
    }
}

private object DeferredReadFirstOptimization {
    @Composable
    fun SnackDetail() {
        // ...

        Box(Modifier.fillMaxSize()) { // Recomposition Scope Start
            val scroll = rememberScrollState(0)
            // ...
            Title(snack) { scroll.value }
            // ...
        } // Recomposition Scope End
    }

    @Composable
    private fun Title(snack: Snack, scrollProvider: () -> Int) {
        // ...
        val offset = with(LocalDensity.current) { scrollProvider().toDp() }
        Column(
            modifier = Modifier
                .offset(y = offset)
        ) {
            // ...
        }
    }
}

private object DeferredReadSecondOptimization {
    @Composable
    private fun Title(snack: Snack, scrollProvider: () -> Int) {
        // ...
        Column(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = scrollProvider()) }
        ) {
            // ...
        }
    }
}

// Following code is an antipattern, better code is in the following AnimateColorFixed
@Composable
private fun AnimateColorBadExample() {
    // Here, assume animateColorBetween() is a function that swaps between
    // two colors
    val color by animateColorBetween(Color.Cyan, Color.Magenta)

    Box(Modifier.fillMaxSize().background(color))
}

@Composable
private fun AnimateColorFixed() {
    val color by animateColorBetween(Color.Cyan, Color.Magenta)

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color)
            }
    )
}

// Following function shows antipattern, bad backwards write to state

private object BackwardsWriteSnippet {
    @Composable
    fun BadComposable() {
        var count by remember { mutableStateOf(0) }

        // Causes recomposition on click
        Button(onClick = { count++ }, Modifier.wrapContentSize()) {
            Text("Recompose")
        }

        Text("$count")
        count++ // Backwards write, writing to state after it has been read
    }
}

/*
Fakes needed for snippets to build:
 */

private data class Contact(val name: String) : Comparable<Contact> {
    override fun compareTo(other: Contact): Int {
        TODO("Not yet implemented")
    }
}

private val sortComparator = naturalOrder<Contact>()

class Note {
    val id: Int = 0
}

class NoteRow(note: Any)

@Composable
private fun ScrollToTopButton(onClick: () -> Unit = {}) = Unit

val snack = Snack()

class Snack

@Composable
private fun animateColorBetween(color1: Color, color2: Color): State<Color> {
    return remember { mutableStateOf(color1) }
}
