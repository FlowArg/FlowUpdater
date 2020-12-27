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
     * Select some mods from CurseForge ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE CURSE_FORGE !!
     */
    private final boolean enableModsFromCurseForge;

    /**
     * Install optifine from the official Website (mod) ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE OPTIFINE !!
     */
    private final boolean installOptifineAsMod;

    private final int nmbrThreadsForAssets;

    private final IFileDeleter externalFileDeleter;

    private UpdaterOptions(boolean silentRead, boolean reExtractNatives, boolean enableModsFromCurseForge, boolean installOptifineAsMod, boolean downloadServer, int nmbrThreadsForAssets, IFileDeleter externalFileDeleter)
    {
        this.silentRead = silentRead;
        this.reExtractNatives = reExtractNatives;
        this.enableModsFromCurseForge = enableModsFromCurseForge;
        this.installOptifineAsMod = installOptifineAsMod;
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

    public boolean isEnableModsFromCurseForge()
    {
        return this.enableModsFromCurseForge;
    }

    public boolean isInstallOptifineAsMod()
    {
        return this.installOptifineAsMod;
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
        private final BuilderArgument<Boolean> enableModsFromCurseForgeArgument = new BuilderArgument<>("EnableModsFromCurseForge", () -> false).optional();
        private final BuilderArgument<Boolean> installOptifineAsModArgument = new BuilderArgument<>("InstallOptifineAsMod", () -> false).optional();
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

        public UpdaterOptionsBuilder withEnableModsFromCurseForge(boolean enableModsFromCurseForge)
        {
            this.enableModsFromCurseForgeArgument.set(enableModsFromCurseForge);
            return this;
        }

        public UpdaterOptionsBuilder withInstallOptifineAsMod(boolean installOptifineAsMod)
        {
            this.installOptifineAsModArgument.set(installOptifineAsMod);
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

        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(
                    this.silentReadArgument.get(),
                    this.reExtractNativesArgument.get(),
                    this.enableModsFromCurseForgeArgument.get(),
                    this.installOptifineAsModArgument.get(),
                    this.downloadServerArgument.get(),
                    this.nmbrThreadsForAssetsArgument.get(),
                    this.externalFileDeleterArgument.get()
            );
        }
    }
}
