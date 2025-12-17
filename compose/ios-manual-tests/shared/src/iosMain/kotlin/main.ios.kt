/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.DialogProperties
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
fun ComposeEntryPoint(): UIViewController = ComposeUIViewController {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            val focusManager = LocalFocusManager.current
            Text("1. Verify keyboard appears and context menu is clickable.")
            Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                TextField(
                    state = rememberTextFieldState("Hello text!"),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { focusManager.clearFocus() },
                    modifier = Modifier.width(44.dp).fillMaxHeight()
                ) {
                    Text("↓")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showAlert by remember { mutableStateOf(false) }
            Text("2. Verify that button is visible.")
            Text("Tap on it and close AlertDialog.")
            UIKitView({
                MuNativeButton(title = "Open AlertDialog") {
                    showAlert = true
                    focusManager.clearFocus()
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp))
            if (showAlert) {
                AlertDialog(
                    onDismissRequest = { showAlert = false },
                    title = { Text("Hello AlertDialog") },
                    text = { Text("Description") },
                    confirmButton = {
                        Button(onClick = { showAlert = false }) { Text("Close") }
                    },
                    properties = DialogProperties(dismissOnClickOutside = false)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("3. Rotate the app - no crashes or glitches")

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class MuNativeButton(
    title: String,
    private val onTap: () -> Unit
): UIButton(frame = CGRectZero.readValue()) {
    init {
        layer.cornerRadius = 16.0
        layer.borderWidth = 1.0
        layer.borderColor = UIColor.lightGrayColor.CGColor
        addTarget(
            target = this,
            action = NSSelectorFromString(::onButtonClick.name),
            forControlEvents = UIControlEventTouchUpInside
        )
        setTitle(title, forState = UIControlStateNormal)
        setTitleColor(UIColor.blackColor, forState = UIControlStateNormal)
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun onButtonClick() {
        onTap()
    }
}