package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IModLoaderVersion
{
    void appendDownloadInfos(DownloadInfos infos);
    boolean isModLoaderAlreadyInstalled(File installDir);
    void install(File dirToInstall);
    ModLoaderLauncherEnvironment prepareModLoaderLauncher(File dirToInstall, InputStream stream) throws IOException;
    void installMods(File modsDir, PluginManager pluginManager) throws Exception;
    List<Mod> getMods();

    class ModLoaderLauncherEnvironment
    {
        private final List<String> command;
        private final File tempDir;

        public ModLoaderLauncherEnvironment(List<String> command, File tempDir)
        {
            this.command = command;
            this.tempDir = tempDir;
        }

        public List<String> getCommand()
        {
            return this.command;
        }

        public File getTempDir()
        {
            return this.tempDir;
        }
    }
}
