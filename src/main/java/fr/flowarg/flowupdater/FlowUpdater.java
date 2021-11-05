package fr.flowarg.flowupdater;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.download.*;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.IntegrationManager;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.UpdaterOptions;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
import fr.flowarg.flowupdater.versions.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent the base class of the updater.<br>
 * You can define some parameters about your version (Forge, Vanilla, MCP, Fabric...).
 * @author FlowArg
 */
public class FlowUpdater
{
    /** Vanilla version's object to update/install */
    private final VanillaVersion vanillaVersion;

    /** Logger object */
    private final ILogger logger;

    /** Forge Version to install, can be null if you want a vanilla/MCP/Fabric installation */
    private final AbstractForgeVersion forgeVersion;

    /** Fabric version to install, can be null if you want a vanilla/MCP/Forge installation **/
    private final FabricVersion fabricVersion;

    /** Progress callback to notify installation progress */
    private final IProgressCallback callback;

    /** Information about download status */
    private final DownloadList downloadList;

    /** Represent some settings for FlowUpdater */
    private final UpdaterOptions updaterOptions;

    /** Represent a list of ExternalFile. External files are download before post executions.*/
    private final List<ExternalFile> externalFiles;

    /** Represent a list of Runnable. Post Executions are called after update. */
    private final List<Runnable> postExecutions;

    /** The integration manager object */
    private final IntegrationManager integrationManager;

    /** Default callback */
    public static final IProgressCallback NULL_CALLBACK = new IProgressCallback()
    {
        @Override
        public void init(@NotNull ILogger logger)
        {
            logger.info("Default callback will be used.");
        }
    };

    /** Default logger, null for path argument = no file logger */
    public static final ILogger DEFAULT_LOGGER = new Logger("[FlowUpdater]", null);

    /**
     * Basic constructor for {@link FlowUpdater}, use {@link FlowUpdaterBuilder} to instantiate a new {@link FlowUpdater}.
     * @param vanillaVersion {@link VanillaVersion} to update.
     * @param logger {@link ILogger} used for log information.
     * @param updaterOptions {@link UpdaterOptions} for this updater
     * @param callback {@link IProgressCallback} used for update progression. If it's null, it will automatically be assigned to {@link FlowUpdater#NULL_CALLBACK}.
     * @param externalFiles {@link List<ExternalFile>} are downloaded before postExecutions.
     * @param postExecutions {@link List<Runnable>} are called after update.
     * @param forgeVersion {@link AbstractForgeVersion} to install, can be null.
     * @param fabricVersion {@link FabricVersion} to install, can be null.
     */
    private FlowUpdater(VanillaVersion vanillaVersion, ILogger logger,
            UpdaterOptions updaterOptions, IProgressCallback callback,
            List<ExternalFile> externalFiles, List<Runnable> postExecutions,
            AbstractForgeVersion forgeVersion, FabricVersion fabricVersion)
    {
        this.logger = logger;
        this.vanillaVersion = vanillaVersion;
        this.externalFiles = externalFiles;
        this.postExecutions = postExecutions;
        this.forgeVersion = forgeVersion;
        this.fabricVersion = fabricVersion;
        this.updaterOptions = updaterOptions;
        this.callback = callback;
        this.downloadList = new DownloadList();
        this.integrationManager = new IntegrationManager(this);
        this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", this.vanillaVersion.getName(), "1.5.0"));
        this.callback.init(this.logger);
    }

    /**
     * This method updates the Minecraft Installation in the given directory. If the {@link #vanillaVersion} is {@link VanillaVersion#NULL_VERSION}, the updater will
     * run only external files and post executions.
     * @param dir Directory where is the Minecraft installation.
     * @throws IOException if an I/O problem occurred.
     */
    public void update(Path dir) throws Exception
    {
        this.checkExtFiles(dir);
        this.updateMinecraft(dir);
        this.updateExtFiles(dir);
        this.runPostExecutions();
        this.endUpdate();
    }

    private void checkExtFiles(Path dir) throws Exception
    {
        this.updaterOptions.getExternalFileDeleter().delete(this.externalFiles, this.downloadList, dir);
    }

    private void updateMinecraft(Path dir) throws Exception
    {
        if(this.vanillaVersion == VanillaVersion.NULL_VERSION)
        {
            this.downloadList.init();
            return;
        }

        this.logger.info(String.format("Reading data about %s Minecraft version...", this.vanillaVersion.getName()));
        new VanillaReader(this).read();

        final Path modsDirPath = dir.resolve("mods");

        final VersionType versionType = this.vanillaVersion.getVersionType();

        if(this.forgeVersion != null && versionType == VersionType.FORGE)
        {
            this.checkMods(this.forgeVersion, modsDirPath);
            this.integrationManager.loadCurseForgeIntegration(modsDirPath, this.forgeVersion);
            this.integrationManager.loadOptiFineIntegration(modsDirPath, this.forgeVersion);
        }

        if (this.fabricVersion != null && versionType == VersionType.FABRIC)
        {
            this.checkMods(this.fabricVersion, modsDirPath);
            this.integrationManager.loadCurseForgeIntegration(modsDirPath, this.fabricVersion);
        }

        if (Files.notExists(dir))
            Files.createDirectories(dir);

        new VanillaDownloader(dir, this).download();

        if(versionType == VersionType.MCP || versionType == VersionType.VANILLA) return;

        if(versionType == VersionType.FORGE) this.installModLoader(this.forgeVersion, dir, "Forge");
        if(versionType == VersionType.FABRIC) this.installModLoader(this.fabricVersion, dir, "Fabric");
    }

