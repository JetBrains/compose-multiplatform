package android.support.checkapi;

import org.gradle.api.DefaultTask
import org.gradle.api.Nullable
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.process.ExecResult

/**
 * A task to invoke Doclava's ApiCheck tool.
 * <p>
 * By default, any API changes will be flagged as errors (strict mode). This
 * can be loosened to merely backwards compatibility checks using
 * {@link #configureAsBackwardsCompatCheck()}.
 */
@ParallelizableTask
public class CheckApiTask extends DefaultTask {

    // see external/doclava/src/com/google/doclava/Errors.java for error code meanings.

    // 2-6 are all the error represented added APIs.
    private static final def API_ADDITIONS = (2..6)

    // Everything past the addition errors except for:
    // 15: CHANGED_VOLATILE
    // 17: CHANGED_VALUE
    // 22: CHANGED_NATIVE
    // 27: REMOVED_FINAL
    private static final def API_CHANGES_AND_REMOVALS = (7..27) - [15, 17, 22, 27]

    // ApiCheck error types which will cause the build to fail
    // Basically, error on everything but
    // 15: CHANGED_VOLATILE
    // 17: CHANGED_VALUE
    // 22: CHANGED_NATIVE
    // 27: REMOVED_FINAL
    // But include the "catch all"
    // 1: PARSE_ERROR
    public static final def DEFAULT_CHECK_API_ERRORS = Collections.unmodifiableSet(
            ([1] + API_ADDITIONS + API_CHANGES_AND_REMOVALS) as Set
    )

    // Ones we want to emit warnings for.
    // 15: CHANGED_VOLATILE
    // 17: CHANGED_VALUE
    // 27: REMOVED_FINAL
    public static final def DEFAULT_CHECK_API_WARNINGS = Collections.unmodifiableSet(
            [15, 17, 27] as Set
    )

    // Ones to just to just ignore as they usually aren't useful.
    // 22: CHANGED_NATIVE
    public static final def DEFAULT_CHECK_API_HIDDEN = Collections.singleton(22)

    // ApiCheck error types for backwards compatiblity API checks which will cause the build to fail.
    // Allow additions, but not removals or changes, except for deprecation changes.
    // 24: CHANGED_DEPRECATED
    // But include the "catch all"
    // 1: PARSE_ERROR
    public static final def DEFAULT_CHECK_API_BACKWARDS_COMPAT_ERRORS = Collections.unmodifiableSet(
            ([1] + API_CHANGES_AND_REMOVALS - [24]) as Set
    )

    // Same as the normal warnings, but with deprecation added as a warning type.
    public static final def DEFAULT_CHECK_API_BACKWARDS_COMPAT_WARNINGS = Collections.unmodifiableSet(
            (DEFAULT_CHECK_API_WARNINGS + [24]) as Set
    )

    // Same as the normal hidden ones + all API addition errors.
    public static final def DEFAULT_CHECK_API_BACKWARDS_COMPAT_HIDDEN = Collections.unmodifiableSet(
            (DEFAULT_CHECK_API_HIDDEN + API_ADDITIONS) as Set
    )


    // Error messages shamelessly ripped from AOSP's check-api error message.
    // For these templates, the parameters are:
    // See #getOnFailMessage()
    // 1: oldApiFile.name
    // 2: oldRemovedApiFile.name
    // 3: updateApiTaskPath
    // 4: checkApiTaskPath
    private static final String DEFAULT_ERROR_MESSAGE_WITH_UPDATE_TASK =
            '''******************************
You have tried to change the API from what has been previously approved.

To make these errors go away, you have two choices:
   1) You can add "@hide" javadoc comments to the methods, etc. listed in the
      errors above.

   2) You can update %1$s and %2$s
      by executing the following command:
          ./gradlew %3$s

      To submit the revised %1$s and %2$s
      to the main repository, you will need approval.

   You can re-run just the API checks using the command:
       ./gradlew %4$s
******************************'''

