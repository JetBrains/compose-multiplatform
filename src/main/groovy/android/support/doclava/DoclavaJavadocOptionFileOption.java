/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.doclava;

import org.gradle.external.javadoc.JavadocOptionFileOption;
import org.gradle.external.javadoc.internal.JavadocOptionFileWriterContext;

import java.io.IOException;
import java.util.Iterator;

/**
 * This class is used to hold complex argument(s) to doclava
 */
public class DoclavaJavadocOptionFileOption implements JavadocOptionFileOption<Iterable<String>> {
    private final String option;
    private Iterable<String> args;

    public DoclavaJavadocOptionFileOption(String option) {
        this.option = option;
    }

    public Iterable<String> getValue() {
        return args;
    }

    public void setValue(Iterable<String> args) {
        this.args = args;
    }

    public String getOption() {
        return option;
    }

    public void write(JavadocOptionFileWriterContext writerContext) throws IOException {
        writerContext.writeOptionHeader(getOption());
        if (args != null) {
            Iterator<String> iter = args.iterator();
            while (true) {
                writerContext.writeValue(iter.next());
                if (!iter.hasNext()) {
                    break;
                }
                writerContext.write(" ");
            }
        }
        writerContext.newLine();
    }
}