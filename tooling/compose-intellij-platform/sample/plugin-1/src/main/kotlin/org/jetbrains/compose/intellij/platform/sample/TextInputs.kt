package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun TextInputs() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        var name by remember { mutableStateOf(TextFieldValue("")) }
        var password by remember { mutableStateOf(TextFieldValue("")) }

        TextField(
            value = name,
            onValueChange = { newValue -> name = newValue },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.SansSerif),
            label = { Text("Account:") },
            placeholder = { Text("account name") }
        )

        OutlinedTextField(
            value = password,
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            label = { Text(text = "Password:") },
            placeholder = { Text(text = "your password") },
            textStyle = TextStyle(fontFamily = FontFamily.SansSerif),
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                password = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}