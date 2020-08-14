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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SimpleRow() {
    Row {
        // The child with no weight will have the specified size.
        Box(Modifier.preferredSize(40.dp, 80.dp), backgroundColor = Color.Magenta)
        // Has weight, the child will occupy half of the remaining width.
        Box(Modifier.preferredHeight(40.dp).weight(1f), backgroundColor = Color.Yellow)
        // Has weight and does not fill, the child will occupy at most half of the remaining width.
        // Therefore it will occupy 80.dp (its preferred width) if the assigned width is larger.
        Box(
            Modifier.preferredSize(80.dp, 40.dp).weight(1f, fill = false),
            backgroundColor = Color.Green
        )
    }
}

@Sampled
@Composable
fun SimpleGravityInRow() {
    Row(Modifier.fillMaxHeight()) {
        // The child with no gravity modifier is positioned by default so that its top edge is
        // aligned to the top of the vertical axis.
        Box(Modifier.preferredSize(80.dp, 40.dp), backgroundColor = Color.Magenta)
        // Gravity.Top, the child will be positioned so that its top edge is aligned to the top
        // of the vertical axis.
        Box(
            Modifier.preferredSize(80.dp, 40.dp)
                .gravity(Alignment.Top),
            backgroundColor = Color.Red
        )
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the vertical axis.
        Box(
            Modifier.preferredSize(80.dp, 40.dp)
                .gravity(Alignment.CenterVertically),
            backgroundColor = Color.Yellow
        )
        // Gravity.Bottom, the child will be positioned so that its bottom edge is aligned to the
        // bottom of the vertical axis.
        Box(
            Modifier.preferredSize(80.dp, 40.dp)
                .gravity(Alignment.Bottom),
            backgroundColor = Color.Green
        )
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblingsInRow() {
    Row(Modifier.fillMaxHeight()) {
        // Center of the colored rectangle is aligned to first baseline of the text.
        Box(
            backgroundColor = Color.Red,
            modifier = Modifier.preferredSize(80.dp, 40.dp)
                .alignWithSiblings { it.height / 2 }
        )
        Text(
            text = "Text.",
            modifier = Modifier.alignWithSiblings(FirstBaseline).background(color = Color.Cyan)
        )
    }
}
