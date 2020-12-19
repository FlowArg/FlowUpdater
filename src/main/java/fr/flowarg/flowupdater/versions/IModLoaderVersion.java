package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.utils.PluginManager;

import java.io.File;

public interface IModLoaderVersion
{
    void appendDownloadInfos(DownloadInfos infos);
    boolean isModLoaderAlreadyInstalled(File installDir);
    void install(File dirToInstall);
    void installMods(File modsDir, PluginManager pluginManager) throws Exception;
}
