package my.lib.res

import kotlin.String
import kotlin.collections.Map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

@ExperimentalResourceApi
public expect val Res.allDrawableResources: Map<String, DrawableResource>

@ExperimentalResourceApi
public expect val Res.allStringResources: Map<String, StringResource>

@ExperimentalResourceApi
public expect val Res.allStringArrayResources: Map<String, StringArrayResource>

@ExperimentalResourceApi
public expect val Res.allPluralStringResources: Map<String, PluralStringResource>

@ExperimentalResourceApi
public expect val Res.allFontResources: Map<String, FontResource>
