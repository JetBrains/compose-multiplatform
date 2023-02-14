package example.imageviewer.utils

import kotlinx.coroutines.Dispatchers

// https://github.com/Kotlin/kotlinx.coroutines/issues/3205
actual val ioDispatcher  = Dispatchers.Default