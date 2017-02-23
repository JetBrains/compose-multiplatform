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

import org.gradle.external.javadoc.internal.AbstractJavadocOptionFileOption;
import org.gradle.external.javadoc.internal.JavadocOptionFileWriterContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used to hold complex argument(s) to doclava
 */
public class DoclavaMultilineJavadocOptionFileOption extends
        AbstractJavadocOptionFileOption<List<List<String>>> {

    public DoclavaMultilineJavadocOptionFileOption(String option) {
        super(option, null);
    }

    public DoclavaMultilineJavadocOptionFileOption(String option, List<List<String>> value) {
        super(option, value);
    }

    @Override
    public void setValue(List<List<String>> value) {
        final List<List<String>> args = getValue();
        if (args == null) {
            super.setValue(new ArrayList<List<String>>(value));
        } else {
            args.addAll(value);
        }
    }

    public void add(List<String>... moreArgs) {
        final List<List<String>> args = getValue();
        if (args == null) {
            super.setValue(new ArrayList<List<String>>(Arrays.asList(moreArgs)));
        } else {
            args.addAll(Arrays.asList(moreArgs));
        }
    }

    @Override
    public void write(JavadocOptionFileWriterContext writerContext) throws IOException {
        final List<List<String>> args = getValue();
        if (args != null && !args.isEmpty()) {
            for (List<String> arg : args) {
                writerContext.writeOptionHeader(getOption());
                if (!arg.isEmpty()) {
                    final Iterator<String> iter = arg.iterator();
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
    }

    /**
     * @return a deep copy of the option
     */
    public DoclavaMultilineJavadocOptionFileOption duplicate() {
        final List<List<String>> value = getValue();
        final ArrayList<List<String>> valueCopy = new ArrayList<>(value.size());
        for (List<String> item : value) {
            valueCopy.add(new ArrayList<>(item));
        }
        return new DoclavaMultilineJavadocOptionFileOption(getOption(), valueCopy);
    }
}
