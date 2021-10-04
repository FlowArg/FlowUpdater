package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represent information about download status. Used with {@link IProgressCallback} progress system.
 *
 * @author FlowArg
 */
public class DownloadList
{
    private final List<Downloadable> downloadableFiles = new ArrayList<>();
    private final List<AssetDownloadable> downloadableAssets = new ArrayList<>();
    private final List<ExternalFile> extFiles = new ArrayList<>();
    private final List<Mod> mods = new ArrayList<>();
    private final List<CurseMod> curseMods = new ArrayList<>();
    private OptiFine optiFine = null;
    private final AtomicLong totalToDownloadBytes = new AtomicLong(0);
    private final AtomicLong downloadedBytes = new AtomicLong(0);
    private boolean init = false;

    /**
     * This method initialize fields.
     */
    public void init()
    {
        if(this.init) return;

        this.downloadableFiles.forEach(downloadable -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + downloadable.getSize()));
        this.downloadableAssets.forEach(downloadable -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + downloadable.getSize()));
        this.extFiles.forEach(externalFile -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + externalFile.getSize()));
        this.mods.forEach(mod -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + mod.getSize()));
        this.curseMods.forEach(obj -> this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + (long)(obj.getLength())));
        if (this.optiFine != null)
            this.totalToDownloadBytes.set(this.totalToDownloadBytes.get() + (long)(this.optiFine.getSize()));
        this.init = true;
    }

    /**
     * This method increments the number of bytes downloaded by the number of bytes passed as parameter.
     * @param bytes number of bytes to add to downloaded bytes.
     */
    public void incrementDownloaded(long bytes)
    {
        this.downloadedBytes.set(this.downloadedBytes.get() + bytes);
    }

    /**
     * Get the total of bytes to download.
     * @return bytes to download.
     */
    public long getTotalToDownloadBytes()
    {
        return this.totalToDownloadBytes.get();
    }

    /**
     * Get the downloaded bytes.
     * @return the downloaded bytes.
     */
    public long getDownloadedBytes()
    {
        return this.downloadedBytes.get();
    }

    /**
     * Get the queue that contains all assets to download.
     * @return the queue that contains all assets to download.
     */
    public List<AssetDownloadable> getDownloadableAssets()
    {
        return this.downloadableAssets;
    }

    /**
     * Get the list that contains all downloadable files.
     * @return the list that contains all downloadable files.
     */
    public List<Downloadable> getDownloadableFiles()
    {
        return this.downloadableFiles;
    }

    /**
     * Get the list that contains all external files.
     * @return the list that contains all external files.
     */
    public List<ExternalFile> getExtFiles()
    {
        return this.extFiles;
    }

    /**
     * Get the list that contains all mods.
     * @return the list that contains all mods.
     */
    public List<Mod> getMods()
    {
        return this.mods;
    }

    /**
     * Get the list that contains all curse mods.
     * @return the list that contains all curse mods.
     */
    public List<CurseMod> getCurseMods()
    {
        return this.curseMods;
    }

    /**
     * Get the OptiFine object.
     * @return the OptiFine object.
     */
    public OptiFine getOptiFine()
    {
        return this.optiFine;
    }

    /**
     * Define the OptiFine object.
     * @param optiFine the OptiFine object to define.
     */
    public void setOptiFine(OptiFine optiFine)
    {
        this.optiFine = optiFine;
    }

    /**
     * Clear and reset that download list object.
     */
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
