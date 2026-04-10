/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.components.input.demo.shared

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.components.input.onPointerClick
import org.jetbrains.compose.components.input.PointerClickEvent
import org.jetbrains.compose.components.input.isPrimaryAction
import org.jetbrains.compose.components.input.isSecondaryAction

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UsePointerClick() {
    val eventLog = remember { mutableStateListOf<String>() }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Left Column: The Interactive Targets
        Column(
            Modifier.weight(1.3f).fillMaxHeight().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("onPointerClick Laboratory", style = MaterialTheme.typography.headlineSmall)

            // Feature 1: The Multi-Input Row
            FeatureSection("1. Hardware-Aware Routing", "Unifies Primary, Secondary, and Eraser in one node.") {
                FileActionRow(onEvent = { eventLog.add(it) })
            }

            // Feature 2: Visual Indication Policies
            FeatureSection("2. Ripple Control", "Decouples the click area from the ripple trigger.") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Safe access to buttons for ripple policy
                    RipplePolicyBox("Primary Only", Modifier.weight(1f)) {
                        it.isPrimaryAction
                    }
                    RipplePolicyBox("Secondary Only", Modifier.weight(1f)) {
                        it.isSecondaryAction
                    }
                }
            }

            // Feature 3: Indication Erasure
            FeatureSection("3. Indication Erasure", "Clicking works, but visual ripples are suppressed.") {
                Box(
                    Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .onPointerClick(indication = null, interactionSource = null) {
                            eventLog.add(it.toSummary())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Ripple (Silent)", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // Right Column: The Inspector / Event Log
        Column(
            Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(24.dp)
        ) {
            Text("Live Event Inspector", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Surface(
                Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                // Use a key to force the list to stay scrolled to bottom/top
                LazyColumn(contentPadding = PaddingValues(12.dp), reverseLayout = true) {
                    items(eventLog.asReversed()) { log ->
                        Text(
                            text = "> $log",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileActionRow(onEvent: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (isHovered) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .hoverable(interactionSource)
            .onPointerClick(
                interactionSource = interactionSource,
                // Ripple only on primary (Touch or Left-Click)
                triggerPressInteraction = { it.buttons.isPrimaryPressed },
                onClick = { onEvent(it.toSummary()) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Unified Interaction Node", style = MaterialTheme.typography.labelLarge)
            Text("Try: Tap, Right-Click, or Stylus Eraser", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RipplePolicyBox(label: String, modifier: Modifier, policy: (PointerEvent) -> Boolean) {
    Box(
        modifier
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .onPointerClick(triggerPressInteraction = policy) {
                /* No-op: Just testing ripple policy */
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FeatureSection(title: String, desc: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun PointerClickEvent.toSummary(): String {
    val device = when(type) {
        PointerType.Touch -> "TOUCH"
        PointerType.Mouse -> "MOUSE"
        PointerType.Stylus -> "STYLUS"
        PointerType.Eraser -> "ERASER"
        else -> "UNKNOWN"
    }

    val action = when {
        isEraser -> "ERASE"
        isSecondaryAction -> "SECONDARY"
        isTertiaryAction -> "TERTIARY"
        isPrimaryAction -> if (buttons == null) "SYNTHETIC" else "PRIMARY"
        else -> "OTHER"
    }

    val mods = mutableListOf<String>().apply {
        if (keyboardModifiers.isShiftPressed) add("Shift")
        if (keyboardModifiers.isCtrlPressed) add("Ctrl")
        if (keyboardModifiers.isAltPressed) add("Alt")
    }.joinToString("+").ifEmpty { "None" }

    return "[$device] $action | Mods: $mods | Pos: ${position.x.toInt()},${position.y.toInt()}"
}