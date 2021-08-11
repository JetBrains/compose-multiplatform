/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.build.releasenotes

/** Classes for generating markdown */

enum class HeaderType {
    H1, H2, H3, H4, H5, H6;
    companion object {
        fun getHeaderTag(tag: HeaderType): String {
            when (tag) {
                H1 -> return("#")
                H2 -> return("##")
                H3 -> return("###")
                H4 -> return("####")
                H5 -> return("#####")
                H6 -> return("######")
            }
        }
    }
}

open class MarkdownHeader {
    var markdownType: HeaderType = HeaderType.H1
    var text: String = ""

    @Override
    override fun toString(): String {
        return HeaderType.getHeaderTag(markdownType) + ' ' + text
    }
    fun print() {
        println(toString())
    }
}

open class MarkdownLink {
    var linkText: String = ""
    var linkUrl: String = ""

    @Override
    override fun toString(): String {
        return "([$linkText]($linkUrl))"
    }

    fun print() {
        println(toString())
    }
}

open class MarkdownBoldText(
    inputText: String
) {
    var text: String = ""
    init {
        text = inputText
    }

    override fun toString(): String {
        return "**$text**"
    }

    fun print() {
        println(toString())
    }
}

open class MarkdownComment(
    inputText: String
) {
    var text: String = ""
    init {
        text = inputText
    }

    override fun toString(): String {
        return "{# $text #}"
    }

    fun print() {
        println(toString())
    }
}