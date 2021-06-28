package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.PluginManager;

import java.io.InputStream;
import java.net.MalformedURLException;
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
     * Install all mods in the mods directory.
     * @param modsDir mods directory.
     * @param pluginManager used to check loaded plugins.
     * @throws Exception if an I/O error occurred.
     */
    void installMods(Path modsDir, PluginManager pluginManager) throws Exception;

    /**
     * Get all processed mods / mods to process.
     * @return all processed mods / mods to process.
     */
    List<Mod> getMods();

    default void installAllMods(Path modsDir, boolean cursePluginLoaded)
    {
        this.getDownloadInfos().getMods().forEach(mod -> {
            try
            {
                final Path modFilePath = modsDir.resolve(mod.getName());
                IOUtils.download(this.getLogger(), new URL(mod.getDownloadURL()), modFilePath);
                this.getCallback().onFileDownloaded(modFilePath);
            }
            catch (MalformedURLException e)
            {
                this.getLogger().printStackTrace(e);
            }
            this.getDownloadInfos().incrementDownloaded(mod.getSize());
            this.getCallback().update(this.getDownloadInfos().getDownloadedBytes(), this.getDownloadInfos().getTotalToDownloadBytes());
        });

        if(cursePluginLoaded)
        {
            this.getDownloadInfos().getCurseMods().forEach(obj -> {
                final CurseMod curseMod = (CurseMod)obj;
                try
                {
                    final Path modFilePath = modsDir.resolve(curseMod.getName());
                    IOUtils.download(this.getLogger(), new URL(curseMod.getDownloadURL()), modFilePath);
                    this.getCallback().onFileDownloaded(modFilePath);
                }
                catch (MalformedURLException e)
                {
                    this.getLogger().printStackTrace(e);
                }
                this.getDownloadInfos().incrementDownloaded(curseMod.getLength());
                this.getCallback().update(this.getDownloadInfos().getDownloadedBytes(), this.getDownloadInfos().getTotalToDownloadBytes());
            });
        }
    }

    /**
     * Check if the minecraft installation already contains another mod loader installation not corresponding to this version.
     * @param dirToInstall Mod loader installation directory.
     * @return true if another version of mod loader is installed. false if not.
     * @throws Exception if an error occurred.
     */
    boolean checkModLoaderEnv(Path dirToInstall) throws Exception;

    /**
     * Get the {@link DownloadInfos} object.
     * @return download info.
     */
    DownloadInfos getDownloadInfos();

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

    class ModLoaderLauncherEnvironment
    {
        private final List<String> command;
        private final Path tempDir;

        public ModLoaderLauncherEnvironment(List<String> command, Path tempDir)
        {
            this.command = command;
            this.tempDir = tempDir;
        }

        public List<String> getCommand()
        {
            return this.command;
        }

        public Path getTempDir()
        {
            return this.tempDir;
        }
    }
}
