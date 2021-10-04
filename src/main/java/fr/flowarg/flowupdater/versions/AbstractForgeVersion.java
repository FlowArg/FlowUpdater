package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.integrations.IntegrationManager;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowzipper.ZipUtils;
import org.jetbrains.annotations.NotNull;

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
    protected final List<CurseFileInfo> curseMods;
    protected final ModFileDeleter fileDeleter;
    protected final OptiFineInfo optiFineInfo;
    protected final CurseModPackInfo modPackInfo;
    protected final boolean old;

    protected List<CurseMod> allCurseMods;
    protected URL installerUrl;
    protected DownloadList downloadList;
    protected ILogger logger;
    protected IProgressCallback callback;
    protected VanillaVersion vanilla;
    protected String forgeVersion;

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     * @param mods {@link List} to install.
     * @param curseMods {@link List} to install.
     * @param forgeVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param optiFineInfo OptiFine version to install.
     * @param modPackInfo mod pack information.
     * @param old if the current version of forge is an old forge version.
     */
    protected AbstractForgeVersion(List<Mod> mods, List<CurseFileInfo> curseMods,
            String forgeVersion, ModFileDeleter fileDeleter, OptiFineInfo optiFineInfo,
            CurseModPackInfo modPackInfo, boolean old)
    {
        this.mods = mods;
        this.curseMods = curseMods;
        this.forgeVersion = forgeVersion;
        this.fileDeleter = fileDeleter;
        this.optiFineInfo = optiFineInfo;
        this.modPackInfo = modPackInfo;
        this.old = old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        final Path forgeDir = installDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(this.forgeVersion);

        if(Files.notExists(forgeDir)) return false;

        return Files.exists(forgeDir.resolve("forge-" + this.forgeVersion + ".jar")) || Files.exists(forgeDir.resolve("forge-" + this.forgeVersion + "-universal.jar"));
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
    public ModLoaderLauncherEnvironment prepareModLoaderLauncher(@NotNull Path dirToInstall, InputStream stream) throws Exception
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

    /**
     * This method has to download the Forge's installer and some other files if needed.
     * @param stream the input stream of the Forge's installer.
     * @param install the installation directory.
     * @param patches the patches output file.
     * @throws Exception if an error occurred.
     */
    protected void downloadForgeInstaller(InputStream stream, Path install, Path patches) throws Exception
    {
        this.logger.info("Downloading " + (this.old ? "old" : "new") + " forge installer...");
        Files.copy(stream, install, StandardCopyOption.REPLACE_EXISTING);
        this.logger.info("Downloading patches...");
        Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/" + (this.old ? "old" : "") + "patches.jar").openStream(), patches, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * This method has to patch the Forge's installer with FlowUpdater's changes.
     * @param install the installation directory.
     * @param patches the patches file.
     * @param tempDir the temporary directory where is the installer stuff.
     */
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

    /**
     * This method makes a new {@link fr.flowarg.flowupdater.versions.IModLoaderVersion.ModLoaderLauncherEnvironment} to launch the mod loader's launcher.
     * @param patchedInstaller the patched installer path.
     * @param dirToInstall the installation directory.
     * @param tempDir the temporary directory where is the installer stuff.
     * @return the fresh {@link fr.flowarg.flowupdater.versions.IModLoaderVersion.ModLoaderLauncherEnvironment}.
     */
    protected ModLoaderLauncherEnvironment makeCommand(@NotNull Path patchedInstaller, @NotNull Path dirToInstall, Path tempDir)
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
    public boolean checkModLoaderEnv(@NotNull Path dirToInstall) throws Exception
    {
        final Path forgeDirPath = dirToInstall.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge");

        if(!Files.exists(forgeDirPath)) return false;

        boolean result = false;
        for (Path contained : FileUtils.list(forgeDirPath))
        {
            if(contained.getFileName().toString().contains(this.forgeVersion)) continue;

            FileUtils.deleteDirectory(contained);
            result = true;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installMods(Path modsDir, IntegrationManager integrationManager) throws Exception
    {
        this.callback.step(Step.MODS);
        this.installAllMods(modsDir);

        final OptiFine ofObj = this.downloadList.getOptiFine();

        if(ofObj != null)
        {
            try
            {
                final Path optiFineFilePath = modsDir.resolve(ofObj.getName());

                if (Files.notExists(optiFineFilePath) || Files.size(optiFineFilePath) != ofObj.getSize()) IOUtils.copy(this.logger, modsDir.getParent().resolve(".op").resolve(ofObj.getName()), optiFineFilePath);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
            this.downloadList.incrementDownloaded(ofObj.getSize());
            this.callback.update(this.downloadList.getDownloadedBytes(), this.downloadList.getTotalToDownloadBytes());
        }

        this.fileDeleter.delete(modsDir, this.mods, this.allCurseMods, ofObj);
    }

    /** This methods packs the modified installer to a JAR file.
     * @param tempDir  the temporary directory where is the installer stuff.
     * @param tempInstallerDir the temporary directory where is the installer stuff.
     * @throws Exception if an error occurred.
     */
    protected void packPatchedInstaller(final @NotNull Path tempDir, final Path tempInstallerDir) throws Exception
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
    public void setAllCurseMods(List<CurseMod> allCurseMods)
    {
        this.allCurseMods = allCurseMods;
    }

    /**
     * Get the mod file deleter assigned to this version.
     * @return a mod file deleter.
     */
    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        this.callback = flowUpdater.getCallback();
        this.logger = flowUpdater.getLogger();
        this.downloadList = flowUpdater.getDownloadList();
        this.vanilla = flowUpdater.getVanillaVersion();
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DownloadList getDownloadList()
    {
        return this.downloadList;
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
    public List<CurseFileInfo> getCurseMods()
    {
        return this.curseMods;
    }

    /**
     * Get given OptiFine information.
     * @return OptiFine information.
     */
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
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

    /**
     * Get the forge version.
     * @return the forge version.
     */
    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    /**
     * Get the url to the installer.
     * @return the url to the installer.
     */
    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }

    /**
     * Get the list of curse mods.
     * @return the list of curse mods.
     */
    public List<CurseMod> getAllCurseMods()
    {
        return this.allCurseMods;
    }
}
