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

package androidx.compose.integration.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.Demo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * A scrollable list of [launchableDemos], filtered by [filterText].
 */
@Composable
fun DemoFilter(launchableDemos: List<Demo>, filterText: String, onNavigate: (Demo) -> Unit) {
    val filteredDemos = launchableDemos
        .filter { it.title.contains(filterText, ignoreCase = true) }
        .sortedBy { it.title }
    // TODO: migrate to LazyColumn after b/175671850
    Column(Modifier.verticalScroll(rememberScrollState())) {
        filteredDemos.forEach { demo ->
            FilteredDemoListItem(
                demo,
                filterText = filterText,
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * [SmallTopAppBar] with a text field allowing filtering all the demos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAppBar(
    filterText: String,
    onFilter: (String) -> Unit,
    onClose: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    SmallTopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, null)
            }
        },
        title = {
            FilterField(
                filterText,
                onFilter,
                Modifier.fillMaxWidth()
            )
        },
        scrollBehavior = scrollBehavior
    )
}

/**
 * [BasicTextField] that edits the current [filterText], providing [onFilter] when edited.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FilterField(
    filterText: String,
    onFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    // TODO: replace with Material text field when available
    BasicTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = filterText,
        onValueChange = onFilter,
        textStyle = LocalTextStyle.current,
        cursorBrush = SolidColor(LocalContentColor.current)
    )
    DisposableEffect(focusRequester) {
        focusRequester.requestFocus()
        onDispose { }
    }
}

/**
 * [ListItem] that displays a [demo] and highlights any matches for [filterText] inside [Demo.title]
 */
@Composable
private fun FilteredDemoListItem(
    demo: Demo,
    filterText: String,
    onNavigate: (Demo) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val annotatedString = buildAnnotatedString {
        val title = demo.title
        var currentIndex = 0
        val pattern = filterText.toRegex(option = RegexOption.IGNORE_CASE)
        pattern.findAll(title).forEach { result ->
            val index = result.range.first
            if (index > currentIndex) {
                append(title.substring(currentIndex, index))
                currentIndex = index
            }
            withStyle(SpanStyle(color = primary)) {
                append(result.value)
            }
            currentIndex = result.range.last + 1
        }
        if (currentIndex <= title.lastIndex) {
            append(title.substring(currentIndex, title.length))
        }
    }
    key(demo.title) {
        ListItem(
            onClick = { onNavigate(demo) }) {
            Text(
                modifier = Modifier.height(56.dp).wrapContentSize(Alignment.Center),
                text = annotatedString
            )
        }
    }
}
