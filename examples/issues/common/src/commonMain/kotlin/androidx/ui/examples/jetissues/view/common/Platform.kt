package androidx.ui.examples.jetissues.view.common

import androidx.compose.runtime.Composable

@Composable
expect fun SelectionContainer(children: @Composable () -> Unit)

@Composable
expect fun WithoutSelection(children: @Composable () -> Unit)