/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.compose.common

import androidx.compose.runtime.Composable
import com.example.compose.common.uikit.ActivityIndicator
import com.example.compose.common.uikit.VStack
import com.example.compose.common.uikit.ZStack
import platform.UIKit.UIView
import platform.UIKit.addConstraints
import platform.UIKit.centerXAnchor
import platform.UIKit.centerYAnchor
import platform.UIKit.superview
import com.example.compose.common.uikit.Button as UIKitButton
import com.example.compose.common.uikit.Text as UIKitText
import com.example.compose.common.uikit.TextField as UIKitTextField

@Composable
actual fun Text(modifier: Modifier, text: String) {
    UIKitText(modifier = modifier, text = text)
}

@Composable
actual fun Column(modifier: Modifier, builder: @Composable () -> Unit) {
    VStack(modifier = modifier) {
        builder()
    }
}

@Composable
actual fun Box(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    ZStack(modifier = modifier) {
        val scope = object : BoxScope {
            override fun Modifier.align(alignment: Alignment): Modifier {
                return this.then { view ->
                    val parentView: UIView = view.superview
                        ?: throw IllegalStateException("modifier applied to UIView without parent - $view")

                    parentView.addConstraints(
                        listOf(
                            view.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
                            view.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor)
                        ).onEach { it.active = true }
                    )
                }
            }
        }
        scope.content()
    }
}

@Composable
actual fun ProgressBar(modifier: Modifier) {
    ActivityIndicator(modifier = modifier)
}

@Composable
actual fun TextField(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit) {
    UIKitTextField(
        modifier = modifier,
        value = value,
        onValueChanged = onValueChange
    )
}

@Composable
actual fun Button(modifier: Modifier, text: String, onClick: () -> Unit) {
    UIKitButton(
        modifier = modifier,
        title = text,
        onClick = onClick
    )
}
