package example.imageviewer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers

actual @Composable
fun rememberCoroutineIOScope() = rememberCoroutineScope { Dispatchers.IO }