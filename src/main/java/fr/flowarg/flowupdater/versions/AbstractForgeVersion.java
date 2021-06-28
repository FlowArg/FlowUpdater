package fr.flowarg.flowupdater.versions;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.PluginManager;
import fr.flowarg.flowzipper.ZipUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The base object of a forge version.
 * Implemented by {@link OldForgeVersion} and {@link NewForgeVersion}
 * @author flow
 */
public abstract class AbstractForgeVersion implements ICurseFeaturesUser, IModLoaderVersion
{
    protected final List<Mod> mods;
    protected final List<CurseFileInfos> curseMods;
    protected final ModFileDeleter fileDeleter;
    protected final OptifineInfo optifine;
    protected final CurseModPackInfo modPackInfo;
    protected final boolean old;

    protected List<Object> allCurseMods;
    protected URL installerUrl;
    protected DownloadInfos downloadInfos;
    protected ILogger logger;
    protected IProgressCallback callback;
    protected VanillaVersion vanilla;
    protected String forgeVersion;

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     * @param mods {@link List} to install.
     * @param curseMods {@link List} to install.
     * @param forgeVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     * @param optifine Optifine version to install.
     * @param modPackInfo modpack information.
     * @param old if the current version of forge is an old forge version.
     */
    protected AbstractForgeVersion(List<Mod> mods, List<CurseFileInfos> curseMods,
            String forgeVersion, ModFileDeleter fileDeleter, OptifineInfo optifine,
            CurseModPackInfo modPackInfo, boolean old)
    {
        this.mods = mods;
        this.curseMods = curseMods;
        this.forgeVersion = forgeVersion;
        this.fileDeleter = fileDeleter;
        this.optifine = optifine;
        this.modPackInfo = modPackInfo;
        this.old = old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(Path installDir)
    {
        return Files.exists(installDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(this.forgeVersion).resolve("forge-" + this.forgeVersion + ".jar"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.FORGE);
        this.logger.info("Installing forge, version: " + this.forgeVersion + "...");
        this.checkModLoaderEnv(dirToInstall);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModLoaderLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws Exception
    {
        final Path tempDirPath = dirToInstall.resolve(".flowupdater");
        FileUtils.deleteDirectory(tempDirPath);
        Files.createDirectories(tempDirPath);

        final Path installPath = tempDirPath.resolve("forge-installer.jar");
        final Path patchesPath = tempDirPath.resolve("patches.jar");

        this.downloadForgeInstaller(stream, installPath, patchesPath);
        this.patchForgeInstaller(installPath, patchesPath, tempDirPath);

        return this.makeCommand(tempDirPath.resolve("forge-installer-patched.jar"), dirToInstall, tempDirPath);
    }

    protected void downloadForgeInstaller(InputStream stream, Path install, Path patches) throws Exception
    {
        this.logger.info("Downloading " + (this.old ? "old" : "new") + " forge installer...");
        Files.copy(stream, install, StandardCopyOption.REPLACE_EXISTING);
        this.logger.info("Downloading patches...");
        Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/" + (this.old ? "old" : "") + "patches.jar").openStream(), patches, StandardCopyOption.REPLACE_EXISTING);
    }

    protected void patchForgeInstaller(Path install, Path patches, Path tempDir)
    {
        try {
            final Path tempInstallerDirPath = tempDir.resolve("installer");
            Files.createDirectories(tempInstallerDirPath);

            this.logger.info("Applying patches...");
            ZipUtils.unzipJar(tempInstallerDirPath, install);
            this.cleanInstaller(tempInstallerDirPath);
            ZipUtils.unzipJar(tempInstallerDirPath, patches);

            this.logger.info("Repacking installer...");
            this.packPatchedInstaller(tempDir, tempInstallerDirPath);
            Files.delete(patches);
        }
        catch(Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    protected ModLoaderLauncherEnvironment makeCommand(Path patchedInstaller, Path dirToInstall, Path tempDir)
    {
        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx256M");
        command.add("-jar");
        command.add(patchedInstaller.toAbsolutePath().toString());
        command.add("--installClient");
        command.add(dirToInstall.toAbsolutePath().toString());
        this.logger.info("Launching forge installer...");

        return new ModLoaderLauncherEnvironment(command, tempDir);
    }

    /**
     * Clean temp installer directory.
     * @param tempInstallerDir directory to clear.
     * @throws Exception if an I/O error occurred.
     */
    protected abstract void cleanInstaller(Path tempInstallerDir) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModLoaderEnv(Path dirToInstall) throws Exception
    {
        boolean result = false;
        final Path forgeDirPath = dirToInstall.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge");
        if(Files.exists(forgeDirPath))
        {
            for (Path contained : FileUtils.list(forgeDirPath))
            {
                if(!contained.getFileName().toString().contains(this.forgeVersion))
                {
                    FileUtils.deleteDirectory(contained);
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installMods(Path modsDir, PluginManager pluginManager) throws Exception
    {
        this.callback.step(Step.MODS);
        final boolean cursePluginLoaded = pluginManager.isCursePluginLoaded();
        final boolean optifinePluginLoaded = pluginManager.isOptifinePluginLoaded();
        this.installAllMods(modsDir, cursePluginLoaded);

        Object ofObj = null;
        if(optifinePluginLoaded)
        {
            if(this.downloadInfos.getOptifine() != null)
            {
                final Optifine optifine = (Optifine)this.downloadInfos.getOptifine();
                ofObj = optifine;
                try
                {
                    final Path optifineFilePath = modsDir.resolve(optifine.getName());

                    if(Files.notExists(optifineFilePath))
                        IOUtils.copy(this.logger, modsDir.getParent().resolve(".op").resolve(optifine.getName()), optifineFilePath);
                } catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
                this.downloadInfos.incrementDownloaded(optifine.getSize());
                this.callback.update(this.downloadInfos.getDownloadedBytes(), this.downloadInfos.getTotalToDownloadBytes());
            }
        }

        this.fileDeleter.delete(modsDir, this.mods, cursePluginLoaded, this.allCurseMods, optifinePluginLoaded, ofObj);
    }
    
    protected void packPatchedInstaller(final Path tempDir, final Path tempInstallerDir) throws Exception
    {
        final Path outputPath = tempDir.resolve("forge-installer-patched.zip");
        ZipUtils.compressFiles(FileUtils.list(tempInstallerDir).toArray(new Path[0]), outputPath);
        Files.move(outputPath, Paths.get(outputPath.toString().replace(".zip", ".jar")), StandardCopyOption.REPLACE_EXISTING);
        FileUtils.deleteDirectory(tempInstallerDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Mod> getMods()
    {
        return this.mods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllCurseMods(List<Object> allCurseMods)
    {
        this.allCurseMods = allCurseMods;
    }

    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(FlowUpdater flowUpdater)
    {
        this.callback = flowUpdater.getCallback();
        this.logger = flowUpdater.getLogger();
        this.downloadInfos = flowUpdater.getDownloadInfos();
        this.vanilla = flowUpdater.getVersion();
        if (!forgeVersion.contains("-"))
            this.forgeVersion = this.vanilla.getName() + '-' + forgeVersion;
        else this.forgeVersion = forgeVersion.trim();
        try
        {
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }

        if(!this.curseMods.isEmpty() && !flowUpdater.getUpdaterOptions().isEnableCurseForgePlugin())
            this.logger.warn("You must enable the enableCurseForgePlugin option to use curse forge features!");

        if(this.optifine != null && !flowUpdater.getUpdaterOptions().isEnableOptifineDownloaderPlugin())
            this.logger.warn("You must enable the enableOptifineDownloaderPlugin option to use optifine!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DownloadInfos getDownloadInfos()
    {
        return this.downloadInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProgressCallback getCallback()
    {
        return this.callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseFileInfos> getCurseMods()
    {
        return this.curseMods;
    }

    public OptifineInfo getOptifine()
    {
        return this.optifine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurseModPackInfo getModPackInfo()
    {
        return this.modPackInfo;
    }

    /**
     * {@inheritDoc}
     */
    public ILogger getLogger()
    {
        return this.logger;
    }

    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }

    public List<Object> getAllCurseMods()
    {
        return this.allCurseMods;
    }
}