    private static final String DEFAULT_ERROR_MESSAGE_WITHOUT_UPDATE_TASK =
            '''******************************
    You have tried to change the API from what has been previously approved.

    To make these errors go away you can add "@hide" javadoc comments to the methods, etc. listed
    in the errors above.

    You can re-run just the API checks using the command:
        ./gradlew %4$s
******************************'''

    private static final String DEFAULT_ERROR_MESSAGE_FOR_BACKWARDS_COMPAT =
            '''******************************
    You have tried to change the API from what has been previously released in
    an SDK.  Please fix the errors listed above.

    You can re-run just the API checks using the command:
        ./gradlew %4$s
******************************'''

    @InputFile
    File oldApiFile
    @InputFile
    File oldRemovedApiFile

    @InputFile
    File newApiFile
    @InputFile
    File newRemovedApiFile

    /**
     * If non-null, the list of packages to ignore any API checks on.<br>
     * Packages names will be matched exactly; sub-packages are not automatically recognized.
     */
    @Optional
    @Nullable
    @Input
    Collection ignoredPackages = null

    /**
     * If non-null, the list of classes to ignore any API checks on.<br>
     * Class names will be matched exactly by their full qualified names; inner classes are not
     * automatically recognized.
     */
    @Optional
    @Nullable
    @Input
    Collection ignoredClasses = null

    @InputFiles
    Collection<File> doclavaClasspath

    // A dummy output file meant only to tag when this check was last ran.
    // Without any outputs, Gradle will run this task every time.
    @Optional
    @Nullable
    private File mOutputFile = null;

    @OutputFile
    public File getOutputFile() {
        return mOutputFile ?: new File(project.buildDir, "checkApi/${name}-completed")
    }

    @Optional
    public void setOutputFile(File outputFile) {
        mOutputFile = outputFile
    }

    @Input
    Collection checkApiErrors = DEFAULT_CHECK_API_ERRORS

    @Input
    Collection checkApiWarnings = DEFAULT_CHECK_API_WARNINGS

    @Input
    Collection checkApiHidden = DEFAULT_CHECK_API_HIDDEN

    // The following are optional. They are only used for constructing the failure message.
    @Nullable
    @Optional
    String checkApiTaskPath;
    @Nullable
    @Optional
    String updateApiTaskPath;

    private String checkApiTaskPathToPrint() {
        return getCheckApiTaskPath() ?: this.path
    }

    private def mOnFailMessage

    public CheckApiTask() {
        group = 'Verification'
        description = 'Invoke Doclava\'s ApiCheck tool to make sure current.txt is up to date.'
    }

    private Set<File> collectAndVerifyInputs() {
        Set<File> apiFiles = [getOldApiFile(), getNewApiFile(), getOldRemovedApiFile(), getNewRemovedApiFile()] as Set
        if (apiFiles.size() != 4) {
            throw new InvalidUserDataException("""Conflicting input files:
    oldApiFile: ${getOldApiFile()}
    newApiFile: ${getNewApiFile()}
    oldRemovedApiFile: ${getOldRemovedApiFile()}
    newRemovedApiFile: ${getNewRemovedApiFile()}
All of these must be distinct files.""")
        }
        return apiFiles;
    }

    /**
     * Returns the preprocessed failure message.<br>
     * This string will be passed to {@link String#format(String, Object[])} as the format
     * string with the given parameters to get the failure message.<br>
     * The arguments used are:<br>
     * 1 (String): oldApiFile.name<br>
     * 2 (String): oldRemovedApiFile.name<br>
     * 3 (String): updateApiTaskPath<br>
     * 4 (String): checkApiTaskPath<br>
     * The format string need not use all, or even any, of these arguments.
     */
    public String getOnFailMessage() {
        return (mOnFailMessage == null ?
                (getUpdateApiTaskPath() == null ?
                        DEFAULT_ERROR_MESSAGE_WITHOUT_UPDATE_TASK :
                        DEFAULT_ERROR_MESSAGE_WITH_UPDATE_TASK
                ) : mOnFailMessage.toString())
    }

