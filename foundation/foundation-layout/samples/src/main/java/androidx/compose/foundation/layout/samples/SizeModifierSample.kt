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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SimpleSizeModifier() {
    // The result is a 50.dp x 50.dp red box centered in a 100.dp x 100.dp space.
    // Note that although a previous modifier asked it to be 100.dp x 100.dp, this
    // will not be respected. They would be respected if preferredSize was used instead of size.
    Box(
        Modifier
            .size(100.dp, 100.dp)
            .size(50.dp, 50.dp)
            .background(Color.Red)
    )
}

@Sampled
@Composable
fun SimpleWidthModifier() {
    // The result is a 50.dp x 50.dp magenta box centered in a 100.dp x 100.dp space.
    // Note that although a previous modifier asked it to be 100.dp width, this
    // will not be respected. They would be respected if preferredWidth was used instead of width.
    Box(
        Modifier
            .width(100.dp)
            .width(50.dp)
            .aspectRatio(1f)
            .background(Color.Magenta)
    )
}

@Sampled
@Composable
fun SimpleHeightModifier() {
    // The result is a 50.dp x 50.dp blue box centered in a 100.dp x 100.dp space.
    // Note that although a previous modifier asked it to be 100.dp height, this
    // will not be respected. They would be respected if preferredHeight was used instead of height.
    Box(
        Modifier
            .height(100.dp)
            .height(50.dp)
            .aspectRatio(1f)
            .background(Color.Blue)
    )
}

@Sampled
@Composable
fun SimplePreferredSizeModifier() {
    Box {
        Box(Modifier.preferredSize(100.dp, 100.dp).background(Color.Red))
    }
}

@Sampled
@Composable
fun SimplePreferredWidthModifier() {
    Box {
        Box(Modifier.preferredWidth(100.dp).aspectRatio(1f).background(Color.Magenta))
    }
}

@Sampled
@Composable
fun SimplePreferredHeightModifier() {
    Box {
        Box(Modifier.preferredHeight(100.dp).aspectRatio(1f).background(Color.Blue))
    }
}

@Sampled
@Composable
fun SimpleFillWidthModifier() {
    Box(Modifier.fillMaxWidth().background(Color.Red), contentAlignment = Alignment.Center) {
        Box(Modifier.preferredSize(100.dp).background(color = Color.Magenta))
    }
}

@Sampled
@Composable
fun FillHalfWidthModifier() {
    Box(Modifier.size(100.dp).background(Color.Red), contentAlignment = Alignment.Center) {
        // The inner Box will be (50.dp x 30.dp).
        Box(
            Modifier.fillMaxWidth(fraction = 0.5f)
                .height(30.dp)
                .background(color = Color.Magenta)
        )
    }
}

@Sampled
@Composable
fun SimpleFillHeightModifier() {
    Box(Modifier.fillMaxHeight().background(Color.Red), contentAlignment = Alignment.Center) {
        Box(Modifier.preferredSize(100.dp).background(color = Color.Magenta))
    }
}

@Sampled
@Composable
fun FillHalfHeightModifier() {
    Box(Modifier.size(100.dp).background(Color.Red), contentAlignment = Alignment.Center) {
        // The inner Box will be (30.dp x 50.dp).
        Box(
            Modifier.width(30.dp)
                .fillMaxHeight(0.5f)
                .background(color = Color.Magenta)
        )
    }
}

@Sampled
@Composable
fun SimpleFillModifier() {
    Box(Modifier.fillMaxSize().background(Color.Red), contentAlignment = Alignment.Center) {
        Box(Modifier.preferredSize(100.dp).background(color = Color.Magenta))
    }
}

@Sampled
@Composable
fun FillHalfSizeModifier() {
    Box(Modifier.size(100.dp).background(Color.Red), contentAlignment = Alignment.Center) {
        // The inner Box will be (50.dp x 50.dp).
        Box(
            Modifier.width(30.dp)
                .fillMaxSize(0.5f)
                .background(color = Color.Magenta)
        )
    }
}

@Sampled
@Composable
fun SimpleWrapContentAlignedModifier() {
    // Here the result will be a 20.dp x 20.dp blue box top-centered in a 40.dp x 40.dp space.
    // Because of the preferredSizeIn modifier, if wrapContentSize did not exist, the blue rectangle
    // would actually be 40.dp x 40.dp to satisfy the min size set by the modifier. However,
    // because we provide wrapContentSize, the blue rectangle is specified to be wrap
    // content - if the desired size is smaller than 40.dp x 40.dp, it will be top-centered in
    // this space. Therefore the 20.dp x 20.dp is top-centered in the space.
    Box(
        Modifier.preferredSizeIn(minWidth = 40.dp, minHeight = 40.dp)
            .wrapContentSize(Alignment.TopCenter)
            .preferredSize(20.dp)
            .background(Color.Blue)
    )
}

@Sampled
@Composable
fun SimpleWrapContentVerticallyAlignedModifier() {
    // Here the result will be a 50.dp x 20.dp blue box centered vertically in a 50.dp x 50.dp
    // space. Because of the preferredSize modifier, if wrapContentHeight did not exist,
    // the blue rectangle would actually be 50.dp x 50.dp to satisfy the size set by the modifier.
    // However, because we provide wrapContentHeight, the blue rectangle is specified to be wrap
    // content in height - if the desired height is smaller than 50.dp, it will be centered
    // vertically in this space. Therefore the 50.dp x 20.dp is centered vertically in the space.
    Box(
        Modifier.preferredSize(50.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .preferredHeight(20.dp)
            .background(Color.Blue)
    )
}

@Sampled
@Composable
fun SimpleWrapContentHorizontallyAlignedModifier() {
    // Here the result will be a 20.dp x 50.dp blue box centered vertically in a 50.dp x 50.dp
    // space. Because of the preferredSize modifier, if wrapContentWidth did not exist,
    // the blue rectangle would actually be 50.dp x 50.dp to satisfy the size set by the modifier.
    // However, because we provide wrapContentWidth, the blue rectangle is specified to be wrap
    // content in width - if the desired width is smaller than 50.dp, it will be centered
    // horizontally in this space. Therefore the 50.dp x 20.dp is centered horizontally
    // in the space.
    Box(
        Modifier.preferredSize(50.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .preferredWidth(20.dp)
            .background(Color.Blue)
    )
}

@Sampled
@Composable
fun DefaultMinSizeConstraintsSample() {
    @Composable
    fun DefaultMinBox(modifier: Modifier = Modifier) {
        Box(
            modifier.defaultMinSizeConstraints(minWidth = 100.dp, minHeight = 100.dp)
                .background(Color.Blue)
        )
    }
    // This will be a 100.dp x 100.dp blue box. Because we are not providing any min constraints
    // to the DefaultMinBox, defaultMinSizeConstraints will apply its min constraints.
    DefaultMinBox()
    // This will be a 50.dp x 50.dp blue box. Because we are providing min constraints
    // to the DefaultMinBox, defaultMinSizeConstraints will not apply its min constraints.
    DefaultMinBox(Modifier.sizeIn(minWidth = 50.dp, minHeight = 50.dp))
    // Note that if DefaultMinBox used sizeIn or preferredSizeIn rather than
    // defaultMinSizeConstraints, the min constraints would have been applied with either
    // of the above usages.
}
