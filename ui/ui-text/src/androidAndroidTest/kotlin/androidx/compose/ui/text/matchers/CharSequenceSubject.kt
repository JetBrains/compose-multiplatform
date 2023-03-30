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

package androidx.compose.ui.text.matchers

import android.text.Spanned
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import kotlin.reflect.KClass

/**
 * Truth extension for CharSequence used for Span related checks.
 */
internal class CharSequenceSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: CharSequence?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<CharSequenceSubject?, CharSequence?> =
            Factory { failureMetadata, subject -> CharSequenceSubject(failureMetadata, subject) }
    }

    fun <T : Any> spans(spanClazz: KClass<out T>): SpanIterableSubject<T> {
        check("isNotNull()").that(subject).isNotNull()
        check("instanceOf()").that(subject).isInstanceOf(Spanned::class.java)
        val spanned = subject as Spanned
        val spans = spanned.getSpans(0, spanned.length, spanClazz.java).map {
            SpanInfo(it, spanned.getSpanStart(it), spanned.getSpanEnd(it), spanned.getSpanFlags(it))
        }
        return assertAbout(SpanIterableSubject.factory(spanClazz)).that(spans)!!
    }

    /**
     * Checks if the given text contains a span matching the given class and position.
     *
     * When [predicate] function is provided, for the span that is found, predicate will be called
     * to further validate the span object.
     *
     * @param spanClazz the class of the expected span
     * @param start start position of the expected span
     * @param end end position of the expected span
     * @param predicate function to further assert the span object
     */
    fun <T : Any> hasSpan(
        spanClazz: KClass<out T>,
        start: Int,
        end: Int,
        predicate: ((T) -> Boolean)? = null
    ) {
        spans(spanClazz).has(start, end, predicate)
    }

    fun <T : Any> doesNotHaveSpan(
        spanClazz: KClass<out T>,
    ) {
        spans(spanClazz).isEmpty()
    }

    /**
     * Similar to [hasSpan], and the returned matcher will also check that the span is not covered
     * by other spans.
     *
     * @param spanClazz the class of the expected span
     * @param start start position of the expected span
     * @param end end position of the expected span
     * @param predicate function to further assert the span object
     */
    fun <T : Any> hasSpanOnTop(
        spanClazz: KClass<out T>,
        start: Int,
        end: Int,
        predicate: ((T) -> Boolean)? = null
    ) {
        spans(spanClazz).hasOnTop(start, end, predicate)
    }
}

/**
 * Truth extension for a list of Spans.
 */
internal class SpanIterableSubject<T : Any> private constructor(
    failureMetadata: FailureMetadata?,
    private val subjects: List<SpanInfo<out T>>?,
    private val spanClazz: KClass<out T>
) : IterableSubject(failureMetadata, subjects) {

    companion object {
        fun <T : Any> factory(spanClazz: KClass<out T>): Factory<SpanIterableSubject<T>?,
            List<SpanInfo<T>>?> {
            return Factory { failureMetadata, subject ->
                SpanIterableSubject(
                    failureMetadata,
                    subject,
                    spanClazz
                )
            }
        }
    }

    /**
     * Checks if the spans contain a span matching the given position.
     *
     * When [predicate] function is provided, for the span that is found, predicate will be called
     * to further validate the span object.
     *
     * @param start start position of the expected span
     * @param end end position of the expected span
     * @param predicate function to further assert the span object
     */
    fun has(start: Int, end: Int, predicate: ((T) -> Boolean)? = null) {
        check("isNotNull()").that(subjects).isNotNull()
        check("isNotEmpty()").that(subjects).isNotEmpty()

        subjects!!.forEach { spanInfo ->
            if (spanInfo.start == start && spanInfo.end == end) {
                if (predicate == null || predicate.invoke(spanInfo.span)) return
            }
        }

        failWithActual(
            simpleFact(
                "Can't find span $spanClazz at [$start, $end] ${toString(predicate)}"
            )
        )
    }

    /**
     * Similar to [has] checks if the spans contain a span matching the given position and
     * also that span is not covered by other spans.
     *
     * @param start start position of the expected span
     * @param end end position of the expected span
     * @param predicate function to further assert the span object
     */
    fun hasOnTop(start: Int, end: Int, predicate: ((T) -> Boolean)? = null) {
        check("isNotNull()").that(subjects).isNotNull()
        check("isNotEmpty()").that(subjects).isNotEmpty()

        subjects!!.reversed().forEach { spanInfo ->
            if (spanInfo.start == start || spanInfo.end == end) {
                // Find the target span
                if (predicate == null || predicate.invoke(spanInfo.span)) return
            } else if (start in spanInfo.start until spanInfo.end ||
                end in (spanInfo.start + 1)..spanInfo.end
            ) {
                // Find a span covers the given range.
                // Impossible to find the target span on top.
                failWithActual(simpleFact("Span not on top $spanClazz at [$start, $end]"))
            }
        }

        failWithActual(
            simpleFact(
                "Can't find span $spanClazz at [$start, $end] ${toString(predicate)}"
            )
        )
    }

    private fun toString(predicate: Any?): String {
        return if (predicate != null) "with predicate" else ""
    }

    override fun actualCustomStringRepresentation(): String {
        if (subjects != null) {
            return "{" +
                subjects.joinToString(
                    separator = ", ",
                    transform = {
                        "${it.span::class.java.simpleName}[${it.start}, ${it.end}]"
                    }
                ) +
                "}"
        } else {
            return super.actualCustomStringRepresentation()
        }
    }
}

internal data class SpanInfo<T : Any>(val span: T, val start: Int, val end: Int, val flags: Int)