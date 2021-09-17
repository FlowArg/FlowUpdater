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
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, false, new ExternalFileDeleter());

    private final boolean silentRead;
    private final boolean downloadServer;
    private final ExternalFileDeleter externalFileDeleter;

    private UpdaterOptions(boolean silentRead, boolean downloadServer, ExternalFileDeleter externalFileDeleter)
    {
        this.silentRead = silentRead;
        this.downloadServer = downloadServer;
        this.externalFileDeleter = externalFileDeleter;
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
     * If this option is set to true, {@link fr.flowarg.flowupdater.FlowUpdater} will download the Minecraft Server.
     * Default: false
     * @return downloadServer value.
     */
    public boolean isDownloadServer()
    {
        return this.downloadServer;
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

    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<Boolean> silentReadArgument = new BuilderArgument<>("SilentRead", () -> true).optional();
        private final BuilderArgument<Boolean> downloadServerArgument = new BuilderArgument<>("DownloadServer", () -> false).optional();
        private final BuilderArgument<ExternalFileDeleter> externalFileDeleterArgument = new BuilderArgument<>("External FileDeleter", ExternalFileDeleter::new).optional();

        public UpdaterOptionsBuilder withSilentRead(boolean silentRead)
        {
            this.silentReadArgument.set(silentRead);
            return this;
        }

        @Deprecated
        public UpdaterOptionsBuilder withDownloadServer(boolean downloadServer)
        {
            this.downloadServerArgument.set(downloadServer);
            return this;
        }

        public UpdaterOptionsBuilder withExternalFileDeleter(ExternalFileDeleter externalFileDeleter)
        {
            this.externalFileDeleterArgument.set(externalFileDeleter);
            return this;
        }

        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(
                    this.silentReadArgument.get(),
                    this.downloadServerArgument.get(),
                    this.externalFileDeleterArgument.get()
            );
        }
    }
}
