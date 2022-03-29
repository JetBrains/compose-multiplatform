// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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

// Ignore lint warnings in documentation snippets
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "RemoveEmptyParenthesesFromLambdaCall"
)

package androidx.compose.integration.docs.layout

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/layouts/basics
 *
 * No action required if it's modified.
 */

private object LayoutBasicsSnippet1 {
    @Composable
    fun ArtistCard() {
        Text("Alfred Sisley")
        Text("3 minutes ago")
    }
}

private object LayoutBasicsSnippet2 {
    @Composable
    fun ArtistCard() {
        Column {
            Text("Alfred Sisley")
            Text("3 minutes ago")
        }
    }
}

private object LayoutBasicsSnippet3 {
    @Composable
    fun ArtistCard(artist: Artist) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(/*...*/)
            Column {
                Text(artist.name)
                Text(artist.lastSeenOnline)
            }
        }
    }
}

private object LayoutBasicsSnippet4 {
    @Composable
    fun ArtistCard(artist: Artist) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Image(/*...*/)
            Column { /*...*/ }
        }
    }
}

private object LayoutBasicsSnippet5 {
    @Composable
    fun ArtistCard(
        artist: Artist,
        onClick: () -> Unit
    ) {
        val padding = 16.dp
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(padding)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) { /*...*/ }
            Spacer(Modifier.size(padding))
            Card(elevation = 4.dp) { /*...*/ }
        }
    }
}

private object LayoutBasicsSnippet6 {
    @Composable
    fun ArtistCard(/*...*/) {
        val padding = 16.dp
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(padding)
                .fillMaxWidth()
        ) {
            // rest of the implementation
        }
    }
}

private object LayoutBasicsSnippet7 {
    @Composable
    fun ArtistCard(/*...*/) {
        val padding = 16.dp
        Column(
            Modifier
                .padding(padding)
                .clickable(onClick = onClick)
                .fillMaxWidth()
        ) {
            // rest of the implementation
        }
    }
}

private object LayoutBasicsSnippet8 {
    @Composable
    fun ArtistCard(/*...*/) {
        Row(
            modifier = Modifier.size(width = 400.dp, height = 100.dp)
        ) {
            Image(/*...*/)
            Column { /*...*/ }
        }
    }
}

private object LayoutBasicsSnippet9 {
    @Composable
    fun ArtistCard(/*...*/) {
        Row(
            modifier = Modifier.size(width = 400.dp, height = 100.dp)
        ) {
            Image(
                /*...*/
                modifier = Modifier.requiredSize(150.dp)
            )
            Column { /*...*/ }
        }
    }
}

private object LayoutBasicsSnippet10 {
    @Composable
    fun ArtistCard(/*...*/) {
        Row(
            modifier = Modifier.size(width = 400.dp, height = 100.dp)
        ) {
            Image(
                /*...*/
                modifier = Modifier.fillMaxHeight()
            )
            Column { /*...*/ }
        }
    }
}

private object LayoutBasicsSnippet11 {
    @Composable
    fun MatchParentSizeComposable() {
        Box {
            Spacer(Modifier.matchParentSize().background(Color.LightGray))
            ArtistCard()
        }
    }
}

private object LayoutBasicsSnippet12 {
    @Composable
    fun ArtistCard(artist: Artist) {
        Row(/*...*/) {
            Column {
                Text(
                    text = artist.name,
                    modifier = Modifier.paddingFromBaseline(top = 50.dp)
                )
                Text(artist.lastSeenOnline)
            }
        }
    }
}

private object LayoutBasicsSnippet13 {
    @Composable
    fun ArtistCard(artist: Artist) {
        Row(/*...*/) {
            Column {
                Text(artist.name)
                Text(
                    text = artist.lastSeenOnline,
                    modifier = Modifier.offset(x = 4.dp)
                )
            }
        }
    }
}

private object LayoutBasicsSnippet14 {
    @Composable
    fun ArtistCard(/*...*/) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                /*...*/
                modifier = Modifier.weight(2f)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                /*...*/
            }
        }
    }
}

private object LayoutBasicsSnippet15 {
    @Composable
    fun WithConstraintsComposable() {
        BoxWithConstraints {
            Text("My minHeight is $minHeight while my maxWidth is $maxWidth")
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object LayoutBasicsSnippet16 {
    @Composable
    fun HomeScreen(/*...*/) {
        Scaffold(
            drawerContent = { /*...*/ },
            topBar = { /*...*/ },
            content = { /*...*/ }
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private data class Artist(val name: String, val lastSeenOnline: String)
private class Image

@Composable
private fun Image(modifier: Modifier = Modifier) {
}

@Composable
private fun ArtistCard(modifier: Modifier = Modifier) {
}

private val onClick = {}
