/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import com.intellij.lang.PsiBuilder
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.parsing.KtxStatementsParser
import java.util.ArrayList

class R4aKtxParsingExtension : KtxParsingExtension {

    override fun atKtxStart(parser: KotlinExpressionParsing): Boolean {
        return parser.at(LT)
    }

    override fun parseKtxTag(parser: KotlinExpressionParsing) {
        parser.parseKtxTag()
    }

    override fun createStatementsParser(parser: KotlinExpressionParsing, closeTagToken: String?): KtxStatementsParser {
        return object : KtxStatementsParser {
            val tags = ArrayList<KtxMarkerStart>()
            var shouldBreak = false
            var shouldContinue = false

            override fun handleTag() {
                if (parser.shouldExitKtxBodyBlock(tags, closeTagToken)) {
                    shouldBreak = true
                    shouldContinue = false
                } else {
                    parser.handleKtx(tags)
                    shouldBreak = false
                    shouldContinue = true
                }
            }

            override fun shouldBreak() = shouldBreak;
            override fun shouldContinue() = shouldContinue;

            override fun finish() {
                // Close all open KTX tags
                for (i in tags.size - 1 downTo 0) {
                    val marker = tags.removeAt(i)
                    marker.bodyMarker!!.done(BLOCK)
                    marker.functionLiteralMarker!!.done(FUNCTION_LITERAL)
                    marker.lambdaExpressionMarker!!.done(KTX_BODY_LAMBDA)
                    marker.close()
                }
            }
        }
    }
}


private fun KotlinExpressionParsing.shouldExitKtxBodyBlock(tags: List<KtxMarkerStart>, closeTagToken: String?): Boolean {
    val marker = mark()
    try {
        if (closeTagToken == null) return false
        if (!at(LT)) return false
        advance()
        if (!at(DIV)) return false
        advance()
        val tagName = parseKtxTagName()
        for (tag in tags)
            if (closeTagToken == tag.tagName)
                return false
        return tagName == null || closeTagToken == tagName
    } finally {
        marker.rollbackTo()
    }
}


private fun KotlinExpressionParsing.atCloseKtxTag(): Boolean {
    return lookahead(0) === LT && lookahead(1) === DIV
}

private fun KotlinExpressionParsing.handleKtx(markers: MutableList<KtxMarkerStart>) {
    // Determine if this is a open or close tag
    if (atCloseKtxTag()) {
        val startOfTag = mark()
        // Find the best match in terms of tags to close
        val tagName = parseKtxCloseTag()
        if (markers.size == 0) {
            startOfTag.drop()
            return
        }
        var index = markers.size - 1
        val tagsToClose = ArrayList<KtxMarkerStart>()
        if (tagName == null) {
            // We don't know what type of close tag it is, so just close something and try to recover as gracefully as possible.
            tagsToClose.add(markers.removeAt(index))
        } else {
            while (index >= 0) {
                if (tagName == markers[index].tagName) break
                index--
            }
            if (index >= 0 && !markers.isEmpty() && tagName == markers[index].tagName) {
                for (i in markers.size - 1 downTo index) {
                    tagsToClose.add(markers.removeAt(i))
                }
            }
        }
        if (tagsToClose.size > 0) {
            startOfTag.rollbackTo()
            for (i in 0 until tagsToClose.size - 1) {
                val tag = tagsToClose[i]
                tag.bodyMarker!!.done(BLOCK)
                tag.functionLiteralMarker!!.done(FUNCTION_LITERAL)
                tag.lambdaExpressionMarker!!.done(KTX_BODY_LAMBDA)
                tag.close()
            }
            val tag = tagsToClose[tagsToClose.size - 1]
            tag.bodyMarker!!.done(BLOCK)
            tag.functionLiteralMarker!!.done(FUNCTION_LITERAL)
            tag.lambdaExpressionMarker!!.done(KTX_BODY_LAMBDA)
            parseKtxCloseTag()
            tag.close()
        } else {
            startOfTag.drop()
        }
    } else {
        // Open a new ktx tag
        val tag = parseKtxOpenTag()
        if (tag != null && !tag.isClosed) {
            tag.lambdaExpressionMarker = mark()
            tag.functionLiteralMarker = mark()
            parseKtxChildrenParameters()
            tag.bodyMarker = mark()
            markers.add(tag)
        }
    }

}

/*
     * KtxElement
     *  ;
     */
private fun KotlinExpressionParsing.parseKtxTag() {
    val startTag = parseKtxOpenTag()
    if (startTag == null || startTag.isClosed) return

    startTag.lambdaExpressionMarker = mark()
    startTag.functionLiteralMarker = mark()
    parseKtxChildrenParameters()

    startTag.bodyMarker = mark()
    parseStatements(false, startTag.tagName)
    startTag.bodyMarker!!.done(BLOCK)
    startTag.functionLiteralMarker!!.done(FUNCTION_LITERAL)
    startTag.lambdaExpressionMarker!!.done(KTX_BODY_LAMBDA)

    parseKtxCloseTag()
    startTag.close()
}

