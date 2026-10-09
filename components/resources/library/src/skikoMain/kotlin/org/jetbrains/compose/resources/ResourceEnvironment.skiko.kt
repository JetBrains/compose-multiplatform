package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale

@Composable
internal actual fun rememberResourceLocale(): Locale = Locale.current
