@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.PluralStringResource

private object CommonMainPlurals0 {
    public val numberOfSongsAvailable: PluralStringResource by
    lazy { init_numberOfSongsAvailable() }
}

internal val Res.plurals.numberOfSongsAvailable: PluralStringResource
    get() = CommonMainPlurals0.numberOfSongsAvailable

private fun init_numberOfSongsAvailable(): PluralStringResource =
    org.jetbrains.compose.resources.PluralStringResource(
        "plurals:numberOfSongsAvailable", "numberOfSongsAvailable",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.commonMain.cvr", 10,
                124),
        )
    )