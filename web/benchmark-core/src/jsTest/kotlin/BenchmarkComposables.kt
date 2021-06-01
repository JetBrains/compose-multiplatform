import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

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