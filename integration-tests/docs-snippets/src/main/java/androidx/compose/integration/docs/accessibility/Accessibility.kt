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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.accessibility

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/xxxxxxxxxxxxxxx
 *
 * No action required if it's modified.
 */

private object AccessibilitySnippet1 {
    @Composable
    fun ShareButton(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.label_share)
            )
        }
    }
}

private object AccessibilitySnippet2 {
    @Composable
    fun PostImage(post: Post, modifier: Modifier = Modifier) {
        val image = if (post.imageThumb != null) {
            BitmapPainter(post.imageThumb)
        } else {
            painterResource(R.drawable.placeholder)
        }

        Image(
            painter = image,
            // Specify that this image has no semantic meaning
            contentDescription = null,
            modifier = modifier
                .size(40.dp, 40.dp)
                .clip(MaterialTheme.shapes.small)
        )
    }
}

private object AccessibilitySnippet4 {
    @Composable
    private fun PostMetadata(metadata: Metadata) {
        // Merge elements below for accessibility purposes
        Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Image(
                imageVector = Icons.Filled.AccountCircle,
                // As this image is decorative, contentDescription is set to null
                contentDescription = null
            )
            Column {
                Text(metadata.author.name)
                Text("${metadata.date} • ${metadata.readTimeMinutes} min read")
            }
        }
    }
}

private object AccessibilitySnippet5 {
    @Composable
    fun PostCardSimple(
        /* ... */
        isFavorite: Boolean,
        onToggleFavorite: () -> Boolean
    ) {
        val actionLabel = stringResource(
            if (isFavorite) R.string.unfavorite else R.string.favorite
        )
        Row(
            modifier = Modifier
                .clickable(onClick = { /* ... */ })
                .semantics {
                    // Set any explicit semantic properties
                    customActions = listOf(
                        CustomAccessibilityAction(actionLabel, onToggleFavorite)
                    )
                }
        ) {
            /* ... */
            BookmarkButton(
                isBookmarked = isFavorite,
                onClick = onToggleFavorite,
                // Clear any semantics properties set on this node
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
    }
}

private object AccessibilitySnippet6 {
    @Composable
    private fun TopicItem(itemTitle: String, selected: Boolean, onToggle: () -> Unit) {
        val stateSubscribed = stringResource(R.string.subscribed)
        val stateNotSubscribed = stringResource(R.string.not_subscribed)
        Row(
            modifier = Modifier
                .semantics {
                    // Set any explicit semantic properties
                    stateDescription = if (selected) stateSubscribed else stateNotSubscribed
                }
                .toggleable(
                    value = selected,
                    onValueChange = { onToggle() }
                )
        ) {
            /* ... */
        }
    }
}

private object AccessibilitySnippet7 {
    @Composable
    private fun Subsection(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.semantics { heading() }
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object string {
        const val label_share = 4
        const val unfavorite = 6
        const val favorite = 7
        const val subscribed = 8
        const val not_subscribed = 9
    }

    object drawable {
        const val placeholder = 1
    }
}

private class Post(val imageThumb: ImageBitmap? = null)
private class Metadata(
    val author: Author = Author(),
    val date: String? = null,
    val readTimeMinutes: String? = null
)

private class Author(val name: String = "fake")
private class BookmarkButton(isBookmarked: Boolean, onClick: () -> Boolean, modifier: Modifier)