    /**
     * Returns the failure error message after all the arguments have been processed through
     * {@link String#format(String, Object[])}. This String is what will be used as the
     * error mesage upon failure.<br>
     * See {@link #getOnFailMessage()} for how the arguments are evaluated.
     */
    public String getOnFailMessageFormatted() {
        return String.format(getOnFailMessage(),
                getOldApiFile().name,
                getOldRemovedApiFile().name,
                getUpdateApiTaskPath(),
                getCheckApiTaskPath())
    }

    /**
     * Sets the preprocessed failure message.<br>
     * The given string will be passed to {@link String#format(String, Object[])} as the format
     * string with the given parameters to get the final failure message.<br>
     * The arguments used are:<br>
     * 1 (String): oldApiFileName<br>
     * 2 (String): oldRemovedApiFileName<br>
     * 3 (String): updateApiTaskPath<br>
     * 4 (String): checkApiTaskPath<br>
     * The format string need not use all, or even any, of these arguments.
     */
    public void setOnFailMessage(Object onFailMessage) {
        mOnFailMessage = onFailMessage
    }

    /**
     * Configures this CheckApiTask with reasonable defaults for backwards compatibility checks,
     * which are a bit looser than the normal defaults of erroring on any changes.<br>
     * In particular, this will cause the api check to allow additions of new APIs, though removals
     * and changes of existing APIs will still be marked as errors.<p>
     *
     * Please note that this will set several properties of this task, overwriting any values they
     * may already be set to. This method is meant to be called first thing when configuring this CheckApiTask.
     */
    public void configureAsBackwardsCompatCheck() {
        checkApiErrors = DEFAULT_CHECK_API_BACKWARDS_COMPAT_ERRORS
        checkApiWarnings = DEFAULT_CHECK_API_BACKWARDS_COMPAT_WARNINGS
        checkApiHidden = DEFAULT_CHECK_API_BACKWARDS_COMPAT_HIDDEN
        mOnFailMessage = DEFAULT_ERROR_MESSAGE_FOR_BACKWARDS_COMPAT
    }

    public void setCheckApiErrors(Collection errors) {
        // Make it serializable.
        checkApiErrors = errors as int[]
    }

    public void setCheckApiWarnings(Collection warnings) {
        // Make it serializable.
        checkApiWarnings = warnings as int[]
    }

    public void setCheckApiHidden(Collection hidden) {
        // Make it serializable.
        checkApiHidden = hidden as int[]
    }

    @TaskAction
    public void exec() {
        // TODO(csyoung) Option to run this within the build JVM rather than always fork?
        final def apiFiles = collectAndVerifyInputs()
        // TODO(csyoung) Right now, it is difficult to get the exit code of an ExecTask (including
        // JavaExec), and it is also difficult to have a custom error message on failure. But it is
        // easy to get the exit code with Project#javaexec.
        // If either of those gets tweaked, then this should be refactored to extend JavaExec.
        ExecResult result = project.javaexec {
            // Put Doclava on the classpath so we can get the ApiCheck class.
            classpath(getDoclavaClasspath())
            main = 'com.google.doclava.apicheck.ApiCheck'

            minHeapSize = '128m'
            maxHeapSize = '1024m'

            // [other options] old_api.txt new_api.txt old_removed_api.txt new_removed_api.txt

            // add -error LEVEL for every error level we want to fail the build on.
            getCheckApiErrors().each { args('-error', it) }
            getCheckApiWarnings().each { args('-warning', it) }
            getCheckApiHidden().each { args('-hide', it) }

            Collection ignoredPackages = getIgnoredPackages()
            if (ignoredPackages) {
                ignoredPackages.each { args('-ignorePackage', it) }
            }
            Collection ignoredClasses = getIgnoredClasses()
            if (ignoredClasses) {
                ignoredClasses.each { args('-ignoreClass', it) }
            }

            args(apiFiles.collect( { it.absolutePath } ))

            // We will be handling failures ourselves with a custom message.
            ignoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw new GradleException(getOnFailMessageFormatted())
        }

        // Just create a dummy file upon completion. Without any outputs, Gradle will run this task
        // every time.
        File outputFile = getOutputFile()
        outputFile.parentFile.mkdirs()
        outputFile.createNewFile()
    }
}