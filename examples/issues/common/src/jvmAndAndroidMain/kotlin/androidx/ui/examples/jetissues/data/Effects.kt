package androidx.ui.examples.jetissues.data

import androidx.compose.runtime.*

typealias RepositoryCall<T> = ((Result<T>) -> Unit) -> Unit

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val exception: Exception) : UiState<Nothing>()
}

@Composable
fun <T> uiStateFrom(
    vararg inputs: Any?,
    repositoryCall: RepositoryCall<T>
): MutableState<UiState<T>> {
    val state: MutableState<UiState<T>> = remember { mutableStateOf(UiState.Loading) }

    DisposableEffect(*inputs) {
        state.value = UiState.Loading
        repositoryCall { result ->
            state.value = when (result) {
                is Result.Success -> UiState.Success(result.data)
                is Result.Error -> UiState.Error(result.exception)
            }
        }
        onDispose {  }
    }

    return state
}