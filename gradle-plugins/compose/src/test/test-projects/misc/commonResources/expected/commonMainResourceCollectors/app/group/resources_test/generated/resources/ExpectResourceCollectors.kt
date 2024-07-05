package app.group.resources_test.generated.resources

import kotlin.String
import kotlin.collections.Map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

@ExperimentalResourceApi
internal expect val Res.allDrawableResources: Map<String, DrawableResource>

@ExperimentalResourceApi
internal expect val Res.allStringResources: Map<String, StringResource>

@ExperimentalResourceApi
internal expect val Res.allStringArrayResources: Map<String, StringArrayResource>

@ExperimentalResourceApi
internal expect val Res.allPluralStringResources: Map<String, PluralStringResource>

@ExperimentalResourceApi
internal expect val Res.allFontResources: Map<String, FontResource>
