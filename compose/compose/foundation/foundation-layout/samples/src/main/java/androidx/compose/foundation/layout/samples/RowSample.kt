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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun SimpleRow() {
    Row {
        // The child with no weight will have the specified size.
        Box(Modifier.size(40.dp, 80.dp).background(Color.Magenta))
        // Has weight, the child will occupy half of the remaining width.
        Box(Modifier.height(40.dp).weight(1f).background(Color.Yellow))
        // Has weight and does not fill, the child will occupy at most half of the remaining width.
        // Therefore it will occupy 80.dp (its preferred width) if the assigned width is larger.
        Box(
            Modifier.size(80.dp, 40.dp)
                .weight(1f, fill = false)
                .background(Color.Green)
        )
    }
}

@Sampled
@Composable
fun SimpleAlignInRow() {
    Row(Modifier.fillMaxHeight()) {
        // The child with no align modifier is positioned by default so that its top edge is
        // aligned to the top of the vertical axis.
        Box(Modifier.size(80.dp, 40.dp).background(Color.Magenta))
        // Gravity.Top, the child will be positioned so that its top edge is aligned to the top
        // of the vertical axis.
        Box(
            Modifier.size(80.dp, 40.dp)
                .align(Alignment.Top)
                .background(Color.Red)
        )
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the vertical axis.
        Box(
            Modifier.size(80.dp, 40.dp)
                .align(Alignment.CenterVertically)
                .background(Color.Yellow)
        )
        // Gravity.Bottom, the child will be positioned so that its bottom edge is aligned to the
        // bottom of the vertical axis.
        Box(
            Modifier.size(80.dp, 40.dp)
                .align(Alignment.Bottom)
                .background(Color.Green)
        )
    }
}

@Sampled
@Composable
fun SimpleAlignByInRow() {
    Row(Modifier.fillMaxHeight()) {
        // The center of the magenta Box and the baselines of the two texts will be
        // vertically aligned. Note that alignBy() or alignByBaseline() has to be specified
        // for all children we want to take part in the alignment. For example, alignByBaseline()
        // means that the baseline of the text should be aligned with the alignment line
        // (possibly another baseline) specified for siblings using alignBy or alignByBaseline.
        // If no other sibling had alignBy() or alignByBaseline(), the modifier would have no
        // effect.
        Box(
            modifier = Modifier.size(80.dp, 40.dp)
                .alignBy { it.measuredHeight / 2 }
                .background(Color.Magenta)
        )
        Text(
            text = "Text 1",
            fontSize = 40.sp,
            modifier = Modifier.alignByBaseline().background(color = Color.Red)
        )
        Text(
            text = "Text 2",
            modifier = Modifier.alignByBaseline().background(color = Color.Cyan)
        )
    }
}
