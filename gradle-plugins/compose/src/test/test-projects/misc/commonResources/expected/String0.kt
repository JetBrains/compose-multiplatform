@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

@ExperimentalResourceApi
private object String0 {
    public val PascalCase: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:PascalCase", "PascalCase",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val _1_kebab_case: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:_1_kebab_case", "_1_kebab_case",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val app_name: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:app_name", "app_name",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val camelCase: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:camelCase", "camelCase",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val hello: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:hello", "hello",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val multi_line: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:multi_line", "multi_line",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val str_arr: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:str_arr", "str_arr",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )

    public val str_template: StringResource = org.jetbrains.compose.resources.StringResource(
        "string:str_template", "str_template",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
        )
    )
}

@ExperimentalResourceApi
internal val Res.string.PascalCase: StringResource
    get() = String0.PascalCase

@ExperimentalResourceApi
internal val Res.string._1_kebab_case: StringResource
    get() = String0._1_kebab_case

@ExperimentalResourceApi
internal val Res.string.app_name: StringResource
    get() = String0.app_name

@ExperimentalResourceApi
internal val Res.string.camelCase: StringResource
    get() = String0.camelCase

@ExperimentalResourceApi
internal val Res.string.hello: StringResource
    get() = String0.hello

@ExperimentalResourceApi
internal val Res.string.multi_line: StringResource
    get() = String0.multi_line

@ExperimentalResourceApi
internal val Res.string.str_arr: StringResource
    get() = String0.str_arr

@ExperimentalResourceApi
internal val Res.string.str_template: StringResource
    get() = String0.str_template
