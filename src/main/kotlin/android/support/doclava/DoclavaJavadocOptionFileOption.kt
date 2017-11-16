/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.doclava

import org.gradle.external.javadoc.internal.AbstractJavadocOptionFileOption
import org.gradle.external.javadoc.internal.JavadocOptionFileWriterContext
import java.io.IOException
import java.util.ArrayList

//TODO: remove this once https://github.com/gradle/gradle/issues/2354 is fixed
class DoclavaJavadocOptionFileOption : AbstractJavadocOptionFileOption<Iterable<String>> {

    constructor(option: String) : super(option, null)

    constructor(option: String, value: Iterable<String>?) : super(option, value)

    @Throws(IOException::class)
    override fun write(writerContext: JavadocOptionFileWriterContext) {
        writerContext.writeOptionHeader(getOption())
        val args = getValue()
        if (args != null) {
            val iter = args.iterator()
            while (true) {
                writerContext.writeValue(iter.next())
                if (!iter.hasNext()) {
                    break
                }
                writerContext.write(" ")
            }
        }
        writerContext.newLine()
    }
    /**
     * @return a deep copy of the option
     */
    override fun duplicate(): DoclavaJavadocOptionFileOption {
        val value = getValue()
        val valueCopy: ArrayList<String>?
        if (value != null) {
            valueCopy = ArrayList()
            valueCopy += value
        } else {
            valueCopy = null
        }
        return DoclavaJavadocOptionFileOption(getOption(), valueCopy)
    }
}
