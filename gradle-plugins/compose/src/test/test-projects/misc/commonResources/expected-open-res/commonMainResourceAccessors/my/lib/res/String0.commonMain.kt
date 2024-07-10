@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource

private object CommonMainString0 {
  public val PascalCase: StringResource by 
      lazy { init_PascalCase() }

  public val _1_kebab_case: StringResource by 
      lazy { init__1_kebab_case() }

  public val app_name: StringResource by 
      lazy { init_app_name() }

  public val camelCase: StringResource by 
      lazy { init_camelCase() }

  public val hello: StringResource by 
      lazy { init_hello() }

  public val `info_using_release_$x`: StringResource by 
      lazy { `init_info_using_release_$x`() }

  public val multi_line: StringResource by 
      lazy { init_multi_line() }

  public val str_template: StringResource by 
      lazy { init_str_template() }
}

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("PascalCase", CommonMainString0.PascalCase)
  map.put("_1_kebab_case", CommonMainString0._1_kebab_case)
  map.put("app_name", CommonMainString0.app_name)
  map.put("camelCase", CommonMainString0.camelCase)
  map.put("hello", CommonMainString0.hello)
  map.put("info_using_release_${'$'}x", CommonMainString0.`info_using_release_$x`)
  map.put("multi_line", CommonMainString0.multi_line)
  map.put("str_template", CommonMainString0.str_template)
}

public val Res.string.PascalCase: StringResource
  get() = CommonMainString0.PascalCase

private fun init_PascalCase(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:PascalCase", "PascalCase",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 172, 34),
    )
)

public val Res.string._1_kebab_case: StringResource
  get() = CommonMainString0._1_kebab_case

private fun init__1_kebab_case(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:_1_kebab_case", "_1_kebab_case",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 135, 36),
    )
)

public val Res.string.app_name: StringResource
  get() = CommonMainString0.app_name

private fun init_app_name(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:app_name", "app_name",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 207, 44),
    )
)

public val Res.string.camelCase: StringResource
  get() = CommonMainString0.camelCase

private fun init_camelCase(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:camelCase", "camelCase",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 252, 29),
    )
)

public val Res.string.hello: StringResource
  get() = CommonMainString0.hello

private fun init_hello(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:hello", "hello",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 282, 37),
    )
)

public val Res.string.`info_using_release_$x`: StringResource
  get() = CommonMainString0.`info_using_release_$x`

private fun `init_info_using_release_$x`(): StringResource =
    org.jetbrains.compose.resources.StringResource(
  "string:info_using_release_${'$'}x", "info_using_release_${'$'}x",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 320, 57),
    )
)

public val Res.string.multi_line: StringResource
  get() = CommonMainString0.multi_line

private fun init_multi_line(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:multi_line", "multi_line",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 378, 178),
    )
)

public val Res.string.str_template: StringResource
  get() = CommonMainString0.str_template

private fun init_str_template(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:str_template", "str_template",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/values/strings.commonMain.cvr", 557, 76),
    )
)
