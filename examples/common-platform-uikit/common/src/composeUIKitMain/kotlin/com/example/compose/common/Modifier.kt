/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.compose.common

import platform.UIKit.UIView
import platform.UIKit.addConstraint
import platform.UIKit.addConstraints
import platform.UIKit.bottomAnchor
import platform.UIKit.heightAnchor
import platform.UIKit.leadingAnchor
import platform.UIKit.superview
import platform.UIKit.topAnchor
import platform.UIKit.trailingAnchor
import platform.UIKit.widthAnchor

actual interface Modifier {
    val modifications: List<(UIView) -> Unit>

    actual companion object : Modifier {
        override val modifications: List<(UIView) -> Unit> = emptyList()
    }
}

internal fun UIView.modify(modifier: Modifier) {
    modifier.modifications.forEach { it(this) }
}

internal fun Modifier.then(modification: (UIView) -> Unit): Modifier {
    val original = this
    return object : Modifier {
        override val modifications: List<(UIView) -> Unit> = original.modifications + modification
    }
}

actual fun Modifier.padding(top: Int, bottom: Int, start: Int, end: Int): Modifier {
    return this.then { view ->
        // TODO
    }
}

actual fun Modifier.width(value: Int): Modifier {
    return this.then { view ->
        view.addConstraint(view.widthAnchor.constraintEqualToConstant(value.toDouble()).apply { active = true })
    }
}

actual fun Modifier.height(value: Int): Modifier {
    return this.then { view ->
        view.addConstraint(view.heightAnchor.constraintEqualToConstant(value.toDouble()).apply { active = true })
    }
}

actual fun Modifier.fillMaxSize(): Modifier {
    return this.then { view ->
        val parentView: UIView = view.superview
            ?: throw IllegalStateException("modifier applied to UIView without parent - $view")

        parentView.addConstraints(
            listOf(
                view.leadingAnchor.constraintEqualToAnchor(parentView.leadingAnchor),
                view.trailingAnchor.constraintEqualToAnchor(parentView.trailingAnchor),
                view.topAnchor.constraintEqualToAnchor(parentView.topAnchor),
                view.bottomAnchor.constraintEqualToAnchor(parentView.bottomAnchor)
            ).onEach { it.active = true }
        )
    }
}

actual fun Modifier.fillMaxWidth(): Modifier {
    return this
}
