package my.lib.res

import kotlin.String
import kotlin.collections.Map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

public expect val Res.allDrawableResources: Map<String, DrawableResource>

public expect val Res.allStringResources: Map<String, StringResource>

public expect val Res.allStringArrayResources: Map<String, StringArrayResource>

public expect val Res.allPluralStringResources: Map<String, PluralStringResource>

public expect val Res.allFontResources: Map<String, FontResource>
