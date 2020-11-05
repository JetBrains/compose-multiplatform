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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.AmbientIndication
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@Sampled
fun IndicationSample() {
    val interactionState = remember { InteractionState() }
    Column {
        Text(
            text = "Click me and my neighbour will indicate as well!",
            modifier = Modifier
                // clickable will update interaction state and show indication for this element
                .clickable(interactionState = interactionState) { /** do something */ }
                .padding(10.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "I'm neighbour and I indicate when you click the other one",
            modifier = Modifier
                // this element doesn't have a click, but will indicate as it accepts same
                // interaction state
                .indication(interactionState, AmbientIndication.current())
                .padding(10.dp)
        )
    }
}