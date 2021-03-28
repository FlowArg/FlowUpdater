package fr.flowarg.flowupdater.download;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ModCommons
{
    public static void installAllMods(DownloadInfos downloadInfos, ILogger logger, File modsDir, IProgressCallback callback, boolean cursePluginLoaded)
    {
        downloadInfos.getMods().forEach(mod -> {
            try
            {
                final File modFile = new File(modsDir, mod.getName());
                IOUtils.download(logger, new URL(mod.getDownloadURL()), modFile);
                callback.onFileDownloaded(modFile);
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
                    final File modFile = new File(modsDir, curseMod.getName());
                    IOUtils.download(logger, new URL(curseMod.getDownloadURL()), modFile);
                    callback.onFileDownloaded(modFile);
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
