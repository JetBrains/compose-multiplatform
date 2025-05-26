@file:OptIn(InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String = "composeResources/my.lib.res/"

public val MyRes.string.PascalCase: StringResource by lazy {
      StringResource("string:PascalCase", "PascalCase", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 172, 34),
      ))
    }

public val MyRes.string._1_kebab_case: StringResource by lazy {
      StringResource("string:_1_kebab_case", "_1_kebab_case", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 135, 36),
      ))
    }

public val MyRes.string.app_name: StringResource by lazy {
      StringResource("string:app_name", "app_name", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 207, 44),
      ))
    }

public val MyRes.string.camelCase: StringResource by lazy {
      StringResource("string:camelCase", "camelCase", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 252, 29),
      ))
    }

public val MyRes.string.hello: StringResource by lazy {
      StringResource("string:hello", "hello", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 282, 37),
      ))
    }

public val MyRes.string.`info_using_release_$x`: StringResource by lazy {
      StringResource("string:info_using_release_${'$'}x", "info_using_release_${'$'}x", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 320, 57),
      ))
    }

public val MyRes.string.multi_line: StringResource by lazy {
      StringResource("string:multi_line", "multi_line", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 378, 178),
      ))
    }

public val MyRes.string.str_template: StringResource by lazy {
      StringResource("string:str_template", "str_template", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 557, 76),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("PascalCase", MyRes.string.PascalCase)
  map.put("_1_kebab_case", MyRes.string._1_kebab_case)
  map.put("app_name", MyRes.string.app_name)
  map.put("camelCase", MyRes.string.camelCase)
  map.put("hello", MyRes.string.hello)
  map.put("info_using_release_${'$'}x", MyRes.string.`info_using_release_$x`)
  map.put("multi_line", MyRes.string.multi_line)
  map.put("str_template", MyRes.string.str_template)
}
