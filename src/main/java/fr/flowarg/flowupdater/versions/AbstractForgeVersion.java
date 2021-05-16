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
    protected final ILogger logger;
    protected final List<Mod> mods;
    protected final VanillaVersion vanilla;
    protected final String forgeVersion;
    protected final IProgressCallback callback;
    protected final List<CurseFileInfos> curseMods;
    protected final ModFileDeleter fileDeleter;
    protected final OptifineInfo optifine;
    protected final CurseModPackInfos modPackInfos;
    protected final boolean old;
    protected List<Object> allCurseMods;
    protected URL installerUrl;
    protected DownloadInfos downloadInfos;

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     * @param logger {@link ILogger} used for logging.
     * @param mods {@link List} to install.
     * @param curseMods {@link List} to install.
     * @param forgeVersion to install.
     * @param vanilla {@link VanillaVersion}.
     * @param callback {@link IProgressCallback} used for update progression.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     * @param optifine Optifine version to install.
     * @param modPackInfos modpack informations.
     * @param old if the current version of forge is an old forge version.
     */
    protected AbstractForgeVersion(ILogger logger, List<Mod> mods, List<CurseFileInfos> curseMods, String forgeVersion, VanillaVersion vanilla, IProgressCallback callback, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPackInfos, boolean old)
    {
        this.logger = logger;
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.vanilla = vanilla;
        this.optifine = optifine;
        this.modPackInfos = modPackInfos;
        this.old = old;
        if (!forgeVersion.contains("-"))
            this.forgeVersion = this.vanilla.getName() + '-' + forgeVersion;
        else this.forgeVersion = forgeVersion.trim();
        this.callback = callback;
        try
        {
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
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
    // TODO Optimize this.
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
        IOUtils.deleteDirectory(tempDirPath);
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

    protected void patchForgeInstaller(Path install, Path patches, Path tempDir) throws Exception
    {
        final Path tempInstallerDirPath = Paths.get(tempDir.toString(), "installer");
        Files.createDirectories(tempInstallerDirPath);

        this.logger.info("Applying patches...");
        ZipUtils.unzipJarWithLZMACompat(tempInstallerDirPath.toFile(), install.toFile());
        this.cleanInstaller(tempInstallerDirPath);
        ZipUtils.unzipJarWithLZMACompat(tempInstallerDirPath.toFile(), patches.toFile());

        this.logger.info("Repacking installer...");
        this.packPatchedInstaller(tempDir, tempInstallerDirPath);
        Files.delete(patches);
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
                    IOUtils.deleteDirectory(contained);
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
        ModCommons.installAllMods(this.downloadInfos, this.logger, modsDir, this.callback, cursePluginLoaded);

        Object ofObj = null;
        if(optifinePluginLoaded)
        {
            if(this.downloadInfos.getOptifine() != null)
            {
                final Optifine optifine = (Optifine)this.downloadInfos.getOptifine();
                ofObj = optifine;
                try
                {
                    final Path pluginDirPath = Paths.get(modsDir.getParent().toString(), ".op");
                    final Path optifineFilePath = Paths.get(modsDir.toString(), optifine.getName());

                    if(Files.notExists(optifineFilePath))
                        Files.copy(Paths.get(pluginDirPath.toString(), optifine.getName()), optifineFilePath, StandardCopyOption.REPLACE_EXISTING);
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

    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }

    @Override
    public void appendDownloadInfos(DownloadInfos infos)
    {
        this.downloadInfos = infos;
    }
    
    protected void packPatchedInstaller(final Path tempDir, final Path tempInstallerDir) throws Exception
    {
        final Path outputPath = Paths.get(tempDir.toString(), "forge-installer-patched.zip");
        ZipUtils.compressFiles(FileUtils.list(tempInstallerDir.toFile()), outputPath.toFile());
        Files.move(outputPath, Paths.get(outputPath.toString().replace(".zip", ".jar")), StandardCopyOption.REPLACE_EXISTING);
        IOUtils.deleteDirectory(tempInstallerDir);
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
