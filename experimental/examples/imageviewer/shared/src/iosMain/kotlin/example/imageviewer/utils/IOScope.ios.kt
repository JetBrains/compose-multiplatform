package example.imageviewer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers

// https://github.com/Kotlin/kotlinx.coroutines/issues/3205
actual @Composable
fun rememberCoroutineIOScope() = rememberCoroutineScope { Dispatchers.Default }