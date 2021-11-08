/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
projectUrl = "https://github.com/JetBrains/compose-jb",
projectInceptionYear = "2020",
licenseName = "The Apache Software License, Version 2.0",
licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt",
licenseDistribution = "repo",
scmConnection = "scm:git:https://github.com/JetBrains/compose-jb.git",
scmDeveloperConnection = "scm:git:https://github.com/JetBrains/compose-jb.git",
scmUrl = "https://github.com/JetBrains/compose-jb",
developerName = "Compose Multiplatform Team",
developerOrganization = "JetBrains",
developerOrganizationUrl = "https://www.jetbrains.com",
 */
internal class PomDocument(file: File) {
    private val doc: Document
    val groupId: String?
    val artifactId: String?
    val version: String?
    val packaging: String?

    init {
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val projectNodes = doc.project.children().asMap()
        groupId = projectNodes["groupId"]?.textContent
        artifactId = projectNodes["artifactId"]?.textContent
        version = projectNodes["version"]?.textContent
        packaging = projectNodes["packaging"]?.textContent ?: "jar"
    }

    fun coordiateAsString() = "$groupId:$artifactId:$version"

    fun saveTo(outputFile: File) {
        val sw = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
        }
        transformer.transform(DOMSource(doc), StreamResult(sw))
        outputFile.bufferedWriter().use { writer ->
            for (line in sw.toString().lineSequence()) {
                if (line.isNotBlank()) {
                    writer.appendLine(line)
                }
            }
        }
    }

    fun fillMissingTags(
        projectUrl: String,
        projectInceptionYear: String,
        licenseName: String,
        licenseUrl: String,
        licenseDistribution: String,
        scmConnection: String,
        scmDeveloperConnection: String,
        scmUrl: String,
        developerName: String,
        developerOrganization: String,
        developerOrganizationUrl: String,
    ): Unit = with (doc) {
        val originalNodes = project.children().asMap()

        val nameText = originalNodes["name"]?.textContent
            ?: originalNodes["artifactId"]!!.textContent
                .split("-")
                .joinToString(" ") { it.capitalize() }
        val name = newNode("name", nameText)
        val description = newNode("description", (originalNodes["description"] ?: name).textContent)
        val url = newNode("url", projectUrl)
        val inceptionYear = newNode("inceptionYear", projectInceptionYear)
        val licences =
            newNode("licenses").withChildren(
                newNode("license").withChildren(
                    newNode("name", licenseName),
                    newNode("url", licenseUrl),
                    newNode("distribution", licenseDistribution)
                )
            )
        val scm =
            newNode("scm").withChildren(
                newNode("connection", scmConnection),
                newNode("developerConnection", scmDeveloperConnection),
                newNode("url", scmUrl)
            )
        val developers =
            newNode("developers").withChildren(
                newNode("developer").withChildren(
                    newNode("name", developerName),
                    newNode("organization", developerOrganization),
                    newNode("organizationUrl", developerOrganizationUrl),
                )
            )
        val dependencies = originalNodes["dependencies"]
        val nodesToInsert = listOf(
            name, description, url, inceptionYear, licences, scm, developers, dependencies
        ).filterNotNull()
        for (nodeToInsert in nodesToInsert) {
            val originalNode = originalNodes[nodeToInsert.nodeName]
            if (originalNode != null) {
                project.removeChild(originalNode)
            }
            project.appendChild(nodeToInsert)
        }
    }

    private fun Document.newNode(tag: String, value: String? = null, fn: Element.() -> Unit = {}) =
        createElement(tag).apply {
            if (value != null) {
                appendChild(createTextNode(value))
            }
            fn()
        }

    private fun Element.withChildren(vararg nodes: Node): Element {
        nodes.forEach { appendChild(it) }
        return this
    }

    private fun Node.children(): List<Node> {
        val result = ArrayList<Node>(childNodes.length)
        for (i in 0 until childNodes.length) {
            result.add(childNodes.item(i))
        }
        return result
    }

    private fun List<Node>.asMap(): Map<String, Node> =
        associateBy { it.nodeName }

    private fun Node.getChildByTag(tag: String): Node =
        findChildByTag(tag) ?: error("Could not find <$tag>")

    private fun Node.findChildByTag(tag: String): Node? =
        children().firstOrNull { it.nodeName == tag }

    private val Document.project: Node
        get() = getChildByTag("project")

}
