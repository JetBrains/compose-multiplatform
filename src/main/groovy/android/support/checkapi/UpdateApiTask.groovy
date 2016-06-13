package android.support.checkapi;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.WorkResult

public class UpdateApiTask extends DefaultTask {
    @InputFile
    File newApiFile
    @InputFile
    File newRemovedApiFile

    @OutputFile
    File oldApiFile
    @OutputFile
    File oldRemovedApiFile

    private WorkResult copyFromToFile(File src, File dest) {
        return project.copy {
            from src
            into dest.parent
            rename { dest.name }
        }
    }

    @TaskAction
    public void doUpdate() {
        copyFromToFile(getNewApiFile(), getOldApiFile())
        copyFromToFile(getNewRemovedApiFile(), getOldRemovedApiFile())
        project.logger.warn("Updated ${getOldApiFile().name} and ${getOldRemovedApiFile().name} API files.")
    }
}