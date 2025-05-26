@file:OptIn(InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String = "composeResources/app.group.resources_test.generated.resources/"

internal val Res.string.PascalCase: StringResource by lazy {
      StringResource("string:PascalCase", "PascalCase", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 172, 34),
      ))
    }

internal val Res.string._1_kebab_case: StringResource by lazy {
      StringResource("string:_1_kebab_case", "_1_kebab_case", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 135, 36),
      ))
    }

internal val Res.string.app_name: StringResource by lazy {
      StringResource("string:app_name", "app_name", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 207, 44),
      ))
    }

internal val Res.string.camelCase: StringResource by lazy {
      StringResource("string:camelCase", "camelCase", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 252, 29),
      ))
    }

internal val Res.string.hello: StringResource by lazy {
      StringResource("string:hello", "hello", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 282, 37),
      ))
    }

internal val Res.string.`info_using_release_$x`: StringResource by lazy {
      StringResource("string:info_using_release_${'$'}x", "info_using_release_${'$'}x", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 320, 57),
      ))
    }

internal val Res.string.multi_line: StringResource by lazy {
      StringResource("string:multi_line", "multi_line", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 378, 178),
      ))
    }

internal val Res.string.str_template: StringResource by lazy {
      StringResource("string:str_template", "str_template", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 557, 76),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("PascalCase", Res.string.PascalCase)
  map.put("_1_kebab_case", Res.string._1_kebab_case)
  map.put("app_name", Res.string.app_name)
  map.put("camelCase", Res.string.camelCase)
  map.put("hello", Res.string.hello)
  map.put("info_using_release_${'$'}x", Res.string.`info_using_release_$x`)
  map.put("multi_line", Res.string.multi_line)
  map.put("str_template", Res.string.str_template)
}
