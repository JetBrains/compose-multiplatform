package org.jetbrains.compose.codeeditor.platform;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.compose.codeeditor.platform.api.CodeCompletionElement;
import org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationData;
import org.jetbrains.compose.codeeditor.platform.api.Platform;
import org.jetbrains.compose.codeeditor.platform.api.Project;
import org.jetbrains.compose.codeeditor.platform.impl.IntellijPlatformWrapper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WrapperExtension implements BeforeAllCallback, AfterEachCallback, ExtensionContext.Store.CloseableResource {

    static Platform platform;
    static Path tempDir;
    private static final Object lock = new Object();

    @Override
    public void close() {
        platform.stop();
        FileUtil.delete(tempDir.toFile());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        synchronized (lock) {
            if (platform == null) {
                try {
                    tempDir = Files.createTempDirectory("ipwTest");
                    FileUtil.copyDir(Path.of("testData").toFile(), tempDir.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                platform = new IntellijPlatformWrapper();
                platform.init();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var openProjects = ProjectManager.getInstance().getOpenProjects();
        assertEquals(0, openProjects.length, "Open projects between tests");
    }

    static void replaceInFile(String file, String target, String replacement) throws IOException {
        var path = Path.of(file);
        String code = Files.readString(path);
        code = code.replace(target, replacement);
        Files.writeString(path, code, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void assertCodeCompletion(Project project, String file,
                                     List<String> include) {
        assertCodeCompletion(project, file, include, null);
    }

    static void assertCodeCompletion(Project project, String file, String projectDir,
                                     List<String> include) {
        assertCodeCompletion(project, file, projectDir, include, null);
    }

    static void assertCodeCompletion(Project project, String file,
                                     List<String> include, List<String> exclude) {
        assertCodeCompletion(project, file, include, exclude, false);
    }

    static void assertCodeCompletion(Project project, String file, String projectDir,
                                     List<String> include, List<String> exclude) {
        assertCodeCompletion(project, file, projectDir, include, exclude, false);
    }

    static void assertCodeCompletion(Project project, String file,
                                     List<String> include, boolean sorted) {
        assertCodeCompletion(project, file, Path.of(file), include, null, sorted);
    }

    static void assertCodeCompletion(Project project, String file,
                                     List<String> include, List<String> exclude, boolean sorted) {
        assertCodeCompletion(project, file, Path.of(file), include, exclude, sorted);
    }

    static void assertCodeCompletion(Project project, String file, String projectDir,
                                     List<String> include, List<String> exclude, boolean sorted) {
        assertCodeCompletion(project, file, Path.of(projectDir, file), include, exclude, sorted);
    }

    static void assertCodeCompletion(Project project, String file, Path filePath,
                                     List<String> include, List<String> exclude, boolean sorted) {
        var elements = project.getCodeCompletion(file, getCaretOffset(filePath));
        if (sorted) {
            sortCheck(elements, include);
        } else {
            check(elements, include, exclude);
        }
    }

    private static void check(List<CodeCompletionElement> list, List<String> include, List<String> exclude) {
        List<String> names = list.stream().map(CodeCompletionElement::getName).collect(Collectors.toList());
        if (include != null) {
            assertTrue(
                include.stream().allMatch(it -> names.stream().anyMatch(it::equals)),
                "\nCode completion list: \n" + names + "\n must include: \n" + include + "\n"
            );
        }
        if (exclude != null) {
            assertTrue(
                exclude.stream().allMatch(it -> names.stream().noneMatch(it::equals)),
                "\nCode completion list: \n" + names + "\n must NOT include: \n" + exclude + "\n"
            );
        }
    }

    private static void sortCheck(List<CodeCompletionElement> list, List<String> sortedList) {
        List<String> names = list.stream().map(CodeCompletionElement::getName).collect(Collectors.toList());
        boolean fail = false;
        for (int i = 0; i < sortedList.size(); i++) {
            if (!sortedList.get(i).equals(names.get(i))) {
                fail = true;
                break;
            }
        }
        assertFalse(fail,
            "\nCode completion list: \n" + names + "\n doesn't start with list: \n" + sortedList + "\n");
    }

    static void assertLocalGotoDeclaration(Project project, String file) {
        assertGotoDeclaration(project, file, Path.of(file), WrapperExtension::localGotoDeclarationTestData);
    }

    static void assertGotoDeclaration(Project project, String file, String projectDir) {
        assertGotoDeclaration(project, file, Path.of(projectDir, file), WrapperExtension::gotoDeclarationTestData);
    }

    private static void assertGotoDeclaration(Project project, String file, Path filePath,
                                              Function<Path, List<GTDTestData>> testDataProvider) {
        List<GTDTestData> data = testDataProvider.apply(filePath);
        for (GTDTestData datum : data) {
            GotoDeclarationData gtdData = project.gotoDeclaration(file, datum.offset);

            assertFalse(gtdData.isIndexNotReady(), datum.elementType + ": index is not ready");

            var navigatable = datum.expectedTargetOffset != 0;
            assertEquals(navigatable, gtdData.canNavigate(),
                datum.elementType + ": canNavigate doesn't return expected navigatable state");
            if (!gtdData.canNavigate()) continue;

            var targets = gtdData.getTargets();
            assertEquals(1, targets.size(), datum.elementType + ": more than one declaration");

            var target = targets.iterator().next();
            var targetPath = target.getPath();
            var targetOffset = target.getOffset();

            var expectedTargetFile = datum.expectedTargetFile != null
                ? datum.expectedTargetFile
                : filePath.getFileName().toString();
            assertEquals(expectedTargetFile, Path.of(targetPath).getFileName().toString(),
                datum.elementType + ": target path doesn't equal expected file path");

            assertEquals(datum.expectedTargetOffset, targetOffset,
                datum.elementType + ": target offset is incorrect");

            if (datum.initialElementStartOffset != -1) {
                assertTrue(gtdData.isInitialElementOffsetSet(),
                    datum.elementType + ": initial element offset was not set");

                assertEquals(datum.initialElementStartOffset, gtdData.getInitialElementStartOffset(),
                    datum.elementType + ": initial element start offset isn't equal to the expected value");

                assertEquals(datum.initialElementEndOffset, gtdData.getInitialElementEndOffset(),
                    datum.elementType + ": initial element end offset isn't equal to the expected value");
            }
        }
    }

    private static final Pattern caretPattern = Pattern.compile("// caret: (\\d+)");

    private static int getCaretOffset(Path file) {
        try {
            var code = Files.readString(file);
            var matcher = caretPattern.matcher(code);
            matcher.find();
            return Integer.parseInt(matcher.group(1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Pattern localGotoDeclarationPattern = Pattern.compile("// \\((.*)\\) (\\d+) (\\d+) \\((\\d+) (\\d+)\\)");
    private static final List<Function<Matcher, String>> localGotoDeclarationPatternMapper = List.of(
        matcher -> matcher.group(1),
        matcher -> matcher.group(2),
        matcher -> null,
        matcher -> matcher.group(3),
        matcher -> matcher.group(4),
        matcher -> matcher.group(5)
    );

    private static List<GTDTestData> localGotoDeclarationTestData(Path file) {
        return getGotoDeclarationTestData(file, localGotoDeclarationPattern, localGotoDeclarationPatternMapper);
    }

    private static final Pattern gotoDeclarationPattern = Pattern.compile("// \\((.*)\\) (\\d+) \\((.*)\\) (\\d+)");
    private static final List<Function<Matcher, String>> gotoDeclarationPatternMapper = List.of(
        matcher -> matcher.group(1),
        matcher -> matcher.group(2),
        matcher -> matcher.group(3),
        matcher -> matcher.group(4),
        matcher -> "-1",
        matcher -> "-1"
    );

    private static List<GTDTestData> gotoDeclarationTestData(Path file) {
        return getGotoDeclarationTestData(file, gotoDeclarationPattern, gotoDeclarationPatternMapper);
    }

    private static List<GTDTestData> getGotoDeclarationTestData(
        Path file,
        Pattern pattern,
        List<Function<Matcher, String>> mapper
    ) {
        try {
            var code = Files.readString(file);
            var matcher = pattern.matcher(code);
            var data = new ArrayList<GTDTestData>();
            while (matcher.find()) {
                data.add(new GTDTestData(
                    mapper.get(0).apply(matcher),
                    Integer.parseInt(mapper.get(1).apply(matcher)),
                    mapper.get(2).apply(matcher),
                    Integer.parseInt(mapper.get(3).apply(matcher)),
                    Integer.parseInt(mapper.get(4).apply(matcher)),
                    Integer.parseInt(mapper.get(5).apply(matcher))
                ));
            }
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class GTDTestData {
        String elementType;
        int offset;
        String expectedTargetFile;
        int expectedTargetOffset;
        int initialElementStartOffset;
        int initialElementEndOffset;

        private GTDTestData(String elementType, int offset, String expectedTargetFile, int expectedTargetOffset,
                            int initialElementStartOffset, int initialElementEndOffset) {
            this.elementType = elementType;
            this.offset = offset;
            this.expectedTargetFile = expectedTargetFile;
            this.expectedTargetOffset = expectedTargetOffset;
            this.initialElementStartOffset = initialElementStartOffset;
            this.initialElementEndOffset = initialElementEndOffset;
        }
    }
}
