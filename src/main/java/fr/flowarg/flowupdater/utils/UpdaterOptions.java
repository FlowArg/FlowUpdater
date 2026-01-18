package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.nio.file.Paths;

/**
 * Represent some settings for FlowUpdater
 *
 * @author FlowArg
 */
public class UpdaterOptions
{
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(new ExternalFileDeleter(), true, System.getProperty("java.home") != null ? Paths.get(System.getProperty("java.home"))
            .resolve("bin")
            .resolve("java")
            .toAbsolutePath()
            .toString() : "java"
            , false);

    private final ExternalFileDeleter externalFileDeleter;
    private final boolean versionChecker;
    private final String javaPath;
    private final boolean disableExtFilesAsyncDownload;

    private UpdaterOptions(ExternalFileDeleter externalFileDeleter, boolean versionChecker, String javaPath, boolean disableExtFilesAsyncDownload)
    {
        this.externalFileDeleter = externalFileDeleter;
        this.versionChecker = versionChecker;
        this.javaPath = javaPath;
        this.disableExtFilesAsyncDownload = disableExtFilesAsyncDownload;
    }

    /**
     * The external file deleter is used to check if some external files need to be downloaded.
     * Default: {@link fr.flowarg.flowupdater.utils.ExternalFileDeleter}
     * @return externalFileDeleter value.
     */
    public ExternalFileDeleter getExternalFileDeleter()
    {
        return this.externalFileDeleter;
    }

    /**
     * Should check the version of FlowUpdater.
     * @return true or false.
     */
    public boolean isVersionChecker()
    {
        return this.versionChecker;
    }

    /**
     * The path to the java executable to use with Forge and Fabric installers.
     * By default, it's taken from System.getProperty("java.home").
     * @return the path to the java executable.
     */
    public String getJavaPath()
    {
        return this.javaPath;
    }

    /**
     * If set to true, external files will be downloaded 1 by 1 (as it always been). Asynchronous downloading of
     * external files has been introduced in 1.9.4 in order to fasten the downloading step when a project
     * needs many external files.
     * @return true if asynchronous downloading of external files is disabled. False otherwise.
     */
    public boolean shouldDisableExtFilesAsyncDownload()
    {
        return this.disableExtFilesAsyncDownload;
    }

    /**
     * Builder of {@link UpdaterOptions}
     */
    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<ExternalFileDeleter> externalFileDeleterArgument = new BuilderArgument<>("External FileDeleter", ExternalFileDeleter::new).optional();
        private final BuilderArgument<Boolean> versionChecker = new BuilderArgument<>("VersionChecker", () -> true).optional();
        private final BuilderArgument<String> javaPath = new BuilderArgument<>("JavaPath", () ->
                System.getProperty("java.home") != null ? Paths.get(System.getProperty("java.home"))
                        .resolve("bin")
                        .resolve("java")
                        .toAbsolutePath()
                        .toString() : "java")
                .optional();
        private final BuilderArgument<Boolean> disableExtFilesAsyncDownload = new BuilderArgument<>("DisableExtFilesAsyncDownload", () -> false).optional();

        /**
         * Append an {@link ExternalFileDeleter} object.
         * @param externalFileDeleter the file deleter to define.
         * @return the builder.
         */
        public UpdaterOptionsBuilder withExternalFileDeleter(ExternalFileDeleter externalFileDeleter)
        {
            this.externalFileDeleterArgument.set(externalFileDeleter);
            return this;
        }

        /**
         * Enable or disable the version checker.
         * @param versionChecker the value to define.
         * @return the builder.
         */
        public UpdaterOptionsBuilder withVersionChecker(boolean versionChecker)
        {
            this.versionChecker.set(versionChecker);
            return this;
        }

        /**
         * Set the path to the java executable to use with Forge and Fabric installers.
         * (Directly the java executable, not the java home)
         * If you wish to set up the java home, you should use the {@link System#setProperty(String, String)} method
         * with the "java.home" key.
         * By default, it's taken from {@code System.getProperty("java.home")}.
         * @param javaPath the path to the java executable.
         * @return the builder.
         */
        public UpdaterOptionsBuilder withJavaPath(String javaPath)
        {
            this.javaPath.set(javaPath);
            return this;
        }

        /**
         * Disable asynchronous downloading of external files. See {@link UpdaterOptions#shouldDisableExtFilesAsyncDownload()} for more information.
         * @param disableExtFilesAsyncDownload true to disable asynchronous downloading of external files. False otherwise.
         * @return the builder.
         */
        public UpdaterOptionsBuilder withDisableExtFilesAsyncDownload(boolean disableExtFilesAsyncDownload)
        {
            this.disableExtFilesAsyncDownload.set(disableExtFilesAsyncDownload);
            return this;
        }

        /**
         * Build an {@link UpdaterOptions} object.
         */
        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(
                    this.externalFileDeleterArgument.get(),
                    this.versionChecker.get(),
                    this.javaPath.get(),
                    this.disableExtFilesAsyncDownload.get()
            );
        }
    }
}
