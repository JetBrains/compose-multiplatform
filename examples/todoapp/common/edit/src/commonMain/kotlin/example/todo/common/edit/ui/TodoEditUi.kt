package example.todo.common.edit.ui

import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.badoo.reaktive.base.Consumer
import example.todo.common.edit.TodoEdit.Output
import example.todo.common.edit.store.TodoEditStore.Intent
import example.todo.common.edit.store.TodoEditStore.State

@Composable
internal fun TodoEditUi(
    state: State,
    output: Consumer<Output>,
    intents: (Intent) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = { Text("Edit todo") },
            navigationIcon = {
                IconButton(onClick = { output.onNext(Output.Finished) }) {
                    Icon(Icons.Default.ArrowBack)
                }
            }
        )

        TextField(
            value = state.text,
            modifier = Modifier.weight(1F).fillMaxWidth().padding(8.dp),
            label = { Text("Todo text") },
            onValueChange = { intents(Intent.SetText(text = it)) }
        )

        Row(modifier = Modifier.padding(8.dp)) {
            Text(text = "Completed")

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = state.isDone,
                onCheckedChange = { intents(Intent.SetDone(isDone = it)) }
            )
        }
    }
}
