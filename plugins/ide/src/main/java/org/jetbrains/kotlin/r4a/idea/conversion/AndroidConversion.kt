/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.LiteralExpression
import org.jetbrains.kotlin.j2k.ast.QualifiedExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.r4a.R4aUtils

internal val ANDROID_CONVERSION = conversion {
    DefaultBuiltIns.Instance.booleanType
    anyClass {
        anyAttribute {
            "true" { success<Boolean>(LiteralExpression("true")) }
            "false" { success<Boolean>(LiteralExpression("false")) }
        }
    }
    "android.view.View" {
        "focusable" {
            anyValue {
                val enumValue = when (xmlValue) {
                    "auto" -> "FOCUSABLE_AUTO"
                    "true" -> "FOCUSABLE"
                    "false" -> "NOT_FOCUSABLE"
                    else -> null
                }

                if (enumValue != null) {
                    success<Int>(enumValue.asIdentifier("android.view.View.$enumValue"))
                } else {
                    failure()
                }
            }
        }
        anyOf("layout_width", "layout_height") {
            anyValue {
                val enumValue = when (xmlValue) {
                    "match_parent", "fill_parent" -> "MATCH_PARENT"
                    "wrap_content" -> "WRAP_CONTENT"
                    else -> null
                }

                if (enumValue != null) {
                    success<Int>(enumValue.asIdentifier(
                        FqName("android.view.ViewGroup.LayoutParams.$enumValue"))
                    )
                } else {
                    failure()
                }
            }
        }
        anyAttribute {
            // Resource.
            val resourceTypes = listOf(
                "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font",
                "id", "integer", "layout", "menu", "mipmap", "string", "style", "styleable"
            ).joinToString("|")
            pattern("^@(([^:]+):)?($resourceTypes)/(.+)$") { matcher ->
                success<Int>(with(matcher) { resourceExpression(group(2), group(3), group(4)) })
            }

            // Dimension.
            pattern("^(\\d+(\\.\\d+)?)(px|dip|dp|sp|pt|in|mm)$") { matcher ->
                // Returns expression: <value>.<unit>
                val unit = matcher.group(3)
                success(QualifiedExpression(
                    qualifier = LiteralExpression(matcher.group(1)),
                    identifier = unit.asIdentifier(R4aUtils.r4aFqName("adapters.$unit")),
                    dotPrototype = null
                ), R4aUtils.r4aFqName("adapters.Dimension"))
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