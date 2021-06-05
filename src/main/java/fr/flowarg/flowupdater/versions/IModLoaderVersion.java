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
import java.nio.file.Paths;
import java.util.List;

public interface IModLoaderVersion
{
    void attachFlowUpdater(FlowUpdater flowUpdater);
    boolean isModLoaderAlreadyInstalled(Path installDir);
    void install(Path dirToInstall) throws Exception;
    ModLoaderLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws Exception;
    void installMods(Path modsDir, PluginManager pluginManager) throws Exception;
    List<Mod> getMods();

    default void installAllMods(Path modsDir, boolean cursePluginLoaded)
    {
        this.getDownloadInfos().getMods().forEach(mod -> {
            try
            {
                final Path modFilePath = Paths.get(modsDir.toString(), mod.getName());
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
                    final Path modFilePath = Paths.get(modsDir.toString(), curseMod.getName());
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

    DownloadInfos getDownloadInfos();
    ILogger getLogger();
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
