package android.support.doclava;

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Nullable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask

@ParallelizableTask
public class DoclavaTask extends Javadoc {

    // external/doclava/src/com/google/doclava/Errors.java
    public static final def DEFAULT_DOCLAVA_ERRORS = Collections.unmodifiableSet([
            101,    // unresolved link
            103,    // unknown tag
            104,    // unknown param name
    ] as Set)

    public static final def DEFAULT_DOCLAVA_WARNINGS = Collections.unmodifiableSet([
            121,    // hidden type param
    ] as Set)


    public static final def DEFAULT_DOCLAVA_HIDDEN = Collections.unmodifiableSet([
            111,    // hidden super class
            113,    // @deprecation mismatch
    ] as Set)


    // All lowercase name to match MinimalJavadocOptions#docletpath
    private Collection<File> mDocletpath

    // doclava error types which will cause the build to fail
    @Input
    Collection doclavaErrors = DEFAULT_DOCLAVA_ERRORS
    @Input
    Collection doclavaWarnings = DEFAULT_DOCLAVA_WARNINGS
    // spammy doclava warnings which we want to hide
    @Input
    Collection doclavaHidden = DEFAULT_DOCLAVA_HIDDEN

    /**
     * If non-null, the list of packages that will be treated as if they were
     * marked with {@literal @hide}.<br>
     * Packages names will be matched exactly; sub-packages are not automatically recognized.
     */
    @Optional
    @Nullable
    @Input
    Collection hiddenPackages = null

    /**
     * If non-null and not-empty, the whitelist of packages that will be present in the generated
     * stubs; if null or empty, then all packages have stubs generated.<br>
     * Wildcards are accepted.
     */
    @Optional
    @Nullable
    @Input
    Set<String> stubPackages = null

    @Input
    boolean generateDocs = true

    /**
     * If non-null, the location of where to place the generated api file.
     * If this is non-null, then {@link #removedApiFile} must be non-null as well.
     */
    @Optional
    @Nullable
    @OutputFile
    File apiFile = null

    /**
     * If non-null, the location of where to place the generated removed api file.
     * If this is non-null, then {@link #apiFile} must be non-null as well.
     */
    @Optional
    @Nullable
    @OutputFile
    File removedApiFile = null

    /**
     * If non-null, the location of the generated keep list.
     */
    @Optional
    @Nullable
    @OutputFile
    File keepListFile = null

    /**
     * If non-null, the location to put the generated stub sources.
     */
    @Optional
    @Nullable
    @OutputDirectory
    File stubsDir = null

    public DoclavaTask() {
        failOnError = true
        options.doclet = "com.google.doclava.Doclava"
        options.encoding("UTF-8")
        options.quiet()
        // doclava doesn't understand '-doctitle'
        title = null
        maxMemory = "1280m"
        // TODO(csyoung) Some way to override this?
        // If none of generateDocs, apiFile, keepListFile, or stubJarsDir are true, then there is no work to do.
        onlyIf( { getGenerateDocs() ||
                (getApiFile() != null && getRemovedApiFile() != null) ||
                getKeepListFile() != null ||
                getStubsDir() != null } )
    }

    /**
     * The doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#getDocletpath()
     */
    @InputFiles
    public Collection<File> getDocletpath() {
        return mDocletpath
    }

    /**
     * Sets the doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#setDocletpath(java.util.List)
     */
    public void setDocletpath(Collection<File> docletpath) {
        mDocletpath = docletpath
        // Go ahead and keep the docletpath in our JavadocOptions object in sync.
        options.docletpath = docletpath as List
    }

    public void setDoclavaErrors(Collection errors) {
        // Make it serializable.
        doclavaErrors = errors as int[]
    }

    public void setDoclavaWarnings(Collection warnings) {
        // Make it serializable.
        doclavaWarnings = warnings as int[]
    }

    public void setDoclavaHidden(Collection hidden) {
        // Make it serializable.
        doclavaHidden = hidden as int[]
    }

    private static boolean verifyAndGetGenerateApiFiles(File apiFile, File removedApiFile) {
        if (apiFile == null) {
            if (removedApiFile == null) {
                return false
            } else {
                throw new InvalidUserDataException('removedApiFile specified but not apiFile')
            }
        } else {
            return true
        }
    }

    /**
     * "Configures" this DoclavaTask with parameters that might not be at their final values
     * until this task is run.
     */
    private configureDoclava() {
        options.docletpath = getDocletpath() as List
        // configure doclava error/warning/hide levels
        options.addOption(new DoclavaMultilineJavadocOptionFileOption('hide'))
                .setValue(getDoclavaHidden().collect({[it.toString()]}))
        options.addOption(new DoclavaMultilineJavadocOptionFileOption('warning'))
                .setValue(getDoclavaWarnings().collect({[it.toString()]}))
        options.addOption(new DoclavaMultilineJavadocOptionFileOption('error'))
                .setValue(getDoclavaErrors().collect({[it.toString()]}))

        Collection hiddenPackages = getHiddenPackages()
        if (hiddenPackages) {
            options.addOption(new DoclavaMultilineJavadocOptionFileOption('hidePackage'))
                    .setValue(hiddenPackages.collect({[it.toString()]}))
        }

        if (!getGenerateDocs()) {
            options.addOption(new DoclavaJavadocOptionFileOption('nodocs'))
        }
        // If requested, generate the api files.
        File apiFile = getApiFile()
        File removedApiFile = getRemovedApiFile()
        if (verifyAndGetGenerateApiFiles(apiFile, removedApiFile)) {
            options.addStringOption('api', apiFile.absolutePath)
            options.addStringOption('removedApi', removedApiFile.absolutePath)
        }
        // If requested, generate the keep list.
        File keepListFile = getKeepListFile()
        if (keepListFile != null) {
            options.addStringOption('proguard', keepListFile.absolutePath)
        }
        // If requested, generate stubs.
        File stubsDir = getStubsDir()
        if (stubsDir != null) {
            options.addStringOption('stubs', stubsDir.absolutePath)
            Set<String> stubPackages = getStubPackages()
            if (stubPackages) {
                options.addStringOption('stubpackages', stubPackages.join(':'))
            }
        }
    }

    @Override
    public void generate() {
        configureDoclava();
        super.generate();
    }
}