    private void checkMods(@NotNull IModLoaderVersion modLoader, Path modsDir) throws Exception
    {
        for(Mod mod : modLoader.getMods())
        {
            final Path filePath = modsDir.resolve(mod.getName());

            if(Files.notExists(filePath) || !FileUtils.getSHA1(filePath).equalsIgnoreCase(mod.getSha1()) || FileUtils.getFileSizeBytes(filePath) != mod.getSize())
                this.downloadList.getMods().add(mod);
        }
    }

    private void installModLoader(IModLoaderVersion modLoader, Path dir, String name) throws Exception
    {
        if(modLoader != null)
        {
            modLoader.attachFlowUpdater(this);
            if(!modLoader.isModLoaderAlreadyInstalled(dir))
                modLoader.install(dir);
            else this.logger.info(name + " is already installed ! Skipping installation...");
            modLoader.installMods(dir.resolve("mods"));
        }
    }

    private void updateExtFiles(Path dir)
    {
        if(!this.downloadList.getExtFiles().isEmpty())
        {
            this.callback.step(Step.EXTERNAL_FILES);
            this.logger.info("Downloading external file(s)...");
            this.downloadList.getExtFiles().forEach(extFile -> {
                try
                {
                    final Path filePath = dir.resolve(extFile.getPath());
                    IOUtils.download(this.logger, new URL(extFile.getDownloadURL()), filePath);
                    this.callback.onFileDownloaded(filePath);
                }
                catch (IOException e)
                {
                    this.logger.printStackTrace(e);
                }
                this.downloadList.incrementDownloaded(extFile.getSize());
                this.callback.update(this.downloadList.getDownloadedBytes(), this.downloadList.getTotalToDownloadBytes());
            });
        }
    }

    private void runPostExecutions()
    {
        if(this.postExecutions.isEmpty()) return;

        this.callback.step(Step.POST_EXECUTIONS);
        this.logger.info("Running post executions...");
        this.postExecutions.forEach(Runnable::run);
    }

    private void endUpdate()
    {
        this.callback.step(Step.END);
        this.callback.update(this.downloadList.getTotalToDownloadBytes(), this.downloadList.getTotalToDownloadBytes());
        this.downloadList.clear();
    }

    /**
     * Builder of {@link FlowUpdater}.
     * @author Flow Arg (FlowArg)
     */
    public static class FlowUpdaterBuilder implements IBuilder<FlowUpdater>
    {
        private final BuilderArgument<VanillaVersion> versionArgument = new BuilderArgument<>("VanillaVersion", () -> VanillaVersion.NULL_VERSION).optional();
        private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", () -> DEFAULT_LOGGER).optional();
        private final BuilderArgument<UpdaterOptions> updaterOptionsArgument = new BuilderArgument<>("UpdaterOptions", () -> UpdaterOptions.DEFAULT).optional();
        private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("Callback", () -> NULL_CALLBACK).optional();
        private final BuilderArgument<List<ExternalFile>> externalFilesArgument = new BuilderArgument<List<ExternalFile>>("External Files", ArrayList::new).optional();
        private final BuilderArgument<List<Runnable>> postExecutionsArgument = new BuilderArgument<List<Runnable>>("Post Executions", ArrayList::new).optional();
        private final BuilderArgument<AbstractForgeVersion> forgeVersionArgument = new BuilderArgument<AbstractForgeVersion>("ForgeVersion").optional().require(this.versionArgument);
        private final BuilderArgument<FabricVersion> fabricVersionArgument = new BuilderArgument<FabricVersion>("FabricVersion").optional().require(this.versionArgument);

        /**
         * Append a {@link VanillaVersion} object in the final FlowUpdater instance.
         * @param version the {@link VanillaVersion} to append and install.
         * @return the builder.
         * @deprecated Use {@link #withVanillaVersion(VanillaVersion)} instead.
         */
        @Deprecated
        @ApiStatus.ScheduledForRemoval(inVersion = "1.6.0")
        public FlowUpdaterBuilder withVersion(VanillaVersion version)
        {
            this.versionArgument.set(version);
            return this;
        }

