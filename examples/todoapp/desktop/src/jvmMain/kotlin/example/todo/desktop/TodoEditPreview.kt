/*
 * Should be in the compose-ui module, see https://github.com/JetBrains/compose-jb/issues/908
 */

package example.todo.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import example.todo.common.edit.TodoEdit
import example.todo.common.edit.TodoEdit.Model
import example.todo.common.ui.TodoEditContent

@Composable
@Preview
fun TodoEditContentPreview() {
    TodoEditContent(TodoEditPreview())
}

class TodoEditPreview : TodoEdit {
    override val models: Value<Model> =
        MutableValue(
            Model(
                text = "Some text",
                isDone = true
            )
        )

    override fun onTextChanged(text: String) {}
    override fun onDoneChanged(isDone: Boolean) {}
    override fun onCloseClicked() {}
}
