/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.PlainTooltipState
import androidx.compose.material3.RichTooltipBox
import androidx.compose.material3.RichTooltipState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun PlainTooltipSample() {
    PlainTooltipBox(
        tooltip = { Text("Add to favorites") }
    ) {
        IconButton(
            onClick = { /* Icon button's click event */ },
            modifier = Modifier.tooltipAnchor()
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Localized Description"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun PlainTooltipWithManualInvocationSample() {
    val tooltipState = remember { PlainTooltipState() }
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlainTooltipBox(
            tooltip = { Text("Add to list") },
            tooltipState = tooltipState
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = "Localized Description"
            )
        }
        Spacer(Modifier.requiredHeight(30.dp))
        OutlinedButton(
            onClick = { scope.launch { tooltipState.show() } }
        ) {
            Text("Display tooltip")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Sampled
@Composable
fun RichTooltipSample() {
    val tooltipState = remember { RichTooltipState() }
    val scope = rememberCoroutineScope()
    RichTooltipBox(
        title = { Text(richTooltipSubheadText) },
        action = {
            TextButton(
                onClick = { scope.launch { tooltipState.dismiss() } }
            ) { Text(richTooltipActionText) }
        },
        text = { Text(richTooltipText) },
        tooltipState = tooltipState
    ) {
        IconButton(
            onClick = { /* Icon button's click event */ },
            modifier = Modifier.tooltipAnchor()
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Localized Description"
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Sampled
@Composable
fun RichTooltipWithManualInvocationSample() {
    val tooltipState = remember { RichTooltipState() }
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RichTooltipBox(
            title = { Text(richTooltipSubheadText) },
            action = {
                TextButton(
                    onClick = {
                        scope.launch {
                            tooltipState.dismiss()
                        }
                    }
                ) { Text(richTooltipActionText) }
            },
            text = { Text(richTooltipText) },
            tooltipState = tooltipState
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Localized Description"
            )
        }
        Spacer(Modifier.requiredHeight(30.dp))
        OutlinedButton(
            onClick = { scope.launch { tooltipState.show() } }
        ) {
            Text("Display tooltip")
        }
    }
}

const val richTooltipSubheadText = "Permissions"
const val richTooltipText =
    "Configure permissions for selected service accounts. " +
        "You can add and remove service account members and assign roles to them. " +
        "Visit go/permissions for details"
const val richTooltipActionText = "Request Access"