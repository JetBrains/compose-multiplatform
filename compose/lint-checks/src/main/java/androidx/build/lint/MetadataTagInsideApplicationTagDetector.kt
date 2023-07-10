/*
 * Copyright 2020 The Android Open Source Project
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
@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.SdkConstants.TAG_META_DATA
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.xml.AndroidManifest.NODE_APPLICATION

import org.w3c.dom.Element

class MetadataTagInsideApplicationTagDetector : Detector(), Detector.XmlScanner {

    override fun getApplicableElements(): Collection<String> {
        return listOf(TAG_META_DATA)
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (element.parentNode.nodeName == NODE_APPLICATION) {
            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getLocation(element))
                .message("Detected <application>-level meta-data tag.")
                .scope(element)

            context.report(incident)
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "MetadataTagInsideApplicationTag",
            "Detected <application>-level <meta-data> tag in library manifest",
            "Developers should not add <application>-level <meta-data> tags to library manifests" +
                " because doing so may inadvertently cause denial-of-service attacks against" +
                " other apps. Instead, developers may consider adding <metadata> nested " +
                "inside of placeholder <service> tags.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                MetadataTagInsideApplicationTagDetector::class.java,
                Scope.MANIFEST_SCOPE
            )
        )
    }
}
