package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represent information about download status. Used with {@link IProgressCallback} progress system.
 *
 * @author FlowArg
 */
public class DownloadInfos
{
    private final List<Downloadable> libraryDownloadables = new ArrayList<>();
    private final Queue<AssetDownloadable> assetDownloadables = new ConcurrentLinkedDeque<>();
    private final List<ExternalFile> extFiles = new ArrayList<>();
    private final List<Mod> mods = new ArrayList<>();
    private final List<Object> curseMods = new ArrayList<>();
    private Object optifine = null;
    private final AtomicInteger totalToDownload = new AtomicInteger();
    private final AtomicInteger downloaded = new AtomicInteger();
    private boolean init = false;

    public void init()
    {
        if(!this.isInit())
        {
            this.totalToDownload.set(this.libraryDownloadables.size() + this.assetDownloadables.size() + this.extFiles.size() + this.mods.size() + this.curseMods.size() + (this.optifine == null ? 0 : 1));
            this.downloaded.set(0);
            this.init = true;
        }
    }

    public void incrementDownloaded()
    {
        this.downloaded.incrementAndGet();
    }

    public int getTotalToDownload()
    {
        return this.totalToDownload.get();
    }

    public int getDownloaded()
    {
        return this.downloaded.get();
    }

    public Queue<AssetDownloadable> getAssetDownloadables()
    {
        return this.assetDownloadables;
    }

    public List<Downloadable> getLibraryDownloadables()
    {
        return this.libraryDownloadables;
    }

    public List<ExternalFile> getExtFiles()
    {
        return this.extFiles;
    }

    public List<Mod> getMods()
    {
        return this.mods;
    }

    public List<Object> getCurseMods()
    {
        return this.curseMods;
    }

    public Object getOptifine()
    {
        return this.optifine;
    }

    public void setOptifine(Object optifine)
    {
        this.optifine = optifine;
    }

    public boolean isInit()
    {
        return this.init;
    }

    public void clear()
    {
        this.libraryDownloadables.clear();
        this.extFiles.clear();
        this.assetDownloadables.clear();
        this.mods.clear();
        this.curseMods.clear();
        this.optifine = null;
        this.totalToDownload.set(0);
        this.downloaded.set(0);
    }
}
