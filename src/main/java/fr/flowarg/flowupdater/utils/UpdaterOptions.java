package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.util.Random;

/**
 * Represent some settings for FlowUpdater
 *
 * @author flow
 */
public class UpdaterOptions
{
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, false, false, false, false, new Random().nextInt(2) + 2, new ExternalFileDeleter());

    /**
     * Is the read silent
     */
    private final boolean silentRead;

    /**
     * Is the server must be downloaded
     */
    private final boolean downloadServer;

    /**
     * Re-extract natives at each updates ?
     */
    private final boolean reExtractNatives;

    /**
     * Enable CurseForgePlugin (CPF) ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE CURSE_FORGE !!
     */
    private final boolean enableCurseForgePlugin;

    /**
     * Enable OptifineDownloaderPlugin (ODP) ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE OPTIFINE !!
     */
    private final boolean enableOptifineDownloaderPlugin;

    private final int nmbrThreadsForAssets;

    private final IFileDeleter externalFileDeleter;

    private UpdaterOptions(boolean silentRead, boolean reExtractNatives, boolean enableCurseForgePlugin, boolean enableOptifineDownloaderPlugin, boolean downloadServer, int nmbrThreadsForAssets, IFileDeleter externalFileDeleter)
    {
        this.silentRead = silentRead;
        this.reExtractNatives = reExtractNatives;
        this.enableCurseForgePlugin = enableCurseForgePlugin;
        this.enableOptifineDownloaderPlugin = enableOptifineDownloaderPlugin;
        this.downloadServer = downloadServer;
        this.nmbrThreadsForAssets = nmbrThreadsForAssets;
        this.externalFileDeleter = externalFileDeleter;
    }

    public boolean isSilentRead()
    {
        return this.silentRead;
    }

    public boolean isDownloadServer()
    {
        return this.downloadServer;
    }

    public boolean isReExtractNatives()
    {
        return this.reExtractNatives;
    }

    public boolean isEnableCurseForgePlugin()
    {
        return this.enableCurseForgePlugin;
    }

    public boolean isEnableOptifineDownloaderPlugin()
    {
        return this.enableOptifineDownloaderPlugin;
    }

    public int getNmbrThreadsForAssets()
    {
        return this.nmbrThreadsForAssets;
    }

    public IFileDeleter getExternalFileDeleter()
    {
        return this.externalFileDeleter;
    }

    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<Boolean> silentReadArgument = new BuilderArgument<>("SilentRead", () -> true).optional();
        private final BuilderArgument<Boolean> reExtractNativesArgument = new BuilderArgument<>("ReExtractingNatives", () -> false).optional();
        private final BuilderArgument<Boolean> enableCurseForgePluginArgument = new BuilderArgument<>("EnableCurseForgePlugin", () -> false).optional();
        private final BuilderArgument<Boolean> enableOptifineDownloaderPluginArgument = new BuilderArgument<>("EnableOptifineDownloaderPlugin", () -> false).optional();
        private final BuilderArgument<Boolean> downloadServerArgument = new BuilderArgument<>("DownloadServer", () -> false).optional();
        private final BuilderArgument<Integer> nmbrThreadsForAssetsArgument = new BuilderArgument<>("Number of Threads for assets", () -> 2).optional();
        private final BuilderArgument<IFileDeleter> externalFileDeleterArgument = new BuilderArgument<IFileDeleter>("External FileDeleter", ExternalFileDeleter::new).optional();

        public UpdaterOptionsBuilder withSilentRead(boolean silentRead)
        {
            this.silentReadArgument.set(silentRead);
            return this;
        }

        public UpdaterOptionsBuilder withReExtractNatives(boolean reExtractNatives)
        {
            this.reExtractNativesArgument.set(reExtractNatives);
            return this;
        }

        /**
         * @deprecated Since 1.2.3. Use {@link UpdaterOptionsBuilder#withEnableCurseForgePlugin(boolean)} instead.
         * It will be removed in a future release.
         */
        @Deprecated
        public UpdaterOptionsBuilder withEnableModsFromCurseForge(boolean enableModsFromCurseForge)
        {
            this.enableCurseForgePluginArgument.set(enableModsFromCurseForge);
            return this;
        }

        /**
         * @deprecated Since 1.2.3. Use {@link UpdaterOptionsBuilder#withEnableOptifineDownloaderPlugin(boolean)} (boolean)} instead.
         * It will be removed in a future release.
         */
        @Deprecated
        public UpdaterOptionsBuilder withInstallOptifineAsMod(boolean installOptifineAsMod)
        {
            this.enableOptifineDownloaderPluginArgument.set(installOptifineAsMod);
            return this;
        }

        public UpdaterOptionsBuilder withEnableCurseForgePlugin(boolean enableModsFromCurseForge)
        {
            this.enableCurseForgePluginArgument.set(enableModsFromCurseForge);
            return this;
        }

        public UpdaterOptionsBuilder withEnableOptifineDownloaderPlugin(boolean installOptifineAsMod)
        {
            this.enableOptifineDownloaderPluginArgument.set(installOptifineAsMod);
            return this;
        }

        public UpdaterOptionsBuilder withDownloadServer(boolean downloadServer)
        {
            this.downloadServerArgument.set(downloadServer);
            return this;
        }

        public UpdaterOptionsBuilder withNmbrThreadsForAssets(int nmbrThreadsForAssets)
        {
            this.nmbrThreadsForAssetsArgument.set(nmbrThreadsForAssets);
            return this;
        }

        public UpdaterOptionsBuilder withExternalFileDeleter(IFileDeleter externalFileDeleter)
        {
            this.externalFileDeleterArgument.set(externalFileDeleter);
            return this;
        }

        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(
                    this.silentReadArgument.get(),
                    this.reExtractNativesArgument.get(),
                    this.enableCurseForgePluginArgument.get(),
                    this.enableOptifineDownloaderPluginArgument.get(),
                    this.downloadServerArgument.get(),
                    this.nmbrThreadsForAssetsArgument.get(),
                    this.externalFileDeleterArgument.get()
            );
        }
    }
}
