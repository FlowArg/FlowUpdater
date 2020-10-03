package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.CurseModInfos;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent information about download status. Used for progress system {@link IProgressCallback}.
 * @author FlowArg
 */
public class DownloadInfos
{
    private final List<Downloadable> libraryDownloadables = new ArrayList<>();
    private final List<AssetDownloadable> assetDownloadables = new ArrayList<>();
    private final List<ExternalFile> extFiles = new ArrayList<>();
    private final List<Mod> mods = new ArrayList<>();
    private final List<Object> curseMods = new ArrayList<>();
	private int totalToDownload;
	private int downloaded;
	
	public void init()
	{
		this.totalToDownload = this.libraryDownloadables.size() + this.assetDownloadables.size() + this.extFiles.size() + this.mods.size() + this.curseMods.size();
		this.downloaded = 0;
	}
	
	public void incrementDownloaded()
	{
		++this.downloaded;
	}
	
	public int getTotalToDownload()
	{
		return this.totalToDownload;
	}
	
	public int getDownloaded()
	{
		return this.downloaded;
	}
	
	public void setTotalToDownload(int totalToDownload)
	{
		this.totalToDownload = totalToDownload;
	}
	
	public void setDownloaded(int downloaded)
	{
		this.downloaded = downloaded;
	}
	
	public List<AssetDownloadable> getAssetDownloadables()
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

	public void clear()
	{
		this.libraryDownloadables.clear();
		this.extFiles.clear();
		this.assetDownloadables.clear();
		this.mods.clear();
		this.curseMods.clear();
		this.totalToDownload = 0;
		this.downloaded = 0;
	}
}
