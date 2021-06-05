package example.todo.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import example.todo.common.edit.TodoEdit

@Composable
fun TodoEditContent(component: TodoEdit) {
    val model by component.models.subscribeAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = { Text("Edit todo") },
            navigationIcon = {
                IconButton(onClick = component::onCloseClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        TextField(
            value = model.text,
            modifier = Modifier.weight(1F).fillMaxWidth().padding(8.dp),
            label = { Text("Todo text") },
            onValueChange = component::onTextChanged
        )

        Row(modifier = Modifier.padding(8.dp)) {
            Text(text = "Completed")

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = model.isDone,
                onCheckedChange = component::onDoneChanged
            )
        }
    }
}
