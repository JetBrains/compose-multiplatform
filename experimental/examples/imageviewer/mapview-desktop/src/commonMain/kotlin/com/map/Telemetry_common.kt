package com.map

import androidx.compose.runtime.Composable

/**
 * Для тестового отображения состояния MapState
 */
@Composable
internal expect fun Telemetry(state: InternalMapState)
