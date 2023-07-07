package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UITextField

/**
 * Compose wrapper for native UITextField.
 * @param value the input text to be shown in the text field.
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field. Size should be specified in modifier.
 */
@Composable
fun ComposeUITextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    val latestOnValueChanged by rememberUpdatedState(onValueChange)

    UIKitView(
        factory = {
            val textField = object : UITextField(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                @ObjCAction
                fun editingChanged() {
                    latestOnValueChanged(text ?: "")
                }
            }
            textField.addTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
            textField
        },
        modifier = modifier,
        update = { textField ->
            textField.text = value
        }
    )
}
