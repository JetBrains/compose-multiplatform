/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SameWidthBoxes() {
    // Builds a layout containing three Box having the same width as the widest one.
    //
    // Here width min intrinsic is adding a width premeasurement pass for the
    // Column, whose minimum intrinsic width will correspond to the preferred width of the largest
    // Box. Then width min intrinsic will measure the Column with tight width, the
    // same as the premeasured minimum intrinsic width, which due to fillMaxWidth will force
    // the Box's to use the same width.
    Box {
        Column(Modifier.width(IntrinsicSize.Min).fillMaxHeight()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .size(20.dp, 10.dp)
                    .background(Color.Gray)
            )
            Box(
                modifier = Modifier.fillMaxWidth()
                    .size(30.dp, 10.dp)
                    .background(Color.Blue)
            )
            Box(
                modifier = Modifier.fillMaxWidth()
                    .size(10.dp, 10.dp)
                    .background(Color.Magenta)
            )
        }
    }
}

@Sampled
@Composable
fun MatchParentDividerForText() {
    // Builds a layout containing two pieces of text separated by a divider, where the divider
    // is sized according to the height of the longest text.
    //
    // Here height min intrinsic is adding a height premeasurement pass for the Row,
    // whose minimum intrinsic height will correspond to the height of the largest Text. Then
    // height min intrinsic will measure the Row with tight height, the same as the
    // premeasured minimum intrinsic height, which due to fillMaxHeight will force the Texts and
    // the divider to use the same height.
    Box {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Text(
                text = "This is a really short text",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            Text(
                text = "This is a much much much much much much much much much much" +
                    " much much much much much much longer text",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Sampled
@Composable
fun SameWidthTextBoxes() {
    // Builds a layout containing three Text boxes having the same width as the widest one.
    //
    // Here width max intrinsic is adding a width premeasurement pass for the Column,
    // whose maximum intrinsic width will correspond to the preferred width of the largest
    // Box. Then width max intrinsic will measure the Column with tight width, the
    // same as the premeasured maximum intrinsic width, which due to fillMaxWidth modifiers will
    // force the Boxs to use the same width.

    Box {
        Column(Modifier.width(IntrinsicSize.Max).fillMaxHeight()) {
            Box(Modifier.fillMaxWidth().background(Color.Gray)) {
                Text("Short text")
            }
            Box(Modifier.fillMaxWidth().background(Color.Blue)) {
                Text("Extremely long text giving the width of its siblings")
            }
            Box(Modifier.fillMaxWidth().background(Color.Magenta)) {
                Text("Medium length text")
            }
        }
    }
}

@Sampled
@Composable
fun MatchParentDividerForAspectRatio() {
    // Builds a layout containing two aspectRatios separated by a divider, where the divider
    // is sized according to the height of the taller aspectRatio.
    //
    // Here height max intrinsic is adding a height premeasurement pass for the
    // Row, whose maximum intrinsic height will correspond to the height of the taller
    // aspectRatio. Then height max intrinsic will measure the Row with tight height,
    // the same as the premeasured maximum intrinsic height, which due to fillMaxHeight modifier
    // will force the aspectRatios and the divider to use the same height.
    //
    Box {
        Row(Modifier.height(IntrinsicSize.Max)) {
            val modifier = Modifier.fillMaxHeight().weight(1f)
            Box(modifier.aspectRatio(2f).background(Color.Gray))
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            Box(modifier.aspectRatio(1f).background(Color.Blue))
        }
    }
}
