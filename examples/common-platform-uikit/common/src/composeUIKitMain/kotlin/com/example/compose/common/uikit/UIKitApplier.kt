package com.example.compose.common.uikit

import androidx.compose.runtime.AbstractApplier
import platform.UIKit.UIStackView
import platform.UIKit.UIView
import platform.UIKit.insertSubview
import platform.UIKit.removeFromSuperview
import platform.UIKit.subviews
import platform.UIKit.translatesAutoresizingMaskIntoConstraints

class UIKitApplier(
    root: UIView
) : AbstractApplier<UIView>(root) {

    override fun insertTopDown(index: Int, instance: UIView) {
        println("insertTopDown(index = $index, instance = $instance)")

        val subView: UIView = instance

        subView.translatesAutoresizingMaskIntoConstraints = false

        val current = this.current
        if (current is UIStackView) {
            current.insertArrangedSubview(subView, index.toULong())
        } else {
            current.insertSubview(subView, index.toLong())
        }
    }

    override fun insertBottomUp(index: Int, instance: UIView) = Unit

    override fun remove(index: Int, count: Int) {
        println("remove(index = $index, count = $count)")

        val current = current
        val subviews = if (current is UIStackView) {
            current.arrangedSubviews
        } else {
            current.subviews
        }
        subviews.subList(index, index + count)
            .filterIsInstance<UIView>()
            .forEach {
                println("i got $it to remove")
                it.removeFromSuperview()
            }
    }

    override fun move(from: Int, to: Int, count: Int) {
        println("move(from = $from, to = $to, count = $count)")
    }

    override fun onClear() {
        println("onClear()")
        root.subviews
            .filterIsInstance<UIView>()
            .forEach { it.removeFromSuperview() }
    }
}
