package example.todoapp.lite.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun EditDialog(
    item: TodoItem,
    onCloseClicked: () -> Unit,
    onTextChanged: (String) -> Unit,
    onDoneChanged: (Boolean) -> Unit,
) {
    EditDialog(
        onCloseRequest = onCloseClicked,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                value = item.text,
                modifier = Modifier.weight(1F).fillMaxWidth().sizeIn(minHeight = 192.dp),
                label = { Text("Todo text") },
                onValueChange = onTextChanged,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(text = "Completed", Modifier.padding(15.dp))

                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = onDoneChanged,
                )
            }
        }
    }
}

@Composable
private fun EditDialog(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onCloseRequest,
    ) {
        Card(elevation = 8.dp) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .height(IntrinsicSize.Min)
            ) {
                ProvideTextStyle(MaterialTheme.typography.subtitle1) {
                    Text(text = "Edit todo")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1F)) {
                    content()
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCloseRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Done")
                }
            }
        }
    }
}
