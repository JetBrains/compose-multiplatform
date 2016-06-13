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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used to hold complex argument(s) to doclava
 */
public class DoclavaMultilineJavadocOptionFileOption implements JavadocOptionFileOption<List<List<String>>> {
    private final String option;
    private List<List<String>> args;

    public DoclavaMultilineJavadocOptionFileOption(String option) {
        this.option = option;
    }

    public List<List<String>> getValue() {
        return args;
    }

    public void setValue(List<List<String>> value) {
        if (this.args == null) {
            this.args = new ArrayList<List<String>>(value.size());
        }
        this.args.addAll(value);
    }

    public void add(List<String>... moreArgs) {
        if (this.args == null) {
            this.args = new ArrayList<List<String>>(moreArgs.length);
        }
        for (List<String> arg : moreArgs) {
            this.args.add(arg);
        }
    }

    public String getOption() {
        return option;
    }

    public void write(JavadocOptionFileWriterContext writerContext) throws IOException {
        if (args != null && !args.isEmpty()) {
            for (List<String> arg : args) {
                writerContext.writeOptionHeader(getOption());
                if (!arg.isEmpty()) {
                    Iterator<String> iter = arg.iterator();
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
}