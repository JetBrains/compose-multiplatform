package example.imageviewer.utils

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

expect @Composable
fun rememberCoroutineIOScope(): CoroutineScope