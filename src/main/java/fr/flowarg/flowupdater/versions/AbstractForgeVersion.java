package fr.flowarg.flowupdater.versions;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.*;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
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
import java.util.stream.Collectors;

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
    protected final CurseModPackInfos modPackInfos;
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
     * @param modPackInfos modpack informations.
     * @param old if the current version of forge is an old forge version.
     */
    protected AbstractForgeVersion(List<Mod> mods, List<CurseFileInfos> curseMods,
            String forgeVersion, ModFileDeleter fileDeleter, OptifineInfo optifine,
            CurseModPackInfos modPackInfos, boolean old)
    {
        this.mods = mods;
        this.curseMods = curseMods;
        this.forgeVersion = forgeVersion;
        this.fileDeleter = fileDeleter;
        this.optifine = optifine;
        this.modPackInfos = modPackInfos;
        this.old = old;
    }

    /**
     * Check if forge is already installed. Used by {@link FlowUpdater} on update task.
     * @param installDir the minecraft installation dir.
     * @return true if forge is already installed or not.
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(Path installDir)
    {
        return Files.exists(Paths.get(installDir.toString(), "libraries", "net", "minecraftforge", "forge", this.forgeVersion, "forge-" + this.forgeVersion + ".jar"));
    }

    /**
     * This function installs a Forge version at the specified directory.
     * @param dirToInstall Specified directory.
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.FORGE);
        this.logger.info("Installing forge, version: " + this.forgeVersion + "...");
        this.checkForgeEnv(dirToInstall);
    }

    @Override
    public ModLoaderLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws Exception
    {
        final Path tempDirPath = Paths.get(dirToInstall.toString(), ".flowupdater");
        FileUtils.deleteDirectory(tempDirPath);
        Files.createDirectories(tempDirPath);

        final Path installPath = Paths.get(tempDirPath.toString(), "forge-installer.jar");
        final Path patchesPath = Paths.get(tempDirPath.toString(), "patches.jar");

        this.downloadForgeInstaller(stream, installPath, patchesPath);
        this.patchForgeInstaller(installPath, patchesPath, tempDirPath);

        return this.makeCommand(Paths.get(tempDirPath.toString(), "forge-installer-patched.jar"), dirToInstall, tempDirPath);
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
            final Path tempInstallerDirPath = Paths.get(tempDir.toString(), "installer");
            Files.createDirectories(tempInstallerDirPath);

            this.logger.info("Applying patches...");
            ZipUtils.unzipJar(tempInstallerDirPath.toString(), install.toString());
            this.cleanInstaller(tempInstallerDirPath);
            ZipUtils.unzipJar(tempInstallerDirPath.toString(), patches.toString());

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
        command.add(patchedInstaller.toString());
        command.add("--installClient");
        command.add(dirToInstall.toString());
        this.logger.info("Launching forge installer...");

        return new ModLoaderLauncherEnvironment(command, tempDir);
    }

    protected abstract void cleanInstaller(Path tempInstallerDir) throws Exception;

    /**
     * Check if the minecraft installation already contains another forge installation not corresponding to this version.
     * @param dirToInstall Forge installation directory.
     * @return true if another version of forge is installed. false if not.
     * @throws Exception if an error occurred.
     */
    protected boolean checkForgeEnv(Path dirToInstall) throws Exception
    {
        boolean result = false;
        final Path forgeDirPath = Paths.get(dirToInstall.toString(), "libraries", "net", "minecraftforge", "forge");
        if(Files.exists(forgeDirPath))
        {
            for (Path contained : Files.list(forgeDirPath).collect(Collectors.toList()))
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
     * This function installs mods at the specified directory.
     * @param modsDir Specified mods directory.
     * @param pluginManager PluginManager of FlowUpdater
     * @throws Exception If the install fail.
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
                    final Path optifineFilePath = Paths.get(modsDir.toString(), optifine.getName());

                    if(Files.notExists(optifineFilePath))
                        IOUtils.copy(this.logger, Paths.get(modsDir.getParent().toString(), ".op", optifine.getName()), optifineFilePath);
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
        final Path outputPath = Paths.get(tempDir.toString(), "forge-installer-patched.zip");
        ZipUtils.compressFiles(FileUtils.list(tempInstallerDir).toArray(Path[]::new), outputPath);
        Files.move(outputPath, Paths.get(outputPath.toString().replace(".zip", ".jar")), StandardCopyOption.REPLACE_EXISTING);
        FileUtils.deleteDirectory(tempInstallerDir);
    }

    @Override
    public List<Mod> getMods()
    {
        return this.mods;
    }

    @Override
    public void setAllCurseMods(List<Object> allCurseMods)
    {
        this.allCurseMods = allCurseMods;
    }

    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }

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

    @Override
    public DownloadInfos getDownloadInfos()
    {
        return this.downloadInfos;
    }

    @Override
    public IProgressCallback getCallback()
    {
        return this.callback;
    }

    @Override
    public List<CurseFileInfos> getCurseMods()
    {
        return this.curseMods;
    }

    public OptifineInfo getOptifine()
    {
        return this.optifine;
    }

    @Override
    public CurseModPackInfos getModPackInfos()
    {
        return this.modPackInfos;
    }

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
