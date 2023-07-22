package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowzipper.ZipUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
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
public abstract class AbstractForgeVersion extends AbstractModLoaderVersion
{
    protected final OptiFineInfo optiFineInfo;
    protected final ForgeVersionType forgeVersionType;

    protected URL installerUrl;

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     * @param mods {@link List} to install.
     * @param curseMods {@link List} to install.
     * @param forgeVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param optiFineInfo OptiFine version to install.
     * @param curseModPackInfo mod pack information.
     * @param forgeVersionType the type of the forge version.
     */
    protected AbstractForgeVersion(List<Mod> mods, List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods,
            String forgeVersion, ModFileDeleter fileDeleter, OptiFineInfo optiFineInfo,
            CurseModPackInfo curseModPackInfo, ModrinthModPackInfo modrinthModPackInfo, ForgeVersionType forgeVersionType)
    {
        super(mods, forgeVersion, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo);
        this.optiFineInfo = optiFineInfo;
        this.forgeVersionType = forgeVersionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        final Path forgeDir = installDir
                .resolve("libraries")
                .resolve("net")
                .resolve("minecraftforge")
                .resolve("forge")
                .resolve(this.modLoaderVersion);

        final Path neoForgeDir = installDir
                .resolve("libraries")
                .resolve("net")
                .resolve("neoforged")
                .resolve("forge")
                .resolve(this.modLoaderVersion);

        return this.isForgeJarAlreadyInstalled(forgeDir) || this.isForgeJarAlreadyInstalled(neoForgeDir);
    }

    private boolean isForgeJarAlreadyInstalled(Path forgeDir)
    {
        if(Files.notExists(forgeDir))
            return false;

        return Files.exists(forgeDir.resolve("forge-" + this.modLoaderVersion + ".jar")) ||
                Files.exists(forgeDir.resolve("forge-" + this.modLoaderVersion + "-universal.jar"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.MOD_LOADER);
        this.logger.info("Installing Forge, version: " + this.modLoaderVersion + "...");
        this.checkModLoaderEnv(dirToInstall);

        if (!this.isCompatible()) return;

        try (BufferedInputStream stream = new BufferedInputStream(IOUtils.catchForbidden(this.installerUrl)))
        {
            final ModLoaderLauncherEnvironment forgeLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            final ProcessBuilder processBuilder = new ProcessBuilder(forgeLauncherEnvironment.getCommand());

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.directory(dirToInstall.toFile());
            final Process process = processBuilder.start();
            process.waitFor();

            this.logger.info("Successfully installed Forge!");
            FileUtils.deleteDirectory(forgeLauncherEnvironment.getTempDir());
        }
        catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
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
        this.logger.info("Downloading " + this.forgeVersionType.getDisplayName() + " installer...");
        Files.copy(stream, install, StandardCopyOption.REPLACE_EXISTING);
        this.logger.info("Downloading patches...");
        Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/" + this.forgeVersionType.getPatches() + "patches.jar").openStream(),
                   patches, StandardCopyOption.REPLACE_EXISTING);
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
     * This method makes a new {@link fr.flowarg.flowupdater.versions.IModLoaderVersion.ModLoaderLauncherEnvironment}
     * to launch the mod loader's launcher.
     * @param patchedInstaller the patched installer path.
     * @param dirToInstall the installation directory.
     * @param tempDir the temporary directory where is the installer stuff.
     * @return the fresh {@link fr.flowarg.flowupdater.versions.IModLoaderVersion.ModLoaderLauncherEnvironment}.
     */
    protected ModLoaderLauncherEnvironment makeCommand(@NotNull Path patchedInstaller, @NotNull Path dirToInstall, Path tempDir)
    {
        final List<String> command = new ArrayList<>();
        command.add(this.javaPath);
        command.add("-Xmx512M");
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
    public void checkModLoaderEnv(@NotNull Path dirToInstall) throws Exception
    {
        final Path forgeDirPath = dirToInstall.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge");
        final Path neoForgeDirPath = dirToInstall.resolve("libraries").resolve("net").resolve("neoforged").resolve("forge");

        if(this.isCompatible() && (this.containsOtherForgeVersion(forgeDirPath) || this.containsOtherForgeVersion(neoForgeDirPath)))
        {
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("net").resolve("minecraft"));
            FileUtils.deleteDirectory(forgeDirPath.getParent());
            FileUtils.deleteDirectory(neoForgeDirPath.getParent());
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("de").resolve("oceanlabs"));
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("cpw"));
        }
    }

    private boolean containsOtherForgeVersion(Path forgeDirPath) throws Exception
    {
        if(!Files.exists(forgeDirPath))
            return false;

        boolean result = false;
        for (Path contained : FileUtils.list(forgeDirPath))
        {
            if(contained.getFileName().toString().contains(this.modLoaderVersion)) continue;

            FileUtils.deleteDirectory(contained);
            result = true;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installMods(Path modsDir) throws Exception
    {
        this.callback.step(Step.MODS);
        this.installAllMods(modsDir);

        final OptiFine ofObj = this.downloadList.getOptiFine();

        if(ofObj != null)
        {
            try
            {
                final Path optiFineFilePath = modsDir.resolve(ofObj.getName());

                if (Files.notExists(optiFineFilePath) || Files.size(optiFineFilePath) != ofObj.getSize())
                    IOUtils.copy(this.logger, modsDir.getParent().resolve(".op").resolve(ofObj.getName()), optiFineFilePath);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
            this.downloadList.incrementDownloaded(ofObj.getSize());
            this.callback.update(this.downloadList.getDownloadInfo());
        }

        this.fileDeleter.delete(modsDir, this.mods, ofObj);
    }

    /** This method packs the modified installer to a JAR file.
     * @param tempDir  the temporary directory where is the installer stuff.
     * @param tempInstallerDir the temporary directory where is the installer stuff.
     * @throws Exception if an error occurred.
     */
    protected void packPatchedInstaller(final @NotNull Path tempDir, final Path tempInstallerDir) throws Exception
    {
        final Path outputPath = tempDir.resolve("forge-installer-patched.zip");
        ZipUtils.compressFiles(FileUtils.list(tempInstallerDir).toArray(new Path[0]), outputPath);
        Files.move(outputPath, Paths.get(outputPath.toString().replace(".zip", ".jar")),
                   StandardCopyOption.REPLACE_EXISTING);
        FileUtils.deleteDirectory(tempInstallerDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        super.attachFlowUpdater(flowUpdater);
        if (!this.modLoaderVersion.contains("-"))
            this.modLoaderVersion = this.vanilla.getName() + '-' + this.modLoaderVersion;
        else this.modLoaderVersion = this.modLoaderVersion.trim();
        try
        {
            this.installerUrl = new URL(
                    String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar",
                                  this.modLoaderVersion, this.modLoaderVersion));
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }
    
    protected boolean isCompatible()
    {
        return true;
    }

    /**
     * Get given OptiFine information.
     * @return OptiFine information.
     */
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
    }
}
