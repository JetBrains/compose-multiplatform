/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.lint

import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class AndroidManifestServiceExportedDetector : Detector(), XmlScanner {

    override fun getApplicableElements(): Collection<String> {
        return listOf(SdkConstants.TAG_SERVICE)
    }

    override fun visitElement(context: XmlContext, element: Element) {
        val attrExported = element.getAttribute("android:${SdkConstants.ATTR_EXPORTED}")
        if (attrExported != "true") {
            val incident = Incident(context, ISSUE)
                .message("Missing exported=true in <service> tag")
                .at(element)
            context.report(incident)
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "MissingServiceExportedEqualsTrue",
            briefDescription = "Missing exported=true declaration in the <service> tag inside" +
                " the library manifest",
            explanation = "Library-defined services should set the exported attribute to true.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                AndroidManifestServiceExportedDetector::class.java,
                Scope.MANIFEST_SCOPE
            )
        )
    }
}