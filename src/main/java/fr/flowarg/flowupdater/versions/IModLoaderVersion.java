package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.PluginManager;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface IModLoaderVersion
{
    void appendDownloadInfos(DownloadInfos infos);
    boolean isModLoaderAlreadyInstalled(Path installDir);
    void install(Path dirToInstall) throws Exception;
    ModLoaderLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws Exception;
    void installMods(Path modsDir, PluginManager pluginManager) throws Exception;
    List<Mod> getMods();

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
