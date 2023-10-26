package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Finds resource file by ID depending on current environment
 */
internal suspend fun getPathById(id: ResourceId): String = id //TODO

@Composable
internal fun rememberFilePath(id: ResourceId): State<String> = mutableStateOf(id) //TODO