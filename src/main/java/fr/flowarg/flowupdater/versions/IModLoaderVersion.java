package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface IModLoaderVersion
{
    /**
     * Attach {@link FlowUpdater} object to mod loaders, allow them to retrieve some information.
     * @param flowUpdater flow updater object.
     */
    void attachFlowUpdater(FlowUpdater flowUpdater);

    /**
     * Check if the current mod loader is already installed.
     * @param installDir the dir to check.
     * @return if the current mod loader is already installed.
     */
    boolean isModLoaderAlreadyInstalled(Path installDir);

    /**
     * Install the current mod loader in a specified directory.
     * @param dirToInstall folder where the mod loader is going to be installed.
     * @throws Exception if an I/O error occurred.
     */
    void install(Path dirToInstall) throws Exception;

    /**
     * Various setup before mod loader's installer launch.
     * @param dirToInstall folder where the mod loader is going to be installed.
     * @param stream Installer download stream.
     * @return a new {@link ModLoaderLauncherEnvironment} object.
     * @throws Exception is an I/O error occurred.
     */
    ModLoaderLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws Exception;

    /**
     * Install all mods in the mods' directory.
     * @param modsDir mods directory.
     * @throws Exception if an I/O error occurred.
     */
    void installMods(Path modsDir) throws Exception;

    /**
     * Get the mod loader version.
     */
    String getModLoaderVersion();

    /**
     * Get all processed mods / mods to process.
     * @return all processed mods / mods to process.
     */
    List<Mod> getMods();

    /**
     * Download mods in the mods' folder.
     * @param modsDir mods' folder
     */
    default void installAllMods(Path modsDir)
    {
        this.getDownloadList().getMods().forEach(mod -> {
            try
            {
                final Path modFilePath = modsDir.resolve(mod.getName());
                IOUtils.download(this.getLogger(), new URL(mod.getDownloadURL()), modFilePath);
                this.getCallback().onFileDownloaded(modFilePath);
            }
            catch (Exception e)
            {
                this.getLogger().printStackTrace(e);
            }
            this.getDownloadList().incrementDownloaded(mod.getSize());
            this.getCallback().update(this.getDownloadList().getDownloadInfo());
        });
    }

    /**
     * Check if the minecraft installation already contains another mod loader installation not corresponding to this version.
     * @param dirToInstall Mod loader installation directory.
     * @throws Exception if an error occurred.
     */
    void checkModLoaderEnv(Path dirToInstall) throws Exception;

    /**
     * Get the {@link DownloadList} object.
     * @return download info.
     */
    DownloadList getDownloadList();

    /**
     * Get the {@link ILogger} object.
     * @return the logger.
     */
    ILogger getLogger();

    /**
     * Get the {@link IProgressCallback} object.
     * @return the progress callback.
     */
    IProgressCallback getCallback();

    /**
     * This class represents a process' environment with a working directory and the launch command.
     */
    class ModLoaderLauncherEnvironment
    {
        private final List<String> command;
        private final Path tempDir;

        /**
         * Construct a new {@link ModLoaderLauncherEnvironment} object.
         * @param command the process' command.
         * @param tempDir the working directory.
         */
        public ModLoaderLauncherEnvironment(List<String> command, Path tempDir)
        {
            this.command = command;
            this.tempDir = tempDir;
        }

        /**
         * Get the process' command.
         * @return the process' command.
         */
        public List<String> getCommand()
        {
            return this.command;
        }

        /**
         * Get the working directory.
         * @return the working directory.
         */
        public Path getTempDir()
        {
            return this.tempDir;
        }
    }

    /**
     * Get the attached {@link ModFileDeleter} instance;
     * @return this mod file deleter;
     */
    ModFileDeleter getFileDeleter();
}
