/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.demos

import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.compose.foundation.DefaultMarqueeVelocity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.MarqueeAnimationMode.Companion.WhileFocused
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.samples.BasicFocusableMarqueeSample
import androidx.compose.foundation.samples.BasicMarqueeSample
import androidx.compose.foundation.samples.BasicMarqueeWithFadedEdgesSample
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Preview(showBackground = true)
@Composable
fun BasicMarqueeDemo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(4.dp)
    ) {
        Text("Android marquees:", style = MaterialTheme.typography.subtitle1)
        AndroidMarqueeTextView("short", Modifier.fillMaxWidth())
        listOf(40.dp, 80.dp, 120.dp).forEach {
            AndroidMarqueeTextView("long text in short marquee", Modifier.width(it))
        }
        AndroidMarqueeWithClickableLink()
        Row {
            Text("Tap to focus: ")
            AndroidMarqueeTextView(
                "only animate when focused",
                Modifier.size(80.dp, 30.dp),
                initiallySelected = false,
                focusable = true
            )
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Text("Compose marquees:", style = MaterialTheme.typography.subtitle1)
        MarqueeText("short", Modifier.fillMaxWidth())
        listOf(40.dp, 80.dp, 120.dp).forEach {
            MarqueeText("long text in short marquee", Modifier.width(it))
        }
        MarqueeText(
            "backwards animation",
            Modifier.width(80.dp),
            velocity = -DefaultMarqueeVelocity
        )
        MarqueeWithClickable()
        Row {
            Text("Tap to focus: ")
            MarqueeText(
                "only animate when focused",
                Modifier.size(80.dp, 30.dp),
                animationMode = WhileFocused
            )
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Text("Samples:", style = MaterialTheme.typography.subtitle1)
        BasicMarqueeSample()
        BasicFocusableMarqueeSample()
        BasicMarqueeWithFadedEdgesSample()
    }
}

@Composable
private fun AndroidMarqueeWithClickableLink() {
    val text = SpannableStringBuilder("text with link").apply {
        setSpan(URLSpan("https://www.google.com"), 5, 9, 0)
    }
    AndroidMarqueeTextView(text, Modifier.width(60.dp))
}

@Composable
private fun AndroidMarqueeTextView(
    text: CharSequence,
    modifier: Modifier = Modifier,
    initiallySelected: Boolean = true,
    focusable: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    AndroidView(
        modifier = modifier
            .border(if (isFocused) 4.dp else 1.dp, Color.Black)
            .onFocusChanged { isFocused = it.hasFocus },
        factory = {
            TextView(it).apply {
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1 // repeat forever
                // Enable clickable spans.
                movementMethod = LinkMovementMethod.getInstance()
                isSingleLine = true
                isSelected = initiallySelected
                isFocusableInTouchMode = focusable
            }
        },
        update = {
            it.text = text
        }
    )
}

@Composable
private fun MarqueeWithClickable() {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .width(60.dp)
            .border(1.dp, Color.Black)
            .basicMarquee(iterations = Int.MAX_VALUE),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("text ")
        TextButton(onClick = { uriHandler.openUri("https://www.google.com") }) {
            Text("with")
        }
        Text(" link")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    animationMode: MarqueeAnimationMode = Immediately,
    velocity: Dp = DefaultMarqueeVelocity
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Text(
        text,
        modifier
            .border(if (isFocused) 4.dp else 1.dp, Color.Black)
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                animationMode = animationMode,
                velocity = velocity
            )
            .clickable(
                onClick = { focusRequester.requestFocus() },
                indication = null,
                interactionSource = MutableInteractionSource()
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusRequester(focusRequester)
            .then(if (animationMode == WhileFocused) Modifier.focusable() else Modifier)
    )
}