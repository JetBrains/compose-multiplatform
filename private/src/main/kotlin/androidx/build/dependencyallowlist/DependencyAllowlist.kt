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

package androidx.build.dependencyallowlist

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList

/**
 * @param verificationMetadataXml A string containing the entire content of a file that would
 * live at
 * [gradle/verification-metadata.xml](https://docs.gradle.org/current/userguide/dependency_verification.html#sub:enabling-verification)
 *
 * @return a list of strings that are English descriptions of problems with the dependencies
 * (At this point, merely checksum dependency components that do not link to bugs that track
 * asking them to be signed)
 */
fun allowlistWarnings(verificationMetadataXml: String): List<String> {
    return verificationMetadataComponents(verificationMetadataXml).filter { !it.hasValidReason() }
        .map {
            val componentName = it.attributes.getNamedItem("group").textContent
            "Add androidx:reason for unsigned component '$componentName'" +
                " (See go/androidx-unsigned-bugs)"
        }
}

/**
 * @param verificationMetadataXml see [allowlistWarnings]
 *
 * @return a list of [Node]s representing all of the components needing
 *         validation in the file.
 */
private fun verificationMetadataComponents(verificationMetadataXml: String): List<Node> {
    // Throw exception if there is not a single <components> element in the file.
    val singleComponentsNode =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            .parse(verificationMetadataXml.byteInputStream()).getElementsByTagName("components")
            .toList().single()

    val componentsChildNodes = singleComponentsNode.childNodes.toList()
    return componentsChildNodes.filter {
        it.nodeType == Node.ELEMENT_NODE && it.nodeName == "component"
    }
}

private const val ANDROIDX_NAMESPACE_URI = "https://developer.android.com/jetpack/androidx"

private fun Node.hasValidReason(): Boolean {
    val reason = attributes.getNamedItemNS(ANDROIDX_NAMESPACE_URI, "reason")
    return reason?.textContent?.containsBug() == true
}

private fun String.containsBug() =
    contains("b/") || contains("github.com") && contains("issues")

private fun NodeList.toList() = (0 until length).map { item(it) }