        /**
         * Append a {@link VanillaVersion} object in the final FlowUpdater instance.
         * @param version the {@link VanillaVersion} to append and install.
         * @return the builder.
         */
        public FlowUpdaterBuilder withVanillaVersion(VanillaVersion version)
        {
            this.versionArgument.set(version);
            return this;
        }

        /**
         * Append a {@link ILogger} object in the final FlowUpdater instance.
         * @param logger the {@link ILogger} to append and use.
         * @return the builder.
         */
        public FlowUpdaterBuilder withLogger(ILogger logger)
        {
            this.loggerArgument.set(logger);
            return this;
        }

        /**
         * Append a {@link UpdaterOptions} object in the final FlowUpdater instance.
         * @param updaterOptions the {@link UpdaterOptions} to append and propagate.
         * @return the builder.
         */
        public FlowUpdaterBuilder withUpdaterOptions(UpdaterOptions updaterOptions)
        {
            this.updaterOptionsArgument.set(updaterOptions);
            return this;
        }

        /**
         * Append a {@link IProgressCallback} object in the final FlowUpdater instance.
         * @param callback the {@link IProgressCallback} to append and use.
         * @return the builder.
         */
        public FlowUpdaterBuilder withProgressCallback(IProgressCallback callback)
        {
            this.progressCallbackArgument.set(callback);
            return this;
        }

        /**
         * Append a {@link List} object in the final FlowUpdater instance.
         * @param externalFiles the {@link List} to append and update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withExternalFiles(List<ExternalFile> externalFiles)
        {
            this.externalFilesArgument.set(externalFiles);
            return this;
        }

        /**
         * Append a {@link List} object in the final FlowUpdater instance.
         * @param postExecutions the {@link List} to append and run after the update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withPostExecutions(List<Runnable> postExecutions)
        {
            this.postExecutionsArgument.set(postExecutions);
            return this;
        }

        /**
         * Necessary if you want to install a Forge version.
         * Append a {@link AbstractForgeVersion} object in the final FlowUpdater instance.
         * @param forgeVersion the {@link AbstractForgeVersion} to append and install.
         * @return the builder.
         */
        public FlowUpdaterBuilder withForgeVersion(AbstractForgeVersion forgeVersion)
        {
            this.forgeVersionArgument.set(forgeVersion);
            return this;
        }

        /**
         * Necessary if you want to install a Fabric version.
         * Append a {@link FabricVersion} object in the final FlowUpdater instance.
         * @param fabricVersion the {@link FabricVersion} to append and install.
         * @return the builder.
         */
        public FlowUpdaterBuilder withFabricVersion(FabricVersion fabricVersion)
        {
            this.fabricVersionArgument.set(fabricVersion);
            return this;
        }

        /**
         * Build a new {@link FlowUpdater} instance with provided arguments.
         * @return the new {@link FlowUpdater} instance.
         * @throws BuilderException if an error occurred on FlowUpdater instance building.
         */
        @Override
        public FlowUpdater build() throws BuilderException
        {
            return new FlowUpdater(
                    this.versionArgument.get(),
                    this.loggerArgument.get(),
                    this.updaterOptionsArgument.get(),
                    this.progressCallbackArgument.get(),
                    this.externalFilesArgument.get(),
                    this.postExecutionsArgument.get(),
                    this.forgeVersionArgument.get(),
                    this.fabricVersionArgument.get()
            );
        }
    }

    // Some getters

    /**
     * Get the {@link VanillaVersion} attached to this {@link FlowUpdater} instance.
     * @return a vanilla version.
     */
    public VanillaVersion getVanillaVersion()
    {
        return this.vanillaVersion;
    }

    /**
     * Get the current logger.
     * @return a logger.
     */
    public ILogger getLogger()
    {
        return this.logger;
    }

    /**
     * Get the Forge version attached to this FlowUpdater instance.
     * @return a Forge version.
     */
    public AbstractForgeVersion getForgeVersion()
    {
        return this.forgeVersion;
    }

    /**
     * Get the current callback.
     * @return a callback.
     */
    public IProgressCallback getCallback()
    {
        return this.callback;
    }

    /**
     * Get the list of external files.
     * @return external files.
     */
    public List<ExternalFile> getExternalFiles()
    {
        return this.externalFiles;
    }

    /**
     * Get the list of post executions.
     * @return all post executions
     */
    public List<Runnable> getPostExecutions()
    {
        return this.postExecutions;
    }

    /**
     * Get the download list which contains all download information.
     * @return a {@link DownloadList} instance.
     */
    public DownloadList getDownloadList()
    {
        return this.downloadList;
    }

    /**
     * Get the FlowUpdater's options.
     * @return some useful settings.
     */
    public UpdaterOptions getUpdaterOptions()
    {
        return this.updaterOptions;
    }

    /**
     * Get the Fabric version attached to this FlowUpdater instance.
     * @return a Fabric version.
     */
    public FabricVersion getFabricVersion()
    {
        return this.fabricVersion;
    }
}
