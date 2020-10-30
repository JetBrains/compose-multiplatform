package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable

@Composable
expect fun SelectionContainer(children: @Composable () -> Unit)