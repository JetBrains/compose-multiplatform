package org.jetbrains.compose.demo.widgets.ui.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.ui.WidgetsType

@Composable
fun TextInputs() {
    Column(Modifier.testTag(WidgetsType.TEXT_INPUTS.testTag)) {
        var text by remember { mutableStateOf(TextFieldValue("")) }

        // TODO Explore CoreTextField
//    CoreTextField(
//        value = text,
//        onValueChange = { newValue -> text = newValue },
//        modifier = Modifier.padding(8.dp).preferredSize(0.dp),
//        cursorColor = Color.Magenta
//    )
        TextField(
            value = text,
            onValueChange = { newValue -> text = newValue },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            singleLine = true,
            label = { Text("label") },
            placeholder = { Text("placeholder") },
        )

        OutlinedTextField(
            value = text,
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            singleLine = true,
            label = { Text(text = "Password") },
            placeholder = { Text(text = "12334444") },
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                text = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        OutlinedTextField(
            value = text,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            label = { Text(text = "Email address") },
            placeholder = { Text(text = "Your email") },
            onValueChange = {
                text = it
            }
        )
        OutlinedTextField(
            value = text,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            label = { Text(text = "Email address") },
            placeholder = { Text(text = "Your email") },
            onValueChange = {
                text = it
            }
        )

        var numberText by remember { mutableStateOf(TextFieldValue("")) }
        OutlinedTextField(
            value = numberText,
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(text = "Phone number") },
            placeholder = { Text(text = "88888888") },
            onValueChange = {
                numberText = it
            }
        )
    }
}
