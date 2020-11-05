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
import androidx.compose.foundation.layout.ConstraintLayout
import androidx.compose.foundation.layout.ConstraintSet
import androidx.compose.foundation.layout.Dimension
import androidx.compose.foundation.layout.atMost
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun DemoInlineDSL() {
    ConstraintLayout {
        val (text1, text2, text3) = createRefs()

        Text(
            "Text1",
            Modifier.constrainAs(text1) {
                start.linkTo(text2.end, margin = 20.dp)
            }
        )
        Text(
            "Text2",
            Modifier.constrainAs(text2) {
                centerTo(parent)
            }
        )

        val barrier = createBottomBarrier(text1, text2)
        Text(
            "This is a very long text",
            Modifier.constrainAs(text3) {
                top.linkTo(barrier, margin = 20.dp)
                centerHorizontallyTo(parent)
                width = Dimension.preferredWrapContent.atMost(40.dp)
            }
        )
    }
}

@Sampled
@Composable
fun DemoConstraintSet() {
    ConstraintLayout(
        ConstraintSet {
            val text1 = createRefFor("text1")
            val text2 = createRefFor("text2")
            val text3 = createRefFor("text3")

            constrain(text1) {
                start.linkTo(text2.end, margin = 20.dp)
            }
            constrain(text2) {
                centerTo(parent)
            }

            val barrier = createBottomBarrier(text1, text2)
            constrain(text3) {
                top.linkTo(barrier, margin = 20.dp)
                centerHorizontallyTo(parent)
                width = Dimension.preferredWrapContent.atMost(40.dp)
            }
        }
    ) {
        Text("Text1", Modifier.layoutId("text1"))
        Text("Text2", Modifier.layoutId("text2"))
        Text("This is a very long text", Modifier.layoutId("text3"))
    }
}