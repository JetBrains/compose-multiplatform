
/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.build.dependencyTracker
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ProjectGraphTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun testSimple() {
        val tmpDir = tmpFolder.root
        val root = ProjectBuilder.builder()
                .withProjectDir(tmpDir)
                .withName("root")
                .build()
        // Project Graph expects supportRootFolder.
        (root.properties.get("ext") as ExtraPropertiesExtension).set("supportRootFolder", tmpDir)
        val p1 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p1"))
                .withName("p1")
                .withParent(root)
                .build()
        val p2 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p2"))
                .withName("p2")
                .withParent(root)
                .build()
        val p3 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p1").resolve("p3"))
                .withName("p3")
                .withParent(p1)
                .build()
        val graph = ProjectGraph(root)
        assertNull(graph.findContainingProject("nowhere"))
        assertNull(graph.findContainingProject("rootfile.java"))
        assertEquals(
                p1,
                graph.findContainingProject("p1/px/x.java".toLocalPath())
        )
        assertEquals(
                p1,
                graph.findContainingProject("p1/a.java".toLocalPath())
        )
        assertEquals(
                p3,
                graph.findContainingProject("p1/p3/a.java".toLocalPath())
        )
        assertEquals(
                p2,
                graph.findContainingProject("p2/a/b/c/d/e/f/a.java".toLocalPath())
        )
    }
    private fun String.toLocalPath() = this.split("/").joinToString(File.separator)
}
