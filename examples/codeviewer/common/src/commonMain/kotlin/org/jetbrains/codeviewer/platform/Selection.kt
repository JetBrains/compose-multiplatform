package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable

@Composable
expect fun SelectionContainer(children: @Composable () -> Unit)

@Composable
expect fun WithoutSelection(children: @Composable () -> Unit)