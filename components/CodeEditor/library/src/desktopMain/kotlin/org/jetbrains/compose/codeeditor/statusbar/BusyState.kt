package org.jetbrains.compose.codeeditor.statusbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
internal class BusyState {
    private var busyCount = 0
    private var lock = Mutex()

    var isBusy by mutableStateOf(false)

    suspend fun busy() {
        lock.withLock {
            busyCount++
            isBusy = true
        }
    }

    suspend fun free() {
        lock.withLock {
            if (--busyCount < 0) busyCount = 0
            isBusy = busyCount > 0
        }
    }
}
