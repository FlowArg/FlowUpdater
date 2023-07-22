package fr.flowarg.flowupdater;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.download.*;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.IntegrationManager;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseFeaturesUser;
import fr.flowarg.flowupdater.integrations.modrinthintegration.IModrinthFeaturesUser;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.UpdaterOptions;
import fr.flowarg.flowupdater.utils.VersionChecker;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
import fr.flowarg.flowupdater.versions.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represent the base class of the updater.<br>
 * You can define some parameters about your version (Forge, Vanilla, MCP, Fabric...).
 * @author FlowArg
 */
public class FlowUpdater
{
    /** FlowUpdater's version string constant */
    public static final String FU_VERSION = "1.8.0";

    /** Vanilla version's object to update/install */
    private final VanillaVersion vanillaVersion;

    /** Logger object */
    private final ILogger logger;

    /** Mod loader version to install, can be null if you want a vanilla or MCP version */
    private final IModLoaderVersion modLoaderVersion;

    /** Progress callback to notify installation progress */
    private final IProgressCallback callback;

    /** Information about download status */
    private final DownloadList downloadList;

    /** Represent some settings for FlowUpdater */
    private final UpdaterOptions updaterOptions;

    /** Represent a list of ExternalFile. External files are downloaded before post-executions.*/
    private final List<ExternalFile> externalFiles;

