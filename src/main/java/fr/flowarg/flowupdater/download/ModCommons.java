package fr.flowarg.flowupdater.download;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModCommons
{
    public static void installAllMods(DownloadInfos downloadInfos, ILogger logger, Path modsDir, IProgressCallback callback, boolean cursePluginLoaded)
    {
        downloadInfos.getMods().forEach(mod -> {
            try
            {
                final Path modFilePath = Paths.get(modsDir.toString(), mod.getName());
                IOUtils.download(logger, new URL(mod.getDownloadURL()), modFilePath);
                callback.onFileDownloaded(modFilePath);
            }
            catch (MalformedURLException e)
            {
                logger.printStackTrace(e);
            }
            downloadInfos.incrementDownloaded(mod.getSize());
            callback.update(downloadInfos.getDownloadedBytes(), downloadInfos.getTotalToDownloadBytes());
        });

        if(cursePluginLoaded)
        {
            downloadInfos.getCurseMods().forEach(obj -> {
                final CurseMod curseMod = (CurseMod)obj;
                try
                {
                    final Path modFilePath = Paths.get(modsDir.toString(), curseMod.getName());
                    IOUtils.download(logger, new URL(curseMod.getDownloadURL()), modFilePath);
                    callback.onFileDownloaded(modFilePath);
                } catch (MalformedURLException e)
                {
                    logger.printStackTrace(e);
                }
                downloadInfos.incrementDownloaded(curseMod.getLength());
                callback.update(downloadInfos.getDownloadedBytes(), downloadInfos.getTotalToDownloadBytes());
            });
        }
    }
}
