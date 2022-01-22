package com.example.compose.common.uikit

import UIViewProtocol
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.example.compose.common.Modifier
import com.example.compose.common.modify
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIAction
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIStackView
import platform.UIKit.UIStackViewAlignmentFill
import platform.UIKit.UIStackViewDistributionFillEqually
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldDelegateProtocol
import platform.UIKit.UIView
import platform.darwin.NSObject

private val zeroFrame = CGRectMake(.0, .0, .0, .0)

@Composable
fun VStack(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ComposeNode<UIStackView, UIKitApplier>(
        factory = {
            object : UIStackView(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UIStackView.didMoveToSuperView")
                    modify(modifier)
                }
            }.apply {
                axis = UILayoutConstraintAxisVertical
                alignment = UIStackViewAlignmentFill
                distribution = UIStackViewDistributionFillEqually
            }
        },
        update = {
            update(modifier) { this.modify(it) }
        },
        content = content
    )
}

@Composable
fun Text(
    modifier: Modifier,
    text: String,
    textColor: UIColor? = null,
    font: UIFont? = null
) {
    ComposeNode<UILabel, UIKitApplier>(
        factory = {
            object : UILabel(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UILabel.didMoveToSuperView")
                    modify(modifier)
                }
            }.apply {
                this.text = text
                textColor?.let { this.textColor = it }
                font?.let { this.font = it }
            }
        },
        update = {
            update(text) { this.text = it }
            update(textColor) {
                it?.let { this.textColor = it }
            }
            update(font) { it?.let { this.font = it } }
            update(modifier) { this.modify(it) }
        }
    )
}

@Composable
fun ZStack(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ComposeNode<UIView, UIKitApplier>(
        factory = {
            object : UIView(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UIView.didMoveToSuperView")
                    modify(modifier)
                }
            }
        },
        update = {
            update(modifier) { this.modify(it) }
        },
        content = content
    )
}

@Composable
fun Button(
    modifier: Modifier,
    title: String,
    onClick: () -> Unit
) {
    ComposeNode<UIButton, UIKitApplier>(
        factory = {
            object : UIButton(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UIButton.didMoveToSuperView")
                    modify(modifier)
                }
            }.apply {
                this.setTitle(title, UIControlStateNormal)
                this.addAction(UIAction.actionWithHandler { onClick() }, UIControlEventTouchUpInside)
            }
        },
        update = {
            update(title) { this.setTitle(it, UIControlStateNormal) }
            update(onClick) { this.addAction(UIAction.actionWithHandler { it() }, UIControlEventTouchUpInside) }
            update(modifier) { this.modify(it) }
        }
    )
}

@Composable
fun TextField(
    modifier: Modifier,
    value: String,
    onValueChanged: (String) -> Unit
) {
    ComposeNode<UITextField, UIKitApplier>(
        factory = {
            object : UITextField(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UITextField.didMoveToSuperView")
                    modify(modifier)
                }
            }.apply {
                this.borderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
                this.text = value
                this.delegate = createTextFieldDelegate(onValueChanged)
            }
        },
        update = {
            update(value) { this.text = it }
            update(onValueChanged) { this.delegate = createTextFieldDelegate(it) }
            update(modifier) { this.modify(it) }
        }
    )
}

@Composable
fun ActivityIndicator(modifier: Modifier) {
    ComposeNode<UIActivityIndicatorView, UIKitApplier>(
        factory = {
            object : UIActivityIndicatorView(frame = zeroFrame), UIViewProtocol {
                override fun didMoveToSuperview() {
                    println("UIActivityIndicatorView.didMoveToSuperView")
                    modify(modifier)
                }
            }.apply {
                this.startAnimating()
            }
        },
        update = {}
    )
}

private fun createTextFieldDelegate(onValueChanged: (String) -> Unit): UITextFieldDelegateProtocol {
    return object : NSObject(), UITextFieldDelegateProtocol {
        override fun textField(
            textField: UITextField,
            shouldChangeCharactersInRange: kotlinx.cinterop.CValue<platform.Foundation.NSRange>,
            replacementString: String
        ): Boolean {
            val currentValue = textField.text.orEmpty()
            val range: IntRange = shouldChangeCharactersInRange.useContents {
                IntRange(this.location.toInt(), (this.location + this.length).toInt() + 1)
            }
            println("value $currentValue range $range replacement $replacementString")
            val newValue = if (range.first == currentValue.length) {
                currentValue + replacementString
            } else {
                currentValue.replaceRange(range, replacementString)
            }
            onValueChanged(newValue)
            return false
        }
    }
}