    /** Represent a list of Runnable. Post-Executions are called after update. */
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
     * @param callback {@link IProgressCallback} used for update progression. If it's null, it will be
     * automatically assigned to {@link FlowUpdater#NULL_CALLBACK}.
     * @param externalFiles {@link List<ExternalFile>} are downloaded before postExecutions.
     * @param postExecutions {@link List<Runnable>} are called after update.
     * @param modLoaderVersion {@link IModLoaderVersion} to install can be null.
     */
    private FlowUpdater(VanillaVersion vanillaVersion, ILogger logger,
            UpdaterOptions updaterOptions, IProgressCallback callback,
            List<ExternalFile> externalFiles, List<Runnable> postExecutions,
            IModLoaderVersion modLoaderVersion)
    {
        this.logger = logger;
        this.vanillaVersion = vanillaVersion;
        this.externalFiles = externalFiles;
        this.postExecutions = postExecutions;
        this.modLoaderVersion = modLoaderVersion;
        this.updaterOptions = updaterOptions;
        this.callback = callback;
        this.downloadList = new DownloadList();
        this.integrationManager = new IntegrationManager(this);
        this.logger.info(String.format(
                "------------------------- FlowUpdater for Minecraft %s v%s -------------------------",
                this.vanillaVersion.getName(), FU_VERSION));

        if(this.updaterOptions.isVersionChecker())
            VersionChecker.run(this.logger);

        this.callback.init(this.logger);
    }

    /**
     * This method updates the Minecraft Installation in the given directory.
     * If the {@link #vanillaVersion} is {@link VanillaVersion#NULL_VERSION}, the updater will
     * only run external files and post executions.
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

    private void updateMinecraft(@NotNull Path dir) throws Exception
    {
        this.loadVanillaStuff();

        if(this.modLoaderVersion != null)
            this.loadModLoader(dir);

        this.startVanillaDownload(dir);

        if(this.modLoaderVersion != null)
            this.installModLoader(dir);
    }

    private void loadVanillaStuff() throws Exception
    {
        if(this.vanillaVersion == VanillaVersion.NULL_VERSION)
        {
            this.downloadList.init();
            return;
        }

        this.logger.info(String.format("Reading data about %s Minecraft version...", this.vanillaVersion.getName()));
        new VanillaReader(this).read();
    }

    private void loadModLoader(@NotNull Path dir) throws Exception
    {
        final Path modsDirPath = dir.resolve("mods");

        this.checkMods(this.modLoaderVersion, modsDirPath);

        if(this.modLoaderVersion instanceof ICurseFeaturesUser)
            this.integrationManager.loadCurseForgeIntegration(modsDirPath, (ICurseFeaturesUser)this.modLoaderVersion);

        if(this.modLoaderVersion instanceof IModrinthFeaturesUser)
            this.integrationManager.loadModrinthIntegration(modsDirPath, (IModrinthFeaturesUser)this.modLoaderVersion);

        if(this.modLoaderVersion instanceof AbstractForgeVersion)
            this.integrationManager.loadOptiFineIntegration(modsDirPath, (AbstractForgeVersion)this.modLoaderVersion);
    }

    private void checkMods(@NotNull IModLoaderVersion modLoader, Path modsDir) throws Exception
    {
        for(Mod mod : modLoader.getMods())
        {
            final Path filePath = modsDir.resolve(mod.getName());

            if(Files.notExists(filePath) ||
                    Files.size(filePath) != mod.getSize() ||
                    (!mod.getSha1().isEmpty() && !FileUtils.getSHA1(filePath).equalsIgnoreCase(mod.getSha1())))
                this.downloadList.getMods().add(mod);
        }
    }

    private void startVanillaDownload(Path dir) throws Exception
    {
        if (Files.notExists(dir))
            Files.createDirectories(dir);

        new VanillaDownloader(dir, this).download();
    }

    private void installModLoader(Path dir) throws Exception
    {
        String name;
        if(this.modLoaderVersion instanceof AbstractForgeVersion)
            name = "Forge";
        else if(this.modLoaderVersion instanceof FabricVersion)
            name = "Fabric";
        else if(this.modLoaderVersion instanceof QuiltVersion)
            name = "Quilt";
        else if(this.modLoaderVersion instanceof OtherModLoaderVersion)
            name = ((OtherModLoaderVersion)this.modLoaderVersion).name();
        else throw new FlowUpdaterException("Hi developer, check the OtherModLoaderVersion documentation.");

        this.installModLoader(this.modLoaderVersion, dir, name);
    }

    private void installModLoader(IModLoaderVersion modLoader, Path dir, String name) throws Exception
    {
        if(modLoader == null) return;

        modLoader.attachFlowUpdater(this);
        if(!modLoader.isModLoaderAlreadyInstalled(dir))
            modLoader.install(dir);
        else this.logger.info(name + " is already installed! Skipping installation...");
        modLoader.installMods(dir.resolve("mods"));
    }

    private void updateExtFiles(Path dir)
    {
        if(this.downloadList.getExtFiles().isEmpty()) return;

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
            this.callback.update(this.downloadList.getDownloadInfo());
        });
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
        this.callback.update(this.downloadList.getDownloadInfo());
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
        private final BuilderArgument<IModLoaderVersion> modLoaderVersionArgument = new BuilderArgument<IModLoaderVersion>("ModLoader").optional().require(this.versionArgument);

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
        public FlowUpdaterBuilder withExternalFiles(Collection<ExternalFile> externalFiles)
        {
            this.externalFilesArgument.get().addAll(externalFiles);
            return this;
        }

        /**
         * Append an array object in the final FlowUpdater instance.
         * @param externalFiles the array to append and update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withExternalFiles(ExternalFile... externalFiles)
        {
            return withExternalFiles(Arrays.asList(externalFiles));
        }

        /**
         * Append external files in the final FlowUpdater instance.
         * @param externalFilesJsonUrl the URL of the json of external files append and update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withExternalFiles(URL externalFilesJsonUrl)
        {
            return withExternalFiles(ExternalFile.getExternalFilesFromJson(externalFilesJsonUrl));
        }

        /**
         * Append external files in the final FlowUpdater instance.
         * @param externalFilesJsonUrl the URL of the json of external files append and update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withExternalFiles(String externalFilesJsonUrl)
        {
            return withExternalFiles(ExternalFile.getExternalFilesFromJson(externalFilesJsonUrl));
        }

        /**
         * Append a {@link List} object in the final FlowUpdater instance.
         * @param postExecutions the {@link List} to append and run after the update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withPostExecutions(Collection<Runnable> postExecutions)
        {
            this.postExecutionsArgument.get().addAll(postExecutions);
            return this;
        }

        /**
         * Append an array object in the final FlowUpdater instance.
         * @param postExecutions the array to append and run after the update.
         * @return the builder.
         */
        public FlowUpdaterBuilder withPostExecutions(Runnable... postExecutions)
        {
            return withPostExecutions(Arrays.asList(postExecutions));
        }

        /**
         * Necessary if you want to install a mod loader like Forge or Fabric, for instance.
         * Append a {@link IModLoaderVersion} object in the final FlowUpdater instance.
         * @param modLoaderVersion the {@link IModLoaderVersion} to append and install.
         * @return the builder.
         */
        public FlowUpdaterBuilder withModLoaderVersion(IModLoaderVersion modLoaderVersion)
        {
            this.modLoaderVersionArgument.set(modLoaderVersion);
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
                    this.modLoaderVersionArgument.get()
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
     * Get th {@link IModLoaderVersion} attached to this {@link FlowUpdater} instance.
     * @return a mod loader version.
     */
    public IModLoaderVersion getModLoaderVersion()
    {
        return this.modLoaderVersion;
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
     * Get the list of post-executions.
     * @return all post-executions
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
}
