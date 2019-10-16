/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.idea

import androidx.compose.plugins.kotlin.ComposeCallResolutionInterceptorExtension
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope

class IdeComposeCallResolutionInterceptorExtension : ComposeCallResolutionInterceptorExtension() {
    override fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver?,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {

        if (isComposeEnabled(resolutionContext.call.callElement)) {
            return super.interceptCandidates(
                candidates,
                scopeTower,
                resolutionContext,
                resolutionScope,
                callResolver,
                name,
                location
            )
        }
        return candidates
    }
}