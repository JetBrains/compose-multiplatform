import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.P
import androidx.compose.web.elements.Text

@Composable
fun AddItems(count: Int) {
    repeat(count) {
        Div {
            P {
                Text("$it")
            }
            P {
                Text("$it")
            }
            Div {
                P {
                    Text("$it")
                }
            }
        }
    }
}

@Composable
fun AddItems(list: List<String>) {
    list.forEach {
        Div {
            P {
                Text(it)
            }
            P {
                Text(it)
            }
            Div {
                P {
                    Text(it)
                }
            }
        }
    }
}