private fun KotlinExpressionParsing.parseKtxChildrenParameters() {

    myBuilder.enableNewlines()

    if (at(ARROW)) {
        //   { -> ...}
        mark().done(VALUE_PARAMETER_LIST)
        advance() // ARROW
    } else if (at(IDENTIFIER) || at(COLON) || at(LPAR)) {
        // Try to parse a simple name list followed by an ARROW
        //   {a -> ...}
        //   {a, b -> ...}
        //   {(a, b) -> ... }
        val rollbackMarker = mark()
        val nextToken = lookahead(1)
        val preferParamsToExpressions = nextToken === COMMA || nextToken === COLON
        parseFunctionLiteralParameterList()

        if (at(ARROW)) {
            advance() // ARROW
            rollbackMarker.drop()
        } else if (preferParamsToExpressions && atCloseKtxTag()) {
            rollbackMarker.drop()
            expect(ARROW, "An -> is expected")
        } else {
            rollbackMarker.rollbackTo()
            mark().done(VALUE_PARAMETER_LIST)
        }
    }
}

private fun KotlinExpressionParsing.parseKtxTagNameIdentifier(): String? {
    val ref = mark()
    if (at(IDENTIFIER)) {
        val identifierText = myBuilder.getTokenText()
        advance() // IDENTIFIER
        ref.done(REFERENCE_EXPRESSION)
        return identifierText
    } else {
        ref.error("Expected identifier as part of KTX tag name")
        ref.rollbackTo()
        return null
    }
}

private fun KotlinExpressionParsing.parseKtxTagName(): String {
    var tagName = ""
    var expression: PsiBuilder.Marker = mark()
    tagName += parseKtxTagNameIdentifier()
    var firstExpressionParsed = true

    while (true) {
        if (interruptedWithNewLine()) {
            break
        } else if (at(DOT)) {
            advance() // DOT
            if (!firstExpressionParsed) {
                expression.drop()
                expression = mark()
            }
            tagName += "." + parseKtxTagNameIdentifier()
            if (firstExpressionParsed) {
                expression.done(DOT_QUALIFIED_EXPRESSION)
            } else {
                firstExpressionParsed = true
                continue
            }
        } else {
            break
        }
        expression = expression.precede()
    }



    expression.drop()
    return tagName
}

/*
     * KtxAttribute
     *  ;
     */
private fun KotlinExpressionParsing.parseKtxAttribute() {
    val marker = mark()
    if (at(IDENTIFIER)) {
        val attrKeyMark = mark()
        advance()
        attrKeyMark.done(REFERENCE_EXPRESSION)
    } else {
        error("Expected attribute name")
    }

    if (at(EQ)) {
        advance()
        parsePrefixExpression()
    }
    marker.done(KTX_ATTRIBUTE)
}

/*
     * KtxElement
     *  ;
     */
private fun KotlinExpressionParsing.parseKtxOpenTag(): KtxMarkerStart? {

    val marker = mark()

    if (at(LT)) {
        advance()
    } else {
        errorAndAdvance("Expecting a KTX Element")
        marker.drop()
        return null
    }

    val tagName = parseKtxTagName()
    val result = KtxMarkerStart(tagName, marker)

    while (at(IDENTIFIER)) {
        parseKtxAttribute()
    }

    if (at(DIV)) {
        advance()
        if (at(GT)) {
            advance()
            result.close()
            return result
        } else {
            result.close()
            errorAndAdvance("Expecting `>` (end of KTX tag)")
            return result
        }
    } else if (at(GT)) {
        advance()
        return result
    } else {
        marker.error("Invalid KTX tag")
        return null
    }
}

/*
     * KtxElement
     *  ;
     */
private fun KotlinExpressionParsing.parseKtxCloseTag(): String? {
    val marker = mark()

    if (at(LT)) {
        advance()
    } else {
        marker.error("Expected a KTX close tag")
        return null
    }

    if (at(DIV)) {
        advance()
    } else {
        marker.error("Expected tag to be a closing tag")
        return null
    }

    val tagName = parseKtxTagName()

    if (at(GT)) {
        advance()
        marker.drop()
        return tagName
    } else {
        marker.error("Expected `>` (end of KTX close tag)")
        return tagName
    }
}


class KtxMarkerStart(tagName: String, internal var tagMarker: PsiBuilder.Marker) {
    var tagName: String
        internal set
    internal var bodyMarker: PsiBuilder.Marker? = null
    internal var functionLiteralMarker: PsiBuilder.Marker? = null
    internal var lambdaExpressionMarker: PsiBuilder.Marker? = null
    internal var isClosed: Boolean = false
    internal var closedAt: Throwable? = null

    init {
        this.tagName = tagName
    }

    fun close() {
        if (closedAt != null) {
            //    throw new RuntimeException("Already closed at", closedAt);
        }
        closedAt = Throwable("Previously closed here")
        tagMarker.done(KTX_ELEMENT)
        isClosed = true
    }
}

