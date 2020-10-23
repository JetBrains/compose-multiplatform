package example.todo.common.utils

import androidx.compose.runtime.Composable

interface Component {

    @Composable
    operator fun invoke()
}
