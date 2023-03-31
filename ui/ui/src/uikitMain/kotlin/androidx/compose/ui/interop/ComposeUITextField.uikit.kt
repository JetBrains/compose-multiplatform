package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UITextField

/**
 * Compose wrapper for native UITextField.
 * @param modifier a [Modifier] for this text field. Size should be specified in modifier.
 * @param value the input text to be shown in the text field.
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * todo in the future, move to separate library
 */
@Composable
fun ComposeUITextField(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
    UIKitInteropView(
        modifier = modifier,
        factory = {
            val textField = object : UITextField(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                @ObjCAction
                fun editingChanged() {
                    onValueChange(text ?: "")
                }
            }
            textField.addTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
            textField
        },
        update = { textField ->
            textField.text = value
        },
        dispose = { textField ->
            textField.removeTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
        },
    )
}
