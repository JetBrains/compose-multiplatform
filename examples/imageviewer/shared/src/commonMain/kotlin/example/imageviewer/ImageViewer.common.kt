package example.imageviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ExternalImageViewerEvent {
    Next,
    Previous,
    ReturnBack,
}

@Composable
fun ImageViewerCommon(
    dependencies: Dependencies
) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeContent)) {
            CommonDialog()
            //NaturalScrolling()
            //DynamicType()
            //TextFieldCapitalization()
            //TestFramework()
        }
    }
}

@Composable
fun CommonDialog() {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)) {
        var isDialogOpen by remember { mutableStateOf(false) }
        Button(onClick = {
            isDialogOpen = true
        }) {
            Text("Open")
        }
        if (isDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDialogOpen = false },
                confirmButton = {
                    Button(onClick = { isDialogOpen = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Alert Dialog") },
                text = { Text("Lorem Ipsum") },
            )
        }
    }
}


@Composable
fun NaturalScrolling() {
    val items = (1..30).map { "Item $it" }
    LazyColumn {
        items(items) {
            Text(
                text = it,
                fontSize = 30.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

@Composable
fun DynamicType() {
    Text("This is some sample text", fontSize = 30.sp)
}

@Composable
fun TextFieldCapitalization() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrect = false,
            keyboardType = KeyboardType.Ascii,
        ),
    )
}

@Composable
fun TestFramework(){
    var searchText by remember { mutableStateOf("cats") }
    val searchHistory = remember { mutableStateListOf<String>() }


    Column(modifier = Modifier.padding(30.dp)) {
        TextField(
            modifier = Modifier.testTag("searchText"),
            value = searchText,
            onValueChange = {
                searchText = it
            }
        )
        Button(
            modifier = Modifier.testTag("search"),
            onClick = {
                searchHistory.add("You searched for: $searchText")
            }
        ) {
            Text("Search")
        }
        LazyColumn {
            items(searchHistory) {
                Text(
                    text = it,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 10.dp).testTag("attempt")
                )
            }
        }
    }
}