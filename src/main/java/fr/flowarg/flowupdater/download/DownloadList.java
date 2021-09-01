package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represent information about download status. Used with {@link IProgressCallback} progress system.
 *
 * @author FlowArg
 */
public class DownloadList
{
    private final List<Downloadable> downloadableFiles = new ArrayList<>();
    private final Queue<AssetDownloadable> downloadableAssets = new ConcurrentLinkedDeque<>();
    private final List<ExternalFile> extFiles = new ArrayList<>();
    private final List<Mod> mods = new ArrayList<>();
    private final List<CurseMod> curseMods = new ArrayList<>();
    private OptiFine optiFine = null;
    private final AtomicLong totalToDownloadBytes = new AtomicLong(0);
    private final AtomicLong downloadedBytes = new AtomicLong(0);
    private boolean init = false;

    public void init()
    {
        if(this.isInit()) return;

        this.downloadableFiles.forEach(downloadable -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + downloadable.getSize()));
        this.downloadableAssets.forEach(downloadable -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + downloadable.getSize()));
        this.extFiles.forEach(externalFile -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + externalFile.getSize()));
        this.mods.forEach(mod -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + mod.getSize()));
        this.curseMods.forEach(obj -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + (long)(obj.getLength())));
        if (this.optiFine != null)
            this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + (long)(this.optiFine.getSize()));
        this.init = true;
    }

    public void incrementDownloaded(long bytes)
    {
        this.downloadedBytes.set(this.downloadedBytes.get() + bytes);
    }

    public long getTotalToDownloadBytes()
    {
        return this.totalToDownloadBytes.get();
    }

    public long getDownloadedBytes()
    {
        return this.downloadedBytes.get();
    }

    public Queue<AssetDownloadable> getDownloadableAssets()
    {
        return this.downloadableAssets;
    }

    public List<Downloadable> getDownloadableFiles()
    {
        return this.downloadableFiles;
    }

    public List<ExternalFile> getExtFiles()
    {
        return this.extFiles;
    }

    public List<Mod> getMods()
    {
        return this.mods;
    }

    public List<CurseMod> getCurseMods()
    {
        return this.curseMods;
    }

    public OptiFine getOptiFine()
    {
        return this.optiFine;
    }

    public void setOptiFine(OptiFine optiFine)
    {
        this.optiFine = optiFine;
    }

    public boolean isInit()
    {
        return this.init;
    }

    public void clear()
    {
        this.downloadableFiles.clear();
        this.extFiles.clear();
        this.downloadableAssets.clear();
        this.mods.clear();
        this.curseMods.clear();
        this.optiFine = null;
        this.totalToDownloadBytes.set(0);
        this.downloadedBytes.set(0);
        this.init = false;
    }
}
