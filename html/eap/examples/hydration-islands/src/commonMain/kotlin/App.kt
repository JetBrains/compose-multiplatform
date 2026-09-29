import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*

@Composable
fun WeightIsland(initialWeight: Int) {
    var weight by remember(initialWeight) { mutableStateOf(initialWeight) }

    IslandHeading("01", "Weight", "$weight")
    Div(attrs = { classes(PageStyles.islandBody) }) {
        Div(attrs = { classes(PageStyles.weightPreview) }) {
            Span(attrs = { style { fontWeight(weight) } }) { Text("Ag") }
        }
        Div(attrs = { classes(PageStyles.controls) }) {
            Label(forId = "weight-range") { Text("LIGHT") }
            RangeInput(value = weight, min = 100, max = 900, step = 10) {
                id("weight-range")
                attr("aria-label", "Font weight")
                onInput { event -> event.value?.toInt()?.let { weight = it } }
            }
            Span { Text("BOLD") }
        }
    }
}

@Composable
fun SizeIsland(initialSize: Int) {
    var size by remember(initialSize) { mutableStateOf(initialSize) }

    IslandHeading("02", "Size", "$size px")
    Div(attrs = { classes(PageStyles.islandBody) }) {
        Div(attrs = { classes(PageStyles.sizePreview) }) {
            Span(attrs = { style { fontSize(size.px) } }) { Text("Compose") }
        }
        Div(attrs = { classes(PageStyles.controls) }) {
            Label(forId = "size-range") { Text("32 PX") }
            RangeInput(value = size, min = 32, max = 144) {
                id("size-range")
                attr("aria-label", "Font size")
                onInput { event -> event.value?.toInt()?.let { size = it } }
            }
            Span { Text("144 PX") }
        }
    }
}

@Composable
fun TextIsland(initialText: String) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var editing by remember { mutableStateOf(false) }

    Div(attrs = { classes(PageStyles.islandBody) }) {
        P(attrs = { classes(PageStyles.textPreview) }) { Text(text.ifBlank { "Your next idea goes here." }) }
        Div(attrs = { classes(PageStyles.textControls) }) {
            Button(attrs = {
                classes(PageStyles.editButton)
                type(ButtonType.Button)
                onClick { editing = !editing }
            }) { Text(if (editing) "Done editing ↗" else "Edit text ↗") }
            Span { Text("A LINE OF YOUR OWN, SET IN TYPE") }
        }
        if (editing) {
            Label(forId = "island-text-input", attrs = { classes(PageStyles.inputLabel) }) {
                Text("YOUR TEXT")
            }
            TextInput(value = text) {
                id("island-text-input")
                classes(PageStyles.textInput)
                onInput { text = it.value }
            }
        }
    }
}

@Composable
internal fun IslandHeading(number: String, title: String, value: String? = null) {
    Div(attrs = { classes(PageStyles.islandHeading) }) {
        Div {
            P(attrs = { classes(PageStyles.eyebrow) }) { Text("$number / INTERACTIVE ISLAND") }
            H2 { Text(title) }
        }
        if (value != null) {
            Output(attrs = { classes(PageStyles.islandValue) }) { Text(value) }
        }
    }
}
