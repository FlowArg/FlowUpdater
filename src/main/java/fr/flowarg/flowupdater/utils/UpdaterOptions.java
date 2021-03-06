package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.download.VanillaDownloader;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.util.Random;

/**
 * Represent some settings for FlowUpdater
 *
 * @author FlowArg
 */
public class UpdaterOptions
{
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, false, false, false, false, new Random().nextInt(2) + 2, new ExternalFileDeleter());

    private final boolean silentRead;
    private final boolean downloadServer;
    private final boolean reExtractNatives;
    private final boolean enableCurseForgePlugin;
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

    /**
     * Disable some debug logs on Minecraft json's parsing.
     * Default: true
     */
    public boolean isSilentRead()
    {
        return this.silentRead;
    }

    /**
     * If this option is set to true, {@link fr.flowarg.flowupdater.FlowUpdater} will download the Minecraft Server.
     * Default: false
     */
    public boolean isDownloadServer()
    {
        return this.downloadServer;
    }

    /**
     * Re-extract natives at each updates?
     * Today, this option isn't useful because {@link VanillaDownloader} checks automatically if all natives are correctly extracted.
     * Default: false
     */
    public boolean isReExtractNatives()
    {
        return this.reExtractNatives;
    }

    /**
     * Enable CurseForgePlugin (CFP)?
     * Default: false
     */
    public boolean isEnableCurseForgePlugin()
    {
        return this.enableCurseForgePlugin;
    }

    /**
     * Enable OptifineDownloaderPlugin (ODP)?
     * Default: false
     */
    public boolean isEnableOptifineDownloaderPlugin()
    {
        return this.enableOptifineDownloaderPlugin;
    }

    /**
     * Number of threads used to download assets.
     * Default: 2 or 3 (random)
     */
    public int getNmbrThreadsForAssets()
    {
        return this.nmbrThreadsForAssets;
    }

    /**
     * The external file deleter is used to check if some external files need to be downloaded.
     * Default: {@link fr.flowarg.flowupdater.utils.ExternalFileDeleter}
     */
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
        private final BuilderArgument<Integer> nmbrThreadsForAssetsArgument = new BuilderArgument<>("Number of Threads for assets", () -> new Random().nextInt(2) + 2).optional();
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
