/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.LiteralExpression
import org.jetbrains.kotlin.j2k.ast.QualifiedExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.r4a.R4aUtils

internal val ANDROID_CONVERSION = conversion {
    anyClass {
        anyAttribute {
            "true" { LiteralExpression("true") }
            "false" { LiteralExpression("false") }
        }
    }
    "android.view.View" {
        val viewEnum: (String) -> Expression = { it.asIdentifier(import = FqName("android.view.View.$it")) }
        "focusable" {
            "auto" { viewEnum("FOCUSABLE_AUTO") }
            "true" { viewEnum("FOCUSABLE") }
            "false" { viewEnum("NOT_FOCUSABLE") }
        }
        anyOf("layout_width", "layout_height") {
            val layoutParamsEnum: (String) -> Expression = { it.asIdentifier(import = FqName("android.view.ViewGroup.LayoutParams.$it")) }
            anyOf("match_parent", "fill_parent") { layoutParamsEnum("MATCH_PARENT") }
            "wrap_content" { layoutParamsEnum("WRAP_CONTENT") }
        }
        anyAttribute {
            // Resource.
            val resourceTypes = listOf(
                "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font", "id", "integer", "layout", "menu",
                "mipmap", "string", "style", "styleable"
            ).joinToString("|")
            pattern("^@(([^:]+):)?($resourceTypes)/(.+)$") { matcher ->
                with(matcher) { resourceExpression(group(2), group(3), group(4)) }
            }

            // Dimension.
            pattern("^(\\d+(\\.\\d+)?)(px|dip|dp|sp|pt|in|mm)$") { matcher ->
                // Returns expression: <value>.<unit>
                val unit = matcher.group(3)
                QualifiedExpression(
                    qualifier = LiteralExpression(matcher.group(1)),
                    identifier = unit.asIdentifier(R4aUtils.r4aFqName("adapters.$unit")),
                    dotPrototype = null
                )
            }
        }
    }
}

// Returns expression: [<package>.]R.<type>.<name>
private fun resourceExpression(packageName: String?, type: String, name: String): Expression {
    // TODO(jdemeulenaere): We directly return the resource ID
    return if (packageName == null) {
        resourceExpression(type, name)
    } else {
        QualifiedExpression(
            qualifier = packageName.asIdentifier(),
            identifier = resourceExpression(type, name),
            dotPrototype = null
        )
    }
}

// Returns expression: R.<type>.<name>
private fun resourceExpression(type: String, name: String): Expression {
    return QualifiedExpression(
        qualifier = QualifiedExpression(
            qualifier = "R".asIdentifier(),
            identifier = type.asIdentifier(),
            dotPrototype = null
        ),
        identifier = name.asIdentifier(),
        dotPrototype = null
    )
}