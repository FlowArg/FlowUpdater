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
                IOUtils.download(logger, new URL(mod.getDownloadURL()), new File(modsDir, mod.getName()));
            }
            catch (MalformedURLException e)
            {
                logger.printStackTrace(e);
            }
            downloadInfos.incrementDownloaded();
            callback.update(downloadInfos.getDownloaded(), downloadInfos.getTotalToDownload());
        });

        if(cursePluginLoaded)
        {
            downloadInfos.getCurseMods().forEach(obj -> {
                try
                {
                    final CurseMod curseMod = (CurseMod)obj;
                    IOUtils.download(logger, new URL(curseMod.getDownloadURL()), new File(modsDir, curseMod.getName()));
                } catch (MalformedURLException e)
                {
                    logger.printStackTrace(e);
                }
                downloadInfos.incrementDownloaded();
                callback.update(downloadInfos.getDownloaded(), downloadInfos.getTotalToDownload());
            });
        }
    }
}
