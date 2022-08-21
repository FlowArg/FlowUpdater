package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

/**
 * Represent some settings for FlowUpdater
 *
 * @author FlowArg
 */
public class UpdaterOptions
{
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, new ExternalFileDeleter(), true);

    private final boolean silentRead;
    private final ExternalFileDeleter externalFileDeleter;
    private final boolean versionChecker;

    private UpdaterOptions(boolean silentRead, ExternalFileDeleter externalFileDeleter, boolean versionChecker)
    {
        this.silentRead = silentRead;
        this.externalFileDeleter = externalFileDeleter;
        this.versionChecker = versionChecker;
    }

    /**
     * Disable some debug logs on Minecraft JSON's parsing.
     * Default: true
     * @return silentRead value.
     */
    public boolean isSilentRead()
    {
        return this.silentRead;
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
     * Builder of {@link UpdaterOptions}
     */
    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<Boolean> silentReadArgument = new BuilderArgument<>("SilentRead", () -> true).optional();
        private final BuilderArgument<ExternalFileDeleter> externalFileDeleterArgument = new BuilderArgument<>("External FileDeleter", ExternalFileDeleter::new).optional();
        private final BuilderArgument<Boolean> versionChecker = new BuilderArgument<>("VersionChecker", () -> true).optional();

        /**
         * Enable or disable the silent read option.
         * @param silentRead the value to define.
         * @return the builder.
         */
        public UpdaterOptionsBuilder withSilentRead(boolean silentRead)
        {
            this.silentReadArgument.set(silentRead);
            return this;
        }

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
         * Build an {@link UpdaterOptions} object.
         */
        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(
                    this.silentReadArgument.get(),
                    this.externalFileDeleterArgument.get(),
                    this.versionChecker.get()
            );
        }
    }